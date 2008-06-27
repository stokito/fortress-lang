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
import com.sun.fortress.useful.NI;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.nodes_util.ExprFactory;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.Span;
import com.sun.fortress.compiler.GlobalEnvironment;
import com.sun.fortress.compiler.index.ApiIndex;
import com.sun.fortress.compiler.typechecker.TypeEnv;
import com.sun.fortress.exceptions.StaticError;

import static edu.rice.cs.plt.tuple.Option.*;

/**
 * <p>Eliminates ambiguities in an AST that can be resolved solely by knowing what kind
 * of entity a name refers to.  This class assumes all types in declarations have been
 * resolved, and specifically handles the following:
 * <ul>
 * <li>All names referring to APIs are made fully qualified (FnRefs and OpExprs may then
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
 * <li>TODO: StaticArgs of FnRefs, and types nested within them, are disambiguated.</li>
 * </ul>
 *
 * Additionally, all name references that are undefined or used incorrectly are
 * treated as static errors.  (TODO: check names in non-type static args)</p>
 */
public class ExprDisambiguator extends NodeUpdateVisitor {

    private NameEnv _env;
    private Set<IdOrOpOrAnonymousName> _onDemandImports;
    private List<StaticError> _errors;
    private Option<Id> _innerMostLabel;

    public ExprDisambiguator(NameEnv env, Set<IdOrOpOrAnonymousName> onDemandImports,
                             List<StaticError> errors) {
        _env = env;
        _onDemandImports = onDemandImports;
        _errors = errors;
        _innerMostLabel = Option.<Id>none();
    }

