/*******************************************************************************
    Copyright 2007 Sun Microsystems, Inc.,
    4150 Network Circle, Santa Clara, California 95054, U.S.A.
    All rights reserved.

    U.S. Government Rights - Commercial software.
    Government users are subject to the Sun Microsystems, Inc. standard
    license agreement and applicable provisions of the FAR and its supplements.

    Use is subject to license terms.

    This distribution may include materials developed by third parties.

    Sun, Sun Microsystems, the Sun logo and Java are trademarks or registered
    trademarks of Sun Microsystems, Inc. in the U.S. and other countries.
 ******************************************************************************/

package com.sun.fortress.compiler.disambiguator;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.tuple.Option;
import edu.rice.cs.plt.collect.ConsList;
import edu.rice.cs.plt.collect.CollectUtil;

import com.sun.fortress.nodes.*;
import com.sun.fortress.useful.HasAt;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.nodes_util.ExprFactory;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.compiler.StaticError;
import com.sun.fortress.compiler.GlobalEnvironment;
import com.sun.fortress.compiler.index.ApiIndex;

/**
 * <p>Eliminates ambiguities in an AST that can be resolved solely by knowing what kind
 * of entity a name refers to.  This class assumes all types in declarations have been
 * resolved, and specifically handles the following:
 * <ul>
 * <li>All names referring to APIs are made fully qualified (FnRefs and OprExprs may then
 *     contain lists of qualified names referring to multiple APIs).
 * <li>VarRefs referring to functions become FnRefs with placeholders for implicit static
 *     arguments filled in (to be replaced later during type inference).</li>
 * <li>VarRefs referring to trait members, and that are juxtaposed with Exprs, become 
 *     MethodInvocations.  (Maybe?  Depends on parsing rules for getters.)</li>
 * <li>VarRefs referring to trait members become FieldRefs.</li>
 * <li>FieldRefs referring to trait members, and that are juxtaposed with Exprs, become 
 *     MethodInvocations.  (Maybe?  Depends on parsing rules for getters.)</li>
 * <li>FnRefs referring to trait members, and that are juxtaposed with Exprs, become 
 *     MethodInvocations.</li>
 * <li>StaticArgs of FnRefs, and types nested within them, are disambiguated.</li>
 * </ul>
 * 
 * Additionally, all name references that are undefined or used incorrectly are
 * treated as static errors.</p>
 */
public class ExprDisambiguator extends NodeUpdateVisitor {
    
    private NameEnv _env;
    private Set<SimpleName> _onDemandImports;
    private List<StaticError> _errors;
    
    public ExprDisambiguator(NameEnv env, Set<SimpleName> onDemandImports,
                             List<StaticError> errors) {
        _env = env;
        _onDemandImports = onDemandImports;
        _errors = errors;
    }
    
    private void error(String msg, HasAt loc) {
        _errors.add(StaticError.make(msg, loc));
    }
    
    /** LocalVarDecls introduce local variables while visiting the body. */
    @Override public Node forLocalVarDecl(LocalVarDecl that) {
      List<LValue> lhsResult = recurOnListOfLValue(that.getLhs());
      Option<Expr> rhsResult = recurOnOptionOfExpr(that.getRhs());
      Set<IdName> definedNames = extractDefinedVarNames(lhsResult);
      NameEnv newEnv = new LocalVarEnv(_env, definedNames);
      ExprDisambiguator v = new ExprDisambiguator(newEnv, _onDemandImports, _errors);
      List<Expr> bodyResult = v.recurOnListOfExpr(that.getBody());
      return forLocalVarDeclOnly(that, bodyResult, lhsResult, rhsResult);
    }
    
    private Set<IdName> extractDefinedVarNames(Iterable<? extends LValue> lvalues) {
      Set<IdName> result = new HashSet<IdName>();
      extractDefinedVarNames(lvalues, result);
      return result;
    }
    
    private void extractDefinedVarNames(Iterable<? extends LValue> lvalues,
                                        Set<IdName> result) {
      for (LValue lv : lvalues) {
        boolean valid = true;
        if (lv instanceof LValueBind) {
          valid = result.add(((LValueBind)lv).getName());
        }
        else if (lv instanceof UnpastingBind) {
          valid = result.add(((UnpastingBind)lv).getName());
        }
        else { // lv instanceof UnpastingSplit
          extractDefinedVarNames(((UnpastingSplit)lv).getElems(), result);
        }
        if (!valid) { error("Duplicate local variable name", lv); }
      }
    }
    
