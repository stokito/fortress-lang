/*******************************************************************************
    Copyright 2008 Sun Microsystems, Inc.,
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
import com.sun.fortress.nodes_util.Span;
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

    private ExprDisambiguator extend(Set<Id> vars) {
        NameEnv newEnv = new LocalVarEnv(_env, vars);
        return new ExprDisambiguator(newEnv, _onDemandImports, _errors);
    }

    private ExprDisambiguator extendWithSelf(Span span) {
        Set<Id> selfSet = new HashSet<Id>();
        selfSet.add(new Id(span, "self"));
        return extend(selfSet);
    }

    private void error(String msg, HasAt loc) {
        _errors.add(StaticError.make(msg, loc));
    }

    /** LocalVarDecls introduce local variables while visiting the body. */
    @Override public Node forLocalVarDecl(LocalVarDecl that) {
      List<LValue> lhsResult = recurOnListOfLValue(that.getLhs());
      Option<Expr> rhsResult = recurOnOptionOfExpr(that.getRhs());
      Set<Id> definedNames = extractDefinedVarNames(lhsResult);
      NameEnv newEnv = new LocalVarEnv(_env, definedNames);
      ExprDisambiguator v = new ExprDisambiguator(newEnv, _onDemandImports, _errors);
      List<Expr> bodyResult = v.recurOnListOfExpr(that.getBody());
      return forLocalVarDeclOnly(that, bodyResult, lhsResult, rhsResult);
    }

    private Set<Id> extractDefinedVarNames(Iterable<? extends LValue> lvalues) {
      Set<Id> result = new HashSet<Id>();
      extractDefinedVarNames(lvalues, result);
      return result;
    }

    private void extractDefinedVarNames(Iterable<? extends LValue> lvalues,
                                        Set<Id> result) {
      for (LValue lv : lvalues) {
        boolean valid = true;
        if (lv instanceof LValueBind) {
            Id id = ((LValueBind)lv).getName();
            valid = (result.add(id) || id.getText().equals("_"));
        }
        else if (lv instanceof UnpastingBind) {
            Id id = ((UnpastingBind)lv).getName();
            valid = (result.add(id) || id.getText().equals("_"));
        }
        else { // lv instanceof UnpastingSplit
          extractDefinedVarNames(((UnpastingSplit)lv).getElems(), result);
        }
        if (!valid) { error("Duplicate local variable name", lv); }
      }
    }

    /**
     * Pull out all static variables that can be used in expression contexts,
     * and return them as a Set<Id>.
     * TODO: Collect OpParams as well.
     */
    private Set<Id> extractStaticExprVars(List<StaticParam> staticParams) {
        Set<Id> result = new HashSet<Id>();
        for (StaticParam staticParam: staticParams) {
            if (staticParam instanceof BoolParam ||
                staticParam instanceof NatParam  ||
                staticParam instanceof IntParam  ||
                staticParam instanceof UnitParam)
            {
                result.add(((IdStaticParam)staticParam).getName());
            }
        }
        return result;
    }

    /**
     * Convenience method that unwraps its argument and passes it
     * to the overloaded definition of extractParamNames on lists.
     */
    private Set<Id> extractParamNames(Option<List<Param>> params) {
        Set<Id> result = new HashSet<Id>();
        if (params.isNone()) { return new HashSet<Id>(); }
        else { return extractParamNames(Option.unwrap(params)); }
    }

    /**
     * Returns a list of Ids of the given list of Params.
     */
    private Set<Id> extractParamNames(List<Param> params) {
        Set<Id> result = new HashSet<Id>();

        for (Param param: params) {
            result.add(param.getName());
        }
        return result;
    }

    /**
     * When recurring on an AbsTraitDecl, we first need to extend the
     * environment with all the newly bound static parameters that can
     * be used in an expression context.
     * TODO: Handle variables bound in where clauses.
     * TODO: Insert inherited method names into the environment.
     */
    @Override public Node forAbsTraitDecl(final AbsTraitDecl that) {
        ExprDisambiguator v = this.extend(extractStaticExprVars
                                              (that.getStaticParams())).
                                  extendWithSelf(that.getSpan());

        return forAbsTraitDeclOnly(that,
                                   v.recurOnListOfModifier(that.getMods()),
                                   (Id) that.getName().accept(v),
                                   v.recurOnListOfStaticParam(that.getStaticParams()),
                                   v.recurOnListOfTraitTypeWhere(that.getExtendsClause()),
                                   (WhereClause) that.getWhere().accept(v),
                                   v.recurOnListOfTraitType(that.getExcludes()),
                                   v.recurOnOptionOfListOfTraitType(that.getComprises()),
                                   v.recurOnListOfAbsDecl(that.getDecls()));
    }

    /**
     * When recurring on a TraitDecl, we first need to extend the
     * environment with all the newly bound static parameters that
     * can be used in an expression context.
     * TODO: Handle variables bound in where clauses.
     * TODO: Insert inherited method names into the environment.
     */
    @Override public Node forTraitDecl(final TraitDecl that) {
        ExprDisambiguator v = this.extend(extractStaticExprVars
                                              (that.getStaticParams())).
                                  extendWithSelf(that.getSpan());

        return forTraitDeclOnly(that,
                                v.recurOnListOfModifier(that.getMods()),
                                (Id) that.getName().accept(v),
                                v.recurOnListOfStaticParam(that.getStaticParams()),
                                v.recurOnListOfTraitTypeWhere(that.getExtendsClause()),
                                (WhereClause) that.getWhere().accept(v),
                                v.recurOnListOfTraitType(that.getExcludes()),
                                v.recurOnOptionOfListOfTraitType(that.getComprises()),
                                v.recurOnListOfDecl(that.getDecls()));
    }


    /**
     * When recurring on an AbsObjectDecl, we first need to extend the
     * environment with all the newly bound static parameters that can
     * be used in an expression context.
     * TODO: Handle variables bound in where clauses.
     * TODO: Insert inherited method names into the environment.
     */
    @Override public Node forAbsObjectDecl(final AbsObjectDecl that) {
        Set<Id> staticExprVars = extractStaticExprVars(that.getStaticParams());
        Set<Id> params = extractParamNames(that.getParams());
        ExprDisambiguator v = extend(staticExprVars).
                                  extendWithSelf(that.getSpan()).
                                      extend(params);

        return forAbsObjectDeclOnly(that,
                                   v.recurOnListOfModifier(that.getMods()),
                                   (Id) that.getName().accept(v),
                                   v.recurOnListOfStaticParam(that.getStaticParams()),
                                   v.recurOnListOfTraitTypeWhere(that.getExtendsClause()),
                                   (WhereClause) that.getWhere().accept(v),
                                   v.recurOnOptionOfListOfParam(that.getParams()),
                                   v.recurOnOptionOfListOfTraitType(that.getThrowsClause()),
                                   (Contract) that.getContract().accept(v),
                                   v.recurOnListOfAbsDecl(that.getDecls()));
    }

    /**
     * When recurring on an ObjectDecl, we first need to extend the
     * environment with all the newly bound static parameters that can
     * be used in an expression context, along with all the object parameters.
     * TODO: Handle variables bound in where clauses.
     * TODO: Insert inherited method names into the environment.
     */
    @Override public Node forObjectDecl(final ObjectDecl that) {
        Set<Id> staticExprVars = extractStaticExprVars(that.getStaticParams());
        Set<Id> params = extractParamNames(that.getParams());
        ExprDisambiguator v = extend(staticExprVars).
                                  extendWithSelf(that.getSpan()).
                                      extend(params);

        return forObjectDeclOnly(that,
                                   v.recurOnListOfModifier(that.getMods()),
                                   (Id) that.getName().accept(v),
                                   v.recurOnListOfStaticParam(that.getStaticParams()),
                                   v.recurOnListOfTraitTypeWhere(that.getExtendsClause()),
                                   (WhereClause) that.getWhere().accept(v),
                                   v.recurOnOptionOfListOfParam(that.getParams()),
                                   v.recurOnOptionOfListOfTraitType(that.getThrowsClause()),
                                   (Contract) that.getContract().accept(v),
                                   v.recurOnListOfDecl(that.getDecls()));
    }


    /**
     * When recurring on an AbsFnDecl, we first need to extend the
     * environment with all the newly bound static parameters that
     * can be used in an expression context, along with all function
     * parameters and 'self'.
     * TODO: Handle variables bound in where clauses.
     */
    @Override public Node forAbsFnDecl(final AbsFnDecl that) {
        Set<Id> staticExprVars = extractStaticExprVars(that.getStaticParams());
        Set<Id> params = extractParamNames(that.getParams());
        ExprDisambiguator v = extend(staticExprVars).extend(params);

        return forAbsFnDeclOnly(that,
                                v.recurOnListOfModifier(that.getMods()),
                                (SimpleName) that.getName().accept(v),
                                v.recurOnListOfStaticParam(that.getStaticParams()),
                                v.recurOnListOfParam(that.getParams()),
                                v.recurOnOptionOfType(that.getReturnType()),
                                v.recurOnOptionOfListOfTraitType(that.getThrowsClause()),
                                (WhereClause) that.getWhere().accept(v),
                                (Contract) that.getContract().accept(v));
    }


    /**
     * When recurring on a FnDef, we first need to extend the
     * environment with all the newly bound static parameters that
     * can be used in an expression context, along with all function
     * parameters and 'self'.
     * TODO: Handle variables bound in where clauses.
     */
    @Override public Node forFnDef(FnDef that) {
        Set<Id> staticExprVars = extractStaticExprVars(that.getStaticParams());
        Set<Id> params = extractParamNames(that.getParams());
        ExprDisambiguator v = extend(staticExprVars).extend(params);

        return forFnDefOnly(that,
                            v.recurOnListOfModifier(that.getMods()),
                            (SimpleName) that.getName().accept(v),
                            v.recurOnListOfStaticParam(that.getStaticParams()),
                            v.recurOnListOfParam(that.getParams()),
                            v.recurOnOptionOfType(that.getReturnType()),
                            v.recurOnOptionOfListOfTraitType(that.getThrowsClause()),
                            (WhereClause) that.getWhere().accept(v),
                            (Contract) that.getContract().accept(v),
                            (Expr) that.getBody().accept(v));
    }

    /**
     * When recurring on a FnExpr, we first need to extend the
     * environment with any newly bound static parameters that
     * can be used in an expression context, along with all function
     * parameters and 'self'.
     */
    @Override public Node forFnExpr(FnExpr that) {
        Set<Id> staticExprVars = extractStaticExprVars(that.getStaticParams());
        Set<Id> params = extractParamNames(that.getParams());
        ExprDisambiguator v = extend(staticExprVars).extend(params);

        return forFnExprOnly(that,
                             (SimpleName) that.getName().accept(v),
                             v.recurOnListOfStaticParam(that.getStaticParams()),
                             v.recurOnListOfParam(that.getParams()),
                             v.recurOnOptionOfType(that.getReturnType()),
                             (WhereClause) that.getWhere().accept(v),
                             v.recurOnOptionOfListOfTraitType(that.getThrowsClause()),
                             (Expr) that.getBody().accept(v));
    }

    /** LetFns introduce local functions in scope within the body. */
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

    /** IdArgs are static arguments that might be variable references. */
    @Override public Node forIdArg(IdArg that) {
        NodeFactory.makeVarRef(that.getSpan(), that.getName()).accept(this);
        return that;
    }

    /** VarRefs can be made qualified or translated into FnRefs. */
    @Override public Node forVarRef(VarRef that) {
        QualifiedIdName qname = that.getVar();
        Option<APIName> api = qname.getApi();
        Id entity = qname.getName();
        ConsList<Id> fields = ConsList.empty();
        Expr result = null;

        // First, try to interpret it as a qualified name
        while (result == null && api.isSome()) {
            APIName givenApiName = Option.unwrap(api);
            Option<APIName> realApiNameOpt = _env.apiName(givenApiName);

            if (realApiNameOpt.isSome()) {
                APIName realApiName = Option.unwrap(realApiNameOpt);
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
                entity = IterUtil.last(ids);
                Iterable<Id> prefix = IterUtil.skipLast(ids);
                if (IterUtil.isEmpty(prefix)) { api = Option.none(); }
                else { api = Option.some(NodeFactory.makeAPIName(prefix)); }
            }
        }

        // Second, try to interpret it as an unqualified name.
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
                // Turn off error message on this branch until we can ensure
                // that the VarRef doesn't resolve to an inherited method.
                // For now, assume it does refer to an inherited method.
                if (ConsList.isEmpty(fields)) {
                    // no change -- no need to recreate the VarRef
                    return that;
                }
                else {
                    QualifiedIdName newName = NodeFactory.makeQualifiedIdName(entity);
                    result = new VarRef(newName.getSpan(), newName);
                }
                // error("Unrecognized name: " + NodeUtil.nameString(qname), that);
                // return that;
            }
        }

        // result is now non-null
        for (Id field : fields) {
            result = new FieldRef(result, field);
        }
        if (that.isParenthesized()) {
            result = ExprFactory.makeInParentheses(result);
        }
        return result;
    }

}