    private ExprDisambiguator(NameEnv env, Set<IdOrOpOrAnonymousName> onDemandImports,
                               List<StaticError> errors, Option<Id> innerMostLabel) {
        this(env, onDemandImports, errors);
        _innerMostLabel = innerMostLabel;
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
        else { return extractParamNames(params.unwrap()); }
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
                                   v.recurOnListOfBaseType(that.getExcludes()),
                                   v.recurOnOptionOfListOfBaseType(that.getComprises()),
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
                                v.recurOnListOfBaseType(that.getExcludes()),
                                v.recurOnOptionOfListOfBaseType(that.getComprises()),
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
                                   v.recurOnOptionOfListOfBaseType(that.getThrowsClause()),
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
                                   v.recurOnOptionOfListOfBaseType(that.getThrowsClause()),
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
                                (IdOrOpOrAnonymousName) that.getName().accept(v),
                                v.recurOnListOfStaticParam(that.getStaticParams()),
                                v.recurOnListOfParam(that.getParams()),
                                v.recurOnOptionOfType(that.getReturnType()),
                                v.recurOnOptionOfListOfBaseType(that.getThrowsClause()),
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
                            (IdOrOpOrAnonymousName) that.getName().accept(v),
                            v.recurOnListOfStaticParam(that.getStaticParams()),
                            v.recurOnListOfParam(that.getParams()),
                            v.recurOnOptionOfType(that.getReturnType()),
                            v.recurOnOptionOfListOfBaseType(that.getThrowsClause()),
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
                             (IdOrOpOrAnonymousName) that.getName().accept(v),
                             v.recurOnListOfStaticParam(that.getStaticParams()),
                             v.recurOnListOfParam(that.getParams()),
                             v.recurOnOptionOfType(that.getReturnType()),
                             (WhereClause) that.getWhere().accept(v),
                             v.recurOnOptionOfListOfBaseType(that.getThrowsClause()),
                             (Expr) that.getBody().accept(v));
    }

    /** LetFns introduce local functions in scope within the body. */
    @Override public Node forLetFn(LetFn that) {
      List<FnDef> fnsResult = recurOnListOfFnDef(that.getFns());
      Set<IdOrOpOrAnonymousName> definedNames = extractDefinedFnNames(fnsResult);
      NameEnv newEnv = new LocalFnEnv(_env, definedNames);
      ExprDisambiguator v = new ExprDisambiguator(newEnv, _onDemandImports, _errors);
      List<Expr> bodyResult = v.recurOnListOfExpr(that.getBody());
      return forLetFnOnly(that, bodyResult, fnsResult);
    }

    private Set<IdOrOpOrAnonymousName> extractDefinedFnNames(Iterable<FnDef> fnDefs) {
      Set<IdOrOpOrAnonymousName> result = new HashSet<IdOrOpOrAnonymousName>();
      for (FnDef fd : fnDefs) { result.add(fd.getName()); }
      // multiple instances of the same name are allowed
      return result;
    }

 /** VarRefs can be made qualified or translated into FnRefs. */
    @Override public Node forVarRef(VarRef that) {
        Id name = that.getVar();
        Option<APIName> api = name.getApi();
        ConsList<Id> fields = ConsList.empty();
        Expr result = null;

        // First, try to interpret it as a qualified name
        while (result == null && api.isSome()) {
            APIName givenApiName = api.unwrap();
            Option<APIName> realApiNameOpt = _env.apiName(givenApiName);

            if (realApiNameOpt.isSome()) {
                APIName realApiName = realApiNameOpt.unwrap();
                Id newId = NodeFactory.makeId(realApiName, name);
                if (_env.hasQualifiedVariable(newId)) {
                    if (fields.isEmpty() && givenApiName == realApiName) {
                        // no change -- no need to recreate the VarRef
                        return that;
                    }
                    else { result = new VarRef(newId.getSpan(), newId); }
                }
                else if (_env.hasQualifiedFunction(newId)) {
                    result = ExprFactory.makeFnRef(newId, name);

                    // TODO: insert correct number of to-infer arguments?
                }
                else {
                    error("Unrecognized name: " + NodeUtil.nameString(name), that);
                    return that;
                }
            }

            else {
                // shift all names to the right, and try a smaller api name
                List<Id> ids = givenApiName.getIds();
                fields = ConsList.cons(name, fields);
                name = IterUtil.last(ids);
                Iterable<Id> prefix = IterUtil.skipLast(ids);
                if (IterUtil.isEmpty(prefix)) { api = Option.none(); }
                else { api = Option.some(NodeFactory.makeAPIName(prefix)); }
            }
        }

        // Second, try to interpret it as an unqualified name.
        if (result == null) {
            // api.isNone() must be true
            Set<Id> vars = _env.explicitVariableNames(name);
            Set<Id> fns = _env.explicitFunctionNames(name);
            if (vars.isEmpty() && fns.isEmpty()) {
                vars = _env.onDemandVariableNames(name);
                fns = _env.onDemandFunctionNames(name);
                _onDemandImports.add(name);
            }

            if (vars.size() == 1 && fns.isEmpty()) {
                Id newName = IterUtil.first(vars);

                if (newName.getApi().isNone() && newName == name && fields.isEmpty()) {
                    // no change -- no need to recreate the VarRef
                    return that;
                }
                else { result = new VarRef(that.getSpan(), newName); }
            }
            else if (vars.isEmpty() && !fns.isEmpty()) {
                result = ExprFactory.makeFnRef(name,CollectUtil.makeList(fns));
                // TODO: insert correct number of to-infer arguments?
            }
            else if (!vars.isEmpty() || !fns.isEmpty()) {
                Set<Id> varsAndFns = CollectUtil.union(vars, fns);
                error("Name may refer to: " + NodeUtil.namesString(varsAndFns), name);
                return that;
            }
            else {
                // Turn off error message on this branch until we can ensure
                // that the VarRef doesn't resolve to an inherited method.
                // For now, assume it does refer to an inherited method.
                if (fields.isEmpty()) {
                    // no change -- no need to recreate the VarRef
                    return that;
                }
                else {
                    result = new VarRef(name.getSpan(), name);
                }
                // error("Unrecognized name: " + NodeUtil.nameString(name), that);
                // return that;
            }
        }

        // result is now non-null
        for (Id field : fields) {
            result = ExprFactory.makeFieldRef(result, field);
        }
        if (that.isParenthesized()) {
            result = ExprFactory.makeInParentheses(result);
        }
        return result;
    }

 @Override
 public Node forAccumulatorOnly(Accumulator that,
   List<StaticArg> staticArgs_result, OpName opr_result,
   List<GeneratorClause> gens_result, Expr body_result) {
  // This method is currently a complete special case that we
  // are only using because the Accumulator node of the AST
  // doesn't hold an OpRef, which I believe it should. NEB
  OpName acc_op = that.getOpr();
  Set<OpName> ops = _env.explicitFunctionNames(acc_op);
  if( ops.isEmpty() ) {
   ops = _env.onDemandFunctionNames(acc_op);
   _onDemandImports.add(acc_op);
  }
  if( ops.isEmpty() ) {
   return that;
  }

  // This is where the hacking comes in
  if( ops.size() > 1 ) {
   return NI.nyi("This means that we need to change the AST.");
  }
  Expr result = new Accumulator(that.getSpan(),
                          that.isParenthesized(),
                          staticArgs_result,
                          IterUtil.first(ops),
                          gens_result,
                          body_result);
  return result;
 }

 
 
    @Override
 public Node for_RewriteObjectRef(_RewriteObjectRef that) {
     Id obj_name = that.getObj();
     
     Set<Id> objs = _env.explicitTypeConsNames(obj_name);
     if( objs.isEmpty() ) {
      objs = _env.onDemandTypeConsNames(obj_name);
      _onDemandImports.add(obj_name);
     }
     
     if( objs.isEmpty() ) {
      return that;
     }
     else if( objs.size() == 1 ) {
      return new _RewriteObjectRef(that.getSpan(), IterUtil.first(objs), that.getStaticArgs());
     }
     else {
      error("Name may refer to: " + NodeUtil.namesString(objs), that);
      return that;
     }
 }

 @Override
 public Node forFnRef(FnRef that) {
  // Many FnRefs will be covered by the VarRef case, since many functions are parsed
     // as variables. FnRefs can be parsed if, for example, explicit static arguments are
     // provided. These function references must still be disambiguated.

     Id fn_name = IterUtil.first(that.getFns());
     Set<Id> fns = _env.explicitFunctionNames(fn_name);
     if( fns.isEmpty() ) {
      fns = _env.onDemandFunctionNames(fn_name);
      _onDemandImports.add(fn_name);
     }

     if( fns.isEmpty() ) {
      // Could be a singleton object with static arguments.
      Set<Id> types = _env.explicitTypeConsNames(fn_name);
      if( types.isEmpty() ) {
       types = _env.onDemandTypeConsNames(fn_name);
       _onDemandImports.add(fn_name);
      }
      if( !types.isEmpty() ) {
       // create _RewriteObjectRef
       _RewriteObjectRef obj = new _RewriteObjectRef(that.getSpan(), fn_name, that.getStaticArgs());
       return obj.accept(this);
      }
      else {
       //error("Function " + that + " could not be disambiguated.", that);
       // TODO: The above line is giving fits to the tests, but it'd be nice to pass.
       return ExprFactory.makeFnRef(that.getSpan(), that.isParenthesized(), fn_name,
         that.getFns(), that.getStaticArgs());
      }
     }

     return ExprFactory.makeFnRef(that.getSpan(), that.isParenthesized(), fn_name,
       CollectUtil.makeList(fns), that.getStaticArgs());
 }

 // Note how this method does not delegate to
    // forOp().
    @Override public Node forOpRef(OpRef that) {
        OpName entity = IterUtil.first(that.getOps());
        Set<OpName> ops = _env.explicitFunctionNames(entity);
        if (ops.isEmpty()) {
            ops = _env.onDemandFunctionNames(entity);
            _onDemandImports.add(entity);
        }

        if (ops.isEmpty()) {
            //System.err.println("OpRef:" + entity);
         return new OpRef(that.getSpan(),that.isParenthesized(),entity,that.getOps(),that.getStaticArgs());
        }

        //System.err.println("OpRef:" + entity);
        return new OpRef(that.getSpan(),that.isParenthesized(),entity,CollectUtil.makeList(ops),that.getStaticArgs());
    }

    @Override public Node forLabel(Label that) {
        Id newName = (Id) that.getName().accept(this);
        ExprDisambiguator dis = new ExprDisambiguator(_env,
                                                      _onDemandImports, _errors,
                                                      Option.wrap(that.getName()));
        Block newBody = (Block) that.getBody().accept(dis);
        return super.forLabelOnly(that, newName, newBody);
    }

    @Override public Node forExitOnly(Exit that, Option<Id> target_result, Option<Expr> returnExpr_result) {
        Option<Id> target = target_result.isSome() ? target_result
                                                   : _innerMostLabel;
        Option<Expr> with = returnExpr_result.isSome() ? returnExpr_result
                                                       : wrap((Expr)new VoidLiteralExpr(that.getSpan()));
        if (target.isNone()) {
            error("Exit occurs outside of a label", that);
        }
        Exit newExit = new Exit(that.getSpan(), that.isParenthesized(), target, with);
        if (newExit.equals(that)) {
            return that;
        } else {
            return newExit;
        }
    }
}