    /** LetFns introduce local functions while visiting the body. */
    @Override public Node forLetFn(LetFn that) {
      List<FnDef> fnsResult = recurOnListOfFnDef(that.getFns());
      Set<SimpleName> definedNames = extractDefinedFnNames(fnsResult);
      NameEnv newEnv = new LocalFnEnv(_env, definedNames);
      ExprDisambiguator v = new ExprDisambiguator(newEnv, _onDemandImports, _errors);
      List<Expr> bodyResult = v.recurOnListOfExpr(that.getBody());
      return forLetFnOnly(that, bodyResult, fnsResult);
    }
    
    private Set<SimpleName> extractDefinedFnNames(Iterable<FnDef> fnDefs) {
      Set<SimpleName> result = new HashSet<SimpleName>();
      for (FnDef fd : fnDefs) { result.add(fd.getName()); }
      // multiple instances of the same name are allowed
      return result;
    }

    /** VarRefs can be made qualified or translated into FnRefs. */
    @Override public Node forVarRef(VarRef that) {
        QualifiedIdName qname = that.getVar();
        Option<DottedName> api = qname.getApi();
        IdName entity = qname.getName();
        ConsList<IdName> fields = ConsList.empty();
        Expr result = null;
        
        // First, try to interpret it as a qualified name
        while (result == null && api.isSome()) {
            DottedName givenApiName = Option.unwrap(api);
            Option<DottedName> realApiNameOpt = _env.apiName(givenApiName);
            
            if (realApiNameOpt.isSome()) {
                DottedName realApiName = Option.unwrap(realApiNameOpt);
                QualifiedIdName newName =
                    NodeFactory.makeQualifiedIdName(realApiName, entity);
                if (_env.hasQualifiedVariable(newName)) {
                    if (ConsList.isEmpty(fields) && givenApiName == realApiName) {
                        // no change -- no need to recreate the VarRef
                        return that;
                    }
                    else { result = new VarRef(newName.getSpan(), newName); }
                }
                else if (_env.hasQualifiedFunction(newName)) {
                    result = ExprFactory.makeFnRef(newName);
                    // TODO: insert correct number of to-infer arguments?
                }
                else {
                    error("Unrecognized name: " + NodeUtil.nameString(qname), that);
                    return that;
                }
            }
            
            else {
                // shift all names to the right, and try a smaller api name
                List<Id> ids = givenApiName.getIds();
                fields = ConsList.cons(entity, fields);
                entity = NodeFactory.makeIdName(IterUtil.last(ids));
                Iterable<Id> prefix = IterUtil.skipLast(ids);
                if (IterUtil.isEmpty(prefix)) { api = Option.none(); }
                else { api = Option.some(NodeFactory.makeDottedName(prefix)); }
            }
        }
        
        // Second, try to interpret it as an unqualified name
        if (result == null) {
            // api.isNone() must be true
            Set<QualifiedIdName> vars = _env.explicitVariableNames(entity);
            Set<QualifiedIdName> fns = _env.explicitFunctionNames(entity);
            if (vars.isEmpty() && fns.isEmpty()) {
                vars = _env.onDemandVariableNames(entity);
                fns = _env.onDemandFunctionNames(entity);
                _onDemandImports.add(entity);
            }
            
            if (vars.size() == 1 && fns.isEmpty()) {
                QualifiedIdName newName = IterUtil.first(vars);
                if (newName.getApi().isNone() && newName.getName() == entity &&
                    ConsList.isEmpty(fields)) {
                    // no change -- no need to recreate the VarRef
                    return that;
                }
                else { result = new VarRef(newName.getSpan(), newName); }
            }
            else if (vars.isEmpty() && !fns.isEmpty()) {
                result = new FnRef(entity.getSpan(), IterUtil.asList(fns));
                // TODO: insert correct number of to-infer arguments?
            }
            else if (!vars.isEmpty() || !fns.isEmpty()) {
                Set<QualifiedIdName> varsAndFns = CollectUtil.union(vars, fns);
                error("Name may refer to: " + NodeUtil.namesString(varsAndFns), entity);
                return that;
            }
            else {
                error("Unrecognized name: " + NodeUtil.nameString(qname), that);
                return that;
            }
        }
            
        // result is now non-null
        for (IdName field : fields) {
            result = new FieldRef(result, field);
        }
        if (that.isParenthesized()) {
            result = ExprFactory.makeInParentheses(result);
        }
        return result;
    }
       
}
