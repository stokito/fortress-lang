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

package com.sun.fortress.compiler.typechecker;


import com.sun.fortress.compiler.*;
import com.sun.fortress.compiler.index.ApiIndex;
import com.sun.fortress.compiler.index.ComponentIndex;
import com.sun.fortress.compiler.index.FunctionalMethod;
import com.sun.fortress.compiler.index.TraitIndex;
import com.sun.fortress.compiler.index.Method;
import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.Span;
import com.sun.fortress.useful.NI;
import com.sun.fortress.useful.Pair;
import edu.rice.cs.plt.collect.HashRelation;
import edu.rice.cs.plt.collect.Relation;
import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.lambda.Lambda;
import edu.rice.cs.plt.lambda.Lambda2;
import edu.rice.cs.plt.tuple.Option;
import java.util.*;

import java.io.PrintWriter;

import static com.sun.fortress.compiler.TypeError.errorMsg;
import static edu.rice.cs.plt.tuple.Option.*;

public class TypeChecker extends NodeDepthFirstVisitor<TypeCheckerResult> {

    private TraitTable table;
    private StaticParamEnv staticParams;
    private TypeEnv params;
    private final SubtypeChecker subtypeChecker;


    public TypeChecker(TraitTable _table,
                       StaticParamEnv _staticParams,
                       TypeEnv _params)
    {
        table = _table;
        staticParams = _staticParams;
        params = _params;
        subtypeChecker = SubtypeChecker.make(table);
    }

    private TypeChecker(TraitTable _table,
                       StaticParamEnv _staticParams,
                       TypeEnv _params,
                       SubtypeChecker _subtypeChecker)
    {
        table = _table;
        staticParams = _staticParams;
        params = _params;
        subtypeChecker = _subtypeChecker;
    }

    private static Type typeFromLValueBinds(List<LValueBind> bindings) {
        List<Type> results = new ArrayList<Type>();

        for (LValueBind binding: bindings) {
            results.add(unwrap(binding.getType()));
        }
        return NodeFactory.makeTupleType(results);
    }

    private TypeChecker extend(List<StaticParam> newStaticParams, Option<List<Param>> newParams, WhereClause whereClause) {
        return new TypeChecker(table, staticParams.extend(newStaticParams, whereClause),
                               params.extend(newParams), subtypeChecker.extend(newStaticParams, whereClause));
    }

    private TypeChecker extend(List<StaticParam> newStaticParams, List<Param> newParams, WhereClause whereClause) {
        return new TypeChecker(table, staticParams.extend(newStaticParams, whereClause),
                               params.extendWithParams(newParams), subtypeChecker.extend(newStaticParams, whereClause));
    }

    private TypeChecker extend(List<StaticParam> newStaticParams, WhereClause whereClause) {
        return new TypeChecker(table,
                               staticParams.extend(newStaticParams, whereClause),
                               params,
                               subtypeChecker.extend(newStaticParams, whereClause));
    }

    private TypeChecker extend(WhereClause whereClause) {
        return new TypeChecker(table,
                               staticParams.extend(Collections.<StaticParam>emptyList(), whereClause),
                               params,
                               subtypeChecker.extend(Collections.<StaticParam>emptyList(), whereClause));
    }

    private TypeChecker extend(List<LValueBind> bindings) {
        return new TypeChecker(table, staticParams,
                               params.extendWithLValues(bindings),
                               subtypeChecker);
    }

    private TypeChecker extend(LocalVarDecl decl) {
        return new TypeChecker(table, staticParams,
                               params.extend(decl),
                               subtypeChecker);
    }

    private TypeChecker extend(Param newParam) {
        return new TypeChecker(table, staticParams,
                               params.extend(newParam),
                               subtypeChecker);
    }

    public TypeChecker extendWithMethods(Relation<SimpleName, Method> methods) {
        return new TypeChecker(table, staticParams,
                               params.extendWithMethods(methods),
                               subtypeChecker);
    }

    public TypeChecker extendWithFunctions(Relation<SimpleName, FunctionalMethod> methods) {
        return new TypeChecker(table, staticParams,
                               params.extendWithFunctions(methods),
                               subtypeChecker);
    }

    public TypeChecker extendWithFnDefs(Relation<SimpleName, ? extends FnDef> fns) {
        return new TypeChecker(table, staticParams,
                               params.extendWithFnDefs(fns),
                               subtypeChecker);
    }

    /**
     * Check the subtype relation for the given types.  If subtype <: supertype, then a TypeCheckerResult
     * for the given node and corresponding type constraints will be returned.  Otherwise, a TypeCheckerResult
     * for the given node with a generic error message will be returned.
     */
    private TypeCheckerResult checkSubtype(Type subtype, Type supertype, Node ast) {
        return checkSubtype(subtype,
                            supertype,
                            ast,
                            TypeError.make(errorMsg("Expected expression of type ", supertype, " ",
                                                    "but was type ", subtype),
                                           ast));
    }

    /**
     * Check the subtype relation for the given types.  If subtype <: supertype, then a TypeCheckerResult
     * for the given node and corresponding type constraints will be returned.  Otherwise, a TypeCheckerResult
     * for the given node with the given TypeError will be returned.
     */
    private TypeCheckerResult checkSubtype(Type subtype, Type supertype, Node ast, StaticError error) {
        System.err.printf("checkSubtype: %s <: %s", subtype, supertype);
        if (!subtypeChecker.subtype(subtype, supertype)) {
            System.err.println(" = FALSE");
            return new TypeCheckerResult(ast, error);
        } else {
            System.err.println(" = TRUE");
            return new TypeCheckerResult(ast);
        }
    }

    /** Ignore unsupported nodes for now. */
    /*public TypeCheckerResult defaultCase(Node that) {
        return new TypeCheckerResult(that, Types.VOID, IterUtil.<TypeError>empty());
    }*/

    public TypeCheckerResult forFnDef(FnDef that) {
        TypeChecker newChecker = this.extend(that.getStaticParams(), that.getParams(), that.getWhere());

        TypeCheckerResult contractResult = that.getContract().accept(newChecker);
        TypeCheckerResult bodyResult = that.getBody().accept(newChecker);

        return new TypeCheckerResult(new FnDef(that.getSpan(),
                                               that.getMods(),
                                               that.getName(),
                                               that.getStaticParams(),
                                               that.getParams(),
                                               that.getReturnType(),
                                               that.getThrowsClause(),
                                               that.getWhere(),
                                               (Contract)contractResult.ast(),
                                               that.getSelfName(),
                                               (Expr)bodyResult.ast()),
                                     IterUtil.compose(contractResult.errors(), bodyResult.errors()));
    }


    public TypeCheckerResult forVarDecl(VarDecl that) {
        // System.err.println("forVarDecl: " + that);
        List<LValueBind> lhs = that.getLhs();
        Expr init = that.getInit();

        TypeCheckerResult initResult = init.accept(this);

        if (lhs.size() == 1) { // We have a single variable binding, not a tuple binding
            LValueBind var = lhs.get(0);
            Option<Type> varType = var.getType();
            if (varType.isSome()) {
                // System.err.println("varType: " + unwrap(varType));
                // System.err.println("initResult.type(): " + initResult.type());
                if (initResult.type().isNone()) {
                    // System.err.println("initResult.ast(): " + initResult.ast());
                    // The right hand side could not be typed, which must have resulted in a
                    // signaled error. No need to signal another error.
                    return TypeCheckerResult.compose(new VarDecl(that.getSpan(),
                                                                 lhs,
                                                                 (Expr)initResult.ast()),
                                                     initResult);
                }
                return checkSubtype(unwrap(initResult.type()),
                                    unwrap(varType),
                                    that,
                                    TypeError.make(errorMsg("Attempt to define variable ", var, " ",
                                                            "with an expression of type ", unwrap(initResult.type())),
                                                   that));
            } else { // Eventually, this case will involve type inference
                // System.err.println("varType.isNone()");
                return NI.nyi();
            }
        } else { // lhs.size() >= 2
            // System.err.println("lhs.size() >= 2");
            Type varType = typeFromLValueBinds(lhs);
            if (initResult.type().isNone()) {
                // The right hand side could not be typed, which must have resulted in a
                // signaled error. No need to signal another error.
                return TypeCheckerResult.compose(new VarDecl(that.getSpan(),
                                                             lhs,
                                                             (Expr)initResult.ast()),
                                                 initResult);
            }
            return checkSubtype(unwrap(initResult.type()),
                                varType,
                                that,
                                TypeError.make(errorMsg("Attempt to define variables ", lhs, " ",
                                                        "with an expression of type ", unwrap(initResult.type())),
                                               that));
        }
    }

    public TypeCheckerResult forId(Id that) {
        Option<Type> thatType = params.type(that);

        if (thatType.isSome()) {
            return new TypeCheckerResult(that, unwrap(thatType));
        } else {
            StaticError error =
                TypeError.make(errorMsg("Attempt to reference an unbound identifier: ", that), that);
            return new TypeCheckerResult(that, error);
        }
    }

    public TypeCheckerResult forAPIName(APIName that) {
        return new TypeCheckerResult(that);
    }

    public TypeCheckerResult forExportOnly(Export that, List<TypeCheckerResult> apis_result) {
        return new TypeCheckerResult(that);
    }

    public TypeCheckerResult forQualifiedIdName(QualifiedIdName that) {
        Id name = that.getName();
        Option<APIName> apiName = that.getApi();
        if (apiName.isSome()) {
            // 'globals' must contain 'unwrap(apiName)', as this component has
            // been disambiguated already.
            TypeEnv apiTypeEnv = TypeEnv.make(table.compilationUnit(unwrap(apiName)));
            Option<Type> type = apiTypeEnv.type(name);
            if (type.isSome()) {
                return new TypeCheckerResult(that,
                                             NodeFactory.makeNamedType(unwrap(apiName),
                                                                       (NamedType)unwrap(type)));
            } else {
                StaticError error =
                    TypeError.make(errorMsg("Attempt to reference unbound variable: ", that),
                                   that);
                return new TypeCheckerResult(that, error);
            }
        }
        Option<Type> type = params.type(name);
        if (type.isSome()) {
            return new TypeCheckerResult(that, unwrap(type));
        } else {
            StaticError error =
                TypeError.make(errorMsg("Attempt to reference unbound variable: ", that),
                               that);
            return new TypeCheckerResult(that, error);
        }
    }

    public TypeCheckerResult forVarRefOnly(VarRef that, TypeCheckerResult var_result) {
        Option<Type> varType = var_result.type();

        if (varType.isSome()) {
            return TypeCheckerResult.compose(that, unwrap(varType), var_result);
        } else {
            return TypeCheckerResult.compose(that, var_result);
        }
    }

    public TypeCheckerResult forObjectDecl(ObjectDecl that) {
        TypeCheckerResult modsResult = TypeCheckerResult.compose(that, recurOnListOfModifier(that.getMods()));
        TypeCheckerResult nameResult = that.getName().accept(this);
        TypeCheckerResult staticParamsResult = TypeCheckerResult.compose(that, recurOnListOfStaticParam(that.getStaticParams()));
        TypeCheckerResult extendsClauseResult = TypeCheckerResult.compose(that, recurOnListOfTraitTypeWhere(that.getExtendsClause()));
        TypeCheckerResult whereResult = that.getWhere().accept(this);
        TypeCheckerResult paramsResult = TypeCheckerResult.compose(that, recurOnOptionOfListOfParam(that.getParams()));
        TypeCheckerResult throwsClauseResult = TypeCheckerResult.compose(that, recurOnOptionOfListOfTraitType(that.getThrowsClause()));

        TypeChecker newChecker = this.extend(that.getStaticParams(), that.getParams(), that.getWhere());
        TypeCheckerResult contractResult = that.getContract().accept(newChecker);

        // Check field declarations.
        TypeCheckerResult fieldsResult = new TypeCheckerResult(that);
        for (Decl decl: that.getDecls()) {
            if (decl instanceof VarDecl) {
                VarDecl _decl = (VarDecl)decl;
                fieldsResult = TypeCheckerResult.compose(that, _decl.accept(newChecker), fieldsResult);
                newChecker = newChecker.extend(_decl.getLhs());
            }
        }

        // Check method declarations.

        TraitIndex thatIndex = (TraitIndex)table.typeCons(that.getName());
        newChecker = newChecker.extendWithMethods(thatIndex.dottedMethods());
        newChecker = newChecker.extendWithFunctions(thatIndex.functionalMethods());

        TypeCheckerResult methodsResult = new TypeCheckerResult(that);
        for (Decl decl: that.getDecls()) {
            if (decl instanceof FnDecl) {
                methodsResult = TypeCheckerResult.compose(that, decl.accept(newChecker), methodsResult);
            }
        }

        return TypeCheckerResult.compose(that, modsResult, nameResult, staticParamsResult,
                                         extendsClauseResult, whereResult, paramsResult, throwsClauseResult,
                                         contractResult, fieldsResult, methodsResult);
    }

    public TypeCheckerResult forTraitDecl(TraitDecl that) {
        TypeCheckerResult modsResult = TypeCheckerResult.compose(that, recurOnListOfModifier(that.getMods()));
        TypeCheckerResult nameResult = that.getName().accept(this);
        TypeCheckerResult staticParamsResult = TypeCheckerResult.compose(that, recurOnListOfStaticParam(that.getStaticParams()));
        TypeCheckerResult extendsClauseResult = TypeCheckerResult.compose(that, recurOnListOfTraitTypeWhere(that.getExtendsClause()));
        TypeCheckerResult whereResult = that.getWhere().accept(this);
        TypeCheckerResult excludesResult = TypeCheckerResult.compose(that, recurOnListOfTraitType(that.getExcludes()));

        TypeCheckerResult comprisesResult = new TypeCheckerResult(that);
        Option<List<TraitType>> comprises = that.getComprises();
        if (comprises.isSome()) {
            comprisesResult =
                TypeCheckerResult.compose
                    (that, unwrap(recurOnOptionOfListOfTraitType(that.getComprises())));
        }

        TypeChecker newChecker = this.extend(that.getStaticParams(), that.getWhere());

        // Check "field" declarations (really getter and setter declarations).
        TypeCheckerResult fieldsResult = new TypeCheckerResult(that);
        for (Decl decl: that.getDecls()) {
            if (decl instanceof VarDecl) {
                VarDecl _decl = (VarDecl)decl;

                fieldsResult = TypeCheckerResult.compose(that, _decl.accept(newChecker), fieldsResult);
                newChecker = newChecker.extend(_decl.getLhs());
            }
        }

        // Check method declarations.

        TraitIndex thatIndex = (TraitIndex)table.typeCons(that.getName());
        newChecker = newChecker.extendWithMethods(thatIndex.dottedMethods());
        newChecker = newChecker.extendWithFunctions(thatIndex.functionalMethods());

        TypeCheckerResult methodsResult = new TypeCheckerResult(that);
        for (Decl decl: that.getDecls()) {
            if (decl instanceof FnDecl) {
                methodsResult = TypeCheckerResult.compose(that, decl.accept(newChecker), methodsResult);
            }
        }

        return TypeCheckerResult.compose(that, modsResult, nameResult, staticParamsResult,
                                         extendsClauseResult, whereResult, excludesResult, comprisesResult,
                                         fieldsResult, methodsResult);
    }

//    public TypeCheckerResult forVarRefOnly(VarRef that, TypeCheckerResult var_result) {
//        if (var_result.isSome()) {
//            return new TypeCheckerResult(that, varType);
//        } else {
//            TypeError error =
//                TypeError.make(errorMsg("Attempt to reference unbound variable: ", that),
//                                 that);
//            return new TypeCheckerResult(that, error);
//        }
//    }

    public TypeCheckerResult forIfOnly(If that,
                                       List<TypeCheckerResult> clauses_result,
                                       Option<TypeCheckerResult> elseClause_result) {

        if (elseClause_result.isSome()) {
            System.err.println("forIfOnly has else");

            TypeCheckerResult result = new TypeCheckerResult(that);
            Type clauseType = Types.BOTTOM;
            TypeCheckerResult elseResult = unwrap(elseClause_result);

            // Get union of all clauses' types
            for (TypeCheckerResult clauseResult : clauses_result) {
                if (clauseResult.type().isSome()) {
                    clauseType = new OrType(clauseType, unwrap(clauseResult.type()));
                } else {
                    result = TypeCheckerResult.compose(that, clauseResult, result);
                }
            }
            if (elseResult.type().isSome()) {
                clauseType = new OrType(clauseType, unwrap(elseResult.type()));
            } else {
                result = TypeCheckerResult.compose(that, elseResult, result);
            }
            return TypeCheckerResult.compose(that, clauseType, result);
        } else {
            System.err.println("forIfOnly does not have else");

            // Check that each if/elif clause has void type
            TypeCheckerResult result = new TypeCheckerResult(that);
            for (TypeCheckerResult clauseResult : clauses_result) {
                if (clauseResult.type().isSome()) {
                    Type clauseType = unwrap(clauseResult.type());
                    result = TypeCheckerResult.compose(
                        that,
                        checkSubtype(clauseType,
                                     Types.VOID,
                                     that,
                                     TypeError.make(errorMsg("An 'if' clause without corresponding 'else' has type ",
                                                             clauseType, " instead of type ()"),
                                                    clauseResult.ast())),
                        result);
                } else {
                    result = TypeCheckerResult.compose(that, clauseResult, result);
                }
            }
            return TypeCheckerResult.compose(that, Types.VOID, result);
        }
    }

    public TypeCheckerResult forIfClauseOnly(IfClause that,
                                             TypeCheckerResult test_result,
                                             TypeCheckerResult body_result) {

        TypeCheckerResult result = new TypeCheckerResult(that);

        // Check that test condition is Boolean.
        if (test_result.type().isSome()) {
            Type testType = unwrap(test_result.type());
            result = TypeCheckerResult.compose(
                that,
                checkSubtype(testType,
                             Types.BOOLEAN,
                             that,
                             TypeError.make(errorMsg("Attempt to use expression of type ", testType, " ",
                                                     "as a test condition"),
                                            that)),
                result);
        } else {
            result = TypeCheckerResult.compose(that, test_result, result);
        }

        // IfClause's type is body's type.
        if (body_result.type().isSome()) {
            return TypeCheckerResult.compose(that, unwrap(body_result.type()), result);
        } else {
            return TypeCheckerResult.compose(that, body_result, result);
        }
    }

    public TypeCheckerResult forDoOnly(Do that, List<TypeCheckerResult> fronts_result) {
        TypeCheckerResult result = new TypeCheckerResult(that);
        Type frontType = Types.BOTTOM;

        // Get union of all clauses' types
        for (TypeCheckerResult frontResult : fronts_result) {
            if (frontResult.type().isSome()) {
                frontType = new OrType(frontType, unwrap(frontResult.type()));
            } else {
                result = TypeCheckerResult.compose(that, frontResult, result);
            }
        }
        return TypeCheckerResult.compose(that, frontType, result);
    }


    public TypeCheckerResult forDoFrontOnly(DoFront that,
                                            Option<TypeCheckerResult> loc_result,
                                            TypeCheckerResult expr_result) {
        if (loc_result.isSome()) {
            TypeCheckerResult _loc_result = unwrap(loc_result);
            if (_loc_result.type().isSome()) {
                return TypeCheckerResult.compose(that,
                                                 checkSubtype(unwrap(_loc_result.type()), Types.REGION, _loc_result.ast()));
            } else {
                return TypeCheckerResult.compose(that, expr_result.type(), _loc_result, expr_result);
            }
        } else {
            return TypeCheckerResult.compose(that, expr_result.type(), expr_result);
        }
    }

    public TypeCheckerResult forFnRefOnly(FnRef that,
                                          List<TypeCheckerResult> fns_result,
                                          List<TypeCheckerResult> staticArgs_result) {

        // Get intersection of overloaded function types.
        Type overloadedType = Types.ANY;
        for (TypeCheckerResult fn_result : fns_result) {
            if (fn_result.type().isSome()) {
              overloadedType = new AndType(overloadedType, unwrap(fn_result.type()));
            }
        }
        return TypeCheckerResult.compose(that,
                                         overloadedType,
                                         TypeCheckerResult.compose(that, fns_result),
                                         TypeCheckerResult.compose(that, staticArgs_result));
    }

    public TypeCheckerResult forBlockOnly(Block that, List<TypeCheckerResult> exprs_result) {
        return TypeCheckerResult.compose(that, exprs_result);
    }

    public TypeCheckerResult forLetFn(LetFn that) {
        TypeCheckerResult result = new TypeCheckerResult(that);
        Relation<SimpleName, FnDef> fnDefs = new HashRelation<SimpleName, FnDef>(true, false);
        for (FnDef fnDef : that.getFns()) {
            fnDefs.add(fnDef.getName(), fnDef);
        }

        TypeChecker newChecker = this.extendWithFnDefs(fnDefs);
        for (FnDef fnDef : that.getFns()) {
            result = TypeCheckerResult.compose(that, fnDef.accept(newChecker), result);
        }
        result = TypeCheckerResult.compose(that,
                                           TypeCheckerResult.compose(that,
                                                                     newChecker.recurOnListOfExpr(that.getBody())),
                                           result);
        return result;
    }

    public TypeCheckerResult forLocalVarDecl(LocalVarDecl that) {
        TypeCheckerResult result = new TypeCheckerResult(that);
        if (that.getRhs().isSome()) {
            result = TypeCheckerResult.compose(that, unwrap(that.getRhs()).accept(this), result);
        }
        TypeChecker newChecker = this.extend(that);
        result = TypeCheckerResult.compose(that,
                                           TypeCheckerResult.compose(that,
                                                                     newChecker.recurOnListOfExpr(that.getBody())),
                                           result);
        return result;
    }

    public TypeCheckerResult forFloatLiteralExpr(FloatLiteralExpr that) {
        return new TypeCheckerResult(that, Types.FLOAT_LITERAL);
    }

    public TypeCheckerResult forIntLiteralExpr(IntLiteralExpr that) {
        return new TypeCheckerResult(that, Types.INT_LITERAL);
    }

    public TypeCheckerResult forCharLiteralExpr(CharLiteralExpr that) {
        return new TypeCheckerResult(that, Types.CHAR);
    }

    public TypeCheckerResult forStringLiteralExpr(StringLiteralExpr that) {
        return new TypeCheckerResult(that, Types.STRING);
    }

    public TypeCheckerResult forVoidLiteralExpr(VoidLiteralExpr that) {
        return new TypeCheckerResult(that, Types.VOID);
    }
    
    public TypeCheckerResult forInstantiatedType(InstantiatedType that) {
        return new TypeCheckerResult(that);
    }

    public TypeCheckerResult forArgExprOnly(ArgExpr that,
                                              List<TypeCheckerResult> exprs_result,
                                              Option<TypeCheckerResult> varargs_result,
                                              List<TypeCheckerResult> keywords_result) {
        if (varargs_result.isSome()) {
            return TypeCheckerResult.compose(that,
                                             TypeCheckerResult.compose(that, exprs_result),
                                             TypeCheckerResult.compose(that, unwrap(varargs_result)),
                                             TypeCheckerResult.compose(that, keywords_result));
        } else {
            return TypeCheckerResult.compose(that,
                                             TypeCheckerResult.compose(that, exprs_result),
                                             TypeCheckerResult.compose(that, keywords_result));
        }
    }

    public TypeCheckerResult forAsExprOnly(AsExpr that,
                                           TypeCheckerResult expr_result,
                                           TypeCheckerResult type_result) {

        // Check that expression type <: ascripted type.
        Type ascriptedType = (Type) type_result.ast();
        if (expr_result.type().isSome()) {
            Type exprType = unwrap(expr_result.type());
            return TypeCheckerResult.compose(
                that,
                ascriptedType,
                checkSubtype(exprType,
                             ascriptedType,
                             expr_result.ast(),
                             TypeError.make(errorMsg("Attempt to ascript expression of type ",
                                                     exprType, " to non-supertype ", ascriptedType),
                                            expr_result.ast())),
                expr_result,
                type_result);
        } else {
            return TypeCheckerResult.compose(that,
                                           ascriptedType,
                                           expr_result,
                                           type_result);
        }
    }

    public TypeCheckerResult forTupleExprOnly(TupleExpr that,
                                            List<TypeCheckerResult> exprs_result) {
        return TypeCheckerResult.compose(that,
                                         TypeCheckerResult.compose(that, exprs_result));
    }
    
    public TypeCheckerResult forContractOnly(Contract that, 
                                             Option<List<TypeCheckerResult>> requires_result,
                                             Option<List<TypeCheckerResult>> ensures_result,
                                             Option<List<TypeCheckerResult>> invariants_result) {
        TypeCheckerResult result = new TypeCheckerResult(that);

        // Check that each 'requires' expression is Boolean
        if (requires_result.isSome()) {
            for (TypeCheckerResult r : unwrap(requires_result)) {
                if (r.type().isNone()) continue;
                Type exprType = unwrap(r.type());
                result = TypeCheckerResult.compose(
                        that,
                        checkSubtype(exprType,
                                Types.BOOLEAN,
                                r.ast(),
                                TypeError.make(errorMsg("Attempt to use expression of type ",
                                        exprType, " in a 'requires' clause, instead of ",
                                        Types.BOOLEAN),
                                        r.ast())),
                                        result);
            }
        }

        return TypeCheckerResult.compose(that,
                TypeCheckerResult.compose(that, requires_result),
                TypeCheckerResult.compose(that, ensures_result),
                TypeCheckerResult.compose(that, invariants_result),
                                         result);
    }

//    public TypeCheckerResult forChainExpr(ChainExpr that) {
//        TypeCheckerResult result = new TypeCheckerResult(that);
//        TypeCheckerResult first_result = that.getFirst().accept(this);
//        final TypeChecker checker = this;
//        
//        
//        IterUtil.fold(that.getLinks(), first_result, new Lambda2<TypeCheckerResult, Pair<Op, Expr>, TypeCheckerResult>() {
//            public TypeCheckerResult value(TypeCheckerResult r, Pair<Op, Expr> p) {
//                TypeCheckerResult expr_result = p.getB().accept(checker);
//                Option<Type> opType = checker.params.type(p.getA());
//                
//                if (r.type().isSome()) {
//                }
//                return null;
//            }
//        });
//        
//        return null;
//    }
    
    // STUBS --------------------
    // NEED: WhereClause, ChainExpr, EnsuresClause
    
    public TypeCheckerResult forTightJuxtOnly(TightJuxt that, List<TypeCheckerResult> exprs_result) {
        return TypeCheckerResult.compose(that, exprs_result);
    }

    public TypeCheckerResult forComponentOnly(Component that,
                                              TypeCheckerResult name_result,
                                              List<TypeCheckerResult> imports_result,
                                              List<TypeCheckerResult> exports_result,
                                              List<TypeCheckerResult> decls_result) {
        return TypeCheckerResult.compose(that,
                                         name_result,
                                         TypeCheckerResult.compose(that, imports_result),
                                         TypeCheckerResult.compose(that, exports_result),
                                         TypeCheckerResult.compose(that, decls_result));
    }

}

    /* Methods copied from superclass, to make it easier to incrementally define
     * overridings here.
     */

//    public RetType forAbsTraitDecl(AbsTraitDecl that) {
//        List<RetType> staticParams_result = recurOnListOfStaticParam(that.getStaticParams());
//        List<RetType> extendsClause_result = recurOnListOfTraitTypeWhere(that.getExtendsClause());
//        RetType where_result = that.getWhere().accept(this);
//        List<RetType> excludes_result = recurOnListOfTraitType(that.getExcludes());
//        Option<List<RetType>> comprises_result = recurOnOptionOfListOfTraitType(that.getComprises());
//
//
//        List<RetType> decls_result = recurOnListOfAbsDecl(that.getDecls());
//        return forAbsTraitDeclOnly(that, mods_result, name_result, staticParams_result, extendsClause_result,
//                                   where_result, excludes_result, comprises_result, decls_result);
//    }
//
//    public RetType forTraitDecl(TraitDecl that) {
//        List<RetType> mods_result = recurOnListOfModifier(that.getMods());
//        RetType name_result = that.getName().accept(this);
//        List<RetType> staticParams_result = recurOnListOfStaticParam(that.getStaticParams());
//        List<RetType> extendsClause_result = recurOnListOfTraitTypeWhere(that.getExtendsClause());
//        RetType where_result = that.getWhere().accept(this);
//        List<RetType> excludes_result = recurOnListOfTraitType(that.getExcludes());
//        Option<List<RetType>> comprises_result = recurOnOptionOfListOfTraitType(that.getComprises());
//        List<RetType> decls_result = recurOnListOfDecl(that.getDecls());
//        return forTraitDeclOnly(that, mods_result, name_result, staticParams_result, extendsClause_result,
//                                where_result, excludes_result, comprises_result, decls_result);
//    }
//
//    public RetType forAbsObjectDecl(AbsObjectDecl that) {
//        List<RetType> mods_result = recurOnListOfModifier(that.getMods());
//        RetType name_result = that.getName().accept(this);
//        List<RetType> staticParams_result = recurOnListOfStaticParam(that.getStaticParams());
//        List<RetType> extendsClause_result = recurOnListOfTraitTypeWhere(that.getExtendsClause());
//        RetType where_result = that.getWhere().accept(this);
//        Option<List<RetType>> params_result = recurOnOptionOfListOfParam(that.getParams());
//        Option<List<RetType>> throwsClause_result = recurOnOptionOfListOfTraitType(that.getThrowsClause());
//        RetType contract_result = that.getContract().accept(this);
//        List<RetType> decls_result = recurOnListOfAbsDecl(that.getDecls());
//        return forAbsObjectDeclOnly(that, mods_result, name_result, staticParams_result, extendsClause_result,
//                                    where_result, params_result, throwsClause_result, contract_result, decls_result);
//    }

//    public RetType forObjectDecl(ObjectDecl that) {
//        List<RetType> mods_result = recurOnListOfModifier(that.getMods());
//        RetType name_result = that.getName().accept(this);
//        List<RetType> staticParams_result = recurOnListOfStaticParam(that.getStaticParams());
//        List<RetType> extendsClause_result = recurOnListOfTraitTypeWhere(that.getExtendsClause());
//        RetType where_result = that.getWhere().accept(this);
//        Option<List<RetType>> params_result = recurOnOptionOfListOfParam(that.getParams());
//        Option<List<RetType>> throwsClause_result = recurOnOptionOfListOfTraitType(that.getThrowsClause());
//        RetType contract_result = that.getContract().accept(this);
//        List<RetType> decls_result = recurOnListOfDecl(that.getDecls());
//        return forObjectDeclOnly(that, mods_result, name_result, staticParams_result, extendsClause_result,
//                                 where_result, params_result, throwsClause_result, contract_result, decls_result);
//    }
//
//    public RetType forTraitObjectAbsDeclOrDeclOnly(TraitObjectAbsDeclOrDecl that, List<RetType> mods_result, RetType name_result,
//                                                   List<RetType> staticParams_result, List<RetType> extendsClause_result, RetType where_result,
//                                                   List<RetType> decls_result) {
//        return forAbstractNodeOnly(that);
//    }
//
//    public RetType forTraitAbsDeclOrDeclOnly(TraitAbsDeclOrDecl that, List<RetType> mods_result, RetType name_result, List<RetType> staticParams_result,
//                                             List<RetType> extendsClause_result, RetType where_result, List<RetType> excludes_result,
//                                             Option<List<RetType>> comprises_result, List<RetType> decls_result) {
//        return forTraitObjectAbsDeclOrDeclOnly(that, mods_result, name_result, staticParams_result, extendsClause_result, where_result, decls_result);
//    }
//
//    public RetType forAbsTraitDeclOnly(AbsTraitDecl that, List<RetType> mods_result, RetType name_result, List<RetType> staticParams_result,
//                                       List<RetType> extendsClause_result, RetType where_result, List<RetType> excludes_result,
//                                       Option<List<RetType>> comprises_result, List<RetType> decls_result) {
//        return forTraitAbsDeclOrDeclOnly(that, mods_result, name_result, staticParams_result, extendsClause_result,
//                                         where_result, excludes_result, comprises_result, decls_result);
//    }
//
//    public RetType forTraitDeclOnly(TraitDecl that, List<RetType> mods_result, RetType name_result, List<RetType> staticParams_result,
//                                    List<RetType> extendsClause_result, RetType where_result, List<RetType> excludes_result,
//                                    Option<List<RetType>> comprises_result, List<RetType> decls_result) {
//        return forTraitAbsDeclOrDeclOnly(that, mods_result, name_result, staticParams_result, extendsClause_result,
//                                         where_result, excludes_result, comprises_result, decls_result);
//    }
//
//    public RetType forObjectAbsDeclOrDeclOnly(ObjectAbsDeclOrDecl that, List<RetType> mods_result, RetType name_result, List<RetType> staticParams_result,
//                                              List<RetType> extendsClause_result, RetType where_result, Option<List<RetType>> params_result,
//                                              Option<List<RetType>> throwsClause_result, RetType contract_result, List<RetType> decls_result) {
//        return forTraitObjectAbsDeclOrDeclOnly(that, mods_result, name_result, staticParams_result, extendsClause_result, where_result, decls_result);
//    }
//
//    public RetType forAbsObjectDeclOnly(AbsObjectDecl that, List<RetType> mods_result, RetType name_result, List<RetType> staticParams_result,
//                                        List<RetType> extendsClause_result, RetType where_result, Option<List<RetType>> params_result,
//                                        Option<List<RetType>> throwsClause_result, RetType contract_result, List<RetType> decls_result) {
//        return forObjectAbsDeclOrDeclOnly(that, mods_result, name_result, staticParams_result, extendsClause_result,
//                                          where_result, params_result, throwsClause_result, contract_result, decls_result);
//    }
//
//    public RetType forObjectDeclOnly(ObjectDecl that, List<RetType> mods_result, RetType name_result, List<RetType> staticParams_result,
//                                     List<RetType> extendsClause_result, RetType where_result, Option<List<RetType>> params_result,
//                                     Option<List<RetType>> throwsClause_result, RetType contract_result, List<RetType> decls_result) {
//        return forObjectAbsDeclOrDeclOnly(that, mods_result, name_result, staticParams_result, extendsClause_result,
//                                          where_result, params_result, throwsClause_result, contract_result, decls_result);
//    }

//    public RetType forAbstractNodeOnly(AbstractNode that) {
//        return defaultCase(that);
//    }
//
//    public RetType forCompilationUnitOnly(CompilationUnit that, RetType name_result, List<RetType> imports_result) {
//        return forAbstractNodeOnly(that);
//    }
//
//    public RetType forApiOnly(Api that, RetType name_result, List<RetType> imports_result, List<RetType> decls_result) {
//        return forCompilationUnitOnly(that, name_result, imports_result);
//    }
//
//    public RetType forImportOnly(Import that) {
//        return forAbstractNodeOnly(that);
//    }
//
//    public RetType forImportedNamesOnly(ImportedNames that, RetType api_result) {
//        return forImportOnly(that);
//    }
//
//    public RetType forImportStarOnly(ImportStar that, RetType api_result, List<RetType> except_result) {
//        return forImportedNamesOnly(that, api_result);
//    }
//
//    public RetType forImportNamesOnly(ImportNames that, RetType api_result, List<RetType> aliasedNames_result) {
//        return forImportedNamesOnly(that, api_result);
//    }
//
//    public RetType forImportApiOnly(ImportApi that, List<RetType> apis_result) {
//        return forImportOnly(that);
//    }
//
//    public RetType forAliasedSimpleNameOnly(AliasedSimpleName that, RetType name_result, Option<RetType> alias_result) {
//        return forAbstractNodeOnly(that);
//    }
//
//    public RetType forAliasedAPINameOnly(AliasedAPIName that, RetType api_result, Option<RetType> alias_result) {
//        return forAbstractNodeOnly(that);
//    }
//
//    public RetType forExportOnly(Export that, List<RetType> apis_result) {
//        return forAbstractNodeOnly(that);
//    }
//
//    public RetType forTraitObjectAbsDeclOrDeclOnly(TraitObjectAbsDeclOrDecl that, List<RetType> mods_result, RetType name_result, List<RetType> staticParams_result, List<RetType> extendsClause_result, RetType where_result, List<RetType> decls_result) {
//        return forAbstractNodeOnly(that);
//    }
//
//    public RetType forTraitAbsDeclOrDeclOnly(TraitAbsDeclOrDecl that, List<RetType> mods_result, RetType name_result, List<RetType> staticParams_result, List<RetType> extendsClause_result, RetType where_result, List<RetType> excludes_result, Option<List<RetType>> comprises_result, List<RetType> decls_result) {
//        return forTraitObjectAbsDeclOrDeclOnly(that, mods_result, name_result, staticParams_result, extendsClause_result, where_result, decls_result);
//    }
//
//    public RetType forAbsTraitDeclOnly(AbsTraitDecl that, List<RetType> mods_result, RetType name_result, List<RetType> staticParams_result, List<RetType> extendsClause_result, RetType where_result, List<RetType> excludes_result, Option<List<RetType>> comprises_result, List<RetType> decls_result) {
//        return forTraitAbsDeclOrDeclOnly(that, mods_result, name_result, staticParams_result, extendsClause_result, where_result, excludes_result, comprises_result, decls_result);
//    }
//
//    public RetType forTraitDeclOnly(TraitDecl that, List<RetType> mods_result, RetType name_result, List<RetType> staticParams_result, List<RetType> extendsClause_result, RetType where_result, List<RetType> excludes_result, Option<List<RetType>> comprises_result, List<RetType> decls_result) {
//        return forTraitAbsDeclOrDeclOnly(that, mods_result, name_result, staticParams_result, extendsClause_result, where_result, excludes_result, comprises_result, decls_result);
//    }
//
//    public RetType forObjectAbsDeclOrDeclOnly(ObjectAbsDeclOrDecl that, List<RetType> mods_result, RetType name_result, List<RetType> staticParams_result, List<RetType> extendsClause_result, RetType where_result, Option<List<RetType>> params_result, Option<List<RetType>> throwsClause_result, RetType contract_result, List<RetType> decls_result) {
//        return forTraitObjectAbsDeclOrDeclOnly(that, mods_result, name_result, staticParams_result, extendsClause_result, where_result, decls_result);
//    }
//
//    public RetType forAbsObjectDeclOnly(AbsObjectDecl that, List<RetType> mods_result, RetType name_result, List<RetType> staticParams_result, List<RetType> extendsClause_result, RetType where_result, Option<List<RetType>> params_result, Option<List<RetType>> throwsClause_result, RetType contract_result, List<RetType> decls_result) {
//        return forObjectAbsDeclOrDeclOnly(that, mods_result, name_result, staticParams_result, extendsClause_result, where_result, params_result, throwsClause_result, contract_result, decls_result);
//    }
//
//    public RetType forObjectDeclOnly(ObjectDecl that, List<RetType> mods_result, RetType name_result, List<RetType> staticParams_result, List<RetType> extendsClause_result, RetType where_result, Option<List<RetType>> params_result, Option<List<RetType>> throwsClause_result, RetType contract_result, List<RetType> decls_result) {
//        return forObjectAbsDeclOrDeclOnly(that, mods_result, name_result, staticParams_result, extendsClause_result, where_result, params_result, throwsClause_result, contract_result, decls_result);
//    }
//
//    public RetType forVarAbsDeclOrDeclOnly(VarAbsDeclOrDecl that, List<RetType> lhs_result) {
//        return forAbstractNodeOnly(that);
//    }
//
//    public RetType forAbsVarDeclOnly(AbsVarDecl that, List<RetType> lhs_result) {
//        return forVarAbsDeclOrDeclOnly(that, lhs_result);
//    }
//
//    public RetType forVarDeclOnly(VarDecl that, List<RetType> lhs_result, RetType init_result) {
//        return forVarAbsDeclOrDeclOnly(that, lhs_result);
//    }
//
//    public RetType forLValueOnly(LValue that) {
//        return forAbstractNodeOnly(that);
//    }
//
//    public RetType forLValueBindOnly(LValueBind that, RetType name_result, Option<RetType> type_result, List<RetType> mods_result) {
//        return forLValueOnly(that);
//    }
//
//    public RetType forUnpastingOnly(Unpasting that) {
//        return forLValueOnly(that);
//    }
//
//    public RetType forUnpastingBindOnly(UnpastingBind that, RetType name_result, List<RetType> dim_result) {
//        return forUnpastingOnly(that);
//    }
//
//    public RetType forUnpastingSplitOnly(UnpastingSplit that, List<RetType> elems_result) {
//        return forUnpastingOnly(that);
//    }
//
//    public RetType forFnAbsDeclOrDeclOnly(FnAbsDeclOrDecl that, List<RetType> mods_result, RetType name_result, List<RetType> staticParams_result, List<RetType> params_result, Option<RetType> returnType_result, Option<List<RetType>> throwsClause_result, RetType where_result, RetType contract_result) {
//        return forAbstractNodeOnly(that);
//    }
//
//    public RetType forAbsFnDeclOnly(AbsFnDecl that, List<RetType> mods_result, RetType name_result, List<RetType> staticParams_result, List<RetType> params_result, Option<RetType> returnType_result, Option<List<RetType>> throwsClause_result, RetType where_result, RetType contract_result) {
//        return forFnAbsDeclOrDeclOnly(that, mods_result, name_result, staticParams_result, params_result, returnType_result, throwsClause_result, where_result, contract_result);
//    }
//
//    public RetType forFnDeclOnly(FnDecl that, List<RetType> mods_result, RetType name_result, List<RetType> staticParams_result, List<RetType> params_result, Option<RetType> returnType_result, Option<List<RetType>> throwsClause_result, RetType where_result, RetType contract_result) {
//        return forFnAbsDeclOrDeclOnly(that, mods_result, name_result, staticParams_result, params_result, returnType_result, throwsClause_result, where_result, contract_result);
//    }
//
//    public RetType forFnDefOnly(FnDef that, List<RetType> mods_result, RetType name_result, List<RetType> staticParams_result, List<RetType> params_result, Option<RetType> returnType_result, Option<List<RetType>> throwsClause_result, RetType where_result, RetType contract_result, RetType body_result) {
//        return forFnDeclOnly(that, mods_result, name_result, staticParams_result, params_result, returnType_result, throwsClause_result, where_result, contract_result);
//    }
//
//    public RetType forParamOnly(Param that, List<RetType> mods_result, RetType name_result) {
//        return forAbstractNodeOnly(that);
//    }
//
//    public RetType forNormalParamOnly(NormalParam that, List<RetType> mods_result, RetType name_result, Option<RetType> type_result, Option<RetType> defaultExpr_result) {
//        return forParamOnly(that, mods_result, name_result);
//    }
//
//    public RetType forVarargsParamOnly(VarargsParam that, List<RetType> mods_result, RetType name_result, RetType varargsType_result) {
//        return forParamOnly(that, mods_result, name_result);
//    }
//
//    public RetType forDimUnitDeclOnly(DimUnitDecl that, Option<RetType> dim_result, Option<RetType> derived_result, Option<RetType> default_result, List<RetType> units_result, Option<RetType> def_result) {
//        return forAbstractNodeOnly(that);
//    }
//
//    public RetType forTestDeclOnly(TestDecl that, RetType name_result, List<RetType> gens_result, RetType expr_result) {
//        return forAbstractNodeOnly(that);
//    }
//
//    public RetType forPropertyDeclOnly(PropertyDecl that, Option<RetType> name_result, List<RetType> params_result, RetType expr_result) {
//        return forAbstractNodeOnly(that);
//    }
//
//    public RetType forExternalSyntaxAbsDeclOrDeclOnly(ExternalSyntaxAbsDeclOrDecl that, RetType openExpander_result, RetType name_result, RetType closeExpander_result) {
//        return forAbstractNodeOnly(that);
//    }
//
//    public RetType forAbsExternalSyntaxOnly(AbsExternalSyntax that, RetType openExpander_result, RetType name_result, RetType closeExpander_result) {
//        return forExternalSyntaxAbsDeclOrDeclOnly(that, openExpander_result, name_result, closeExpander_result);
//    }
//
//    public RetType forExternalSyntaxOnly(ExternalSyntax that, RetType openExpander_result, RetType name_result, RetType closeExpander_result, RetType expr_result) {
//        return forExternalSyntaxAbsDeclOrDeclOnly(that, openExpander_result, name_result, closeExpander_result);
//    }
//
//    public RetType forGrammarDeclOnly(GrammarDecl that, RetType name_result, List<RetType> extends_result) {
//        return forAbstractNodeOnly(that);
//    }
//
//    public RetType forGrammarDefOnly(GrammarDef that, RetType name_result, List<RetType> extends_result, List<RetType> nonterminal_result) {
//        return forGrammarDeclOnly(that, name_result, extends_result);
//    }
//
//    public RetType forProductionDeclOnly(ProductionDecl that, Option<RetType> modifier_result, RetType name_result, RetType type_result, Option<RetType> extends_result) {
//        return forAbstractNodeOnly(that);
//    }
//
//    public RetType forProductionDefOnly(ProductionDef that, Option<RetType> modifier_result, RetType name_result, RetType type_result, Option<RetType> extends_result, List<RetType> syntaxDefs_result) {
//        return forProductionDeclOnly(that, modifier_result, name_result, type_result, extends_result);
//    }
//
//    public RetType forSyntaxDeclOnly(SyntaxDecl that) {
//        return forAbstractNodeOnly(that);
//    }
//
//    public RetType forSyntaxDefOnly(SyntaxDef that, List<RetType> syntaxSymbols_result, RetType transformationExpression_result) {
//        return forSyntaxDeclOnly(that);
//    }
//
//    public RetType forSyntaxSymbolOnly(SyntaxSymbol that) {
//        return forAbstractNodeOnly(that);
//    }
//
//    public RetType forPrefixedSymbolOnly(PrefixedSymbol that, Option<RetType> id_result, RetType symbol_result) {
//        return forSyntaxSymbolOnly(that);
//    }
//
//    public RetType forOptionalSymbolOnly(OptionalSymbol that, RetType symbol_result) {
//        return forSyntaxSymbolOnly(that);
//    }
//
//    public RetType forRepeatSymbolOnly(RepeatSymbol that, RetType symbol_result) {
//        return forSyntaxSymbolOnly(that);
//    }
//
//    public RetType forRepeatOneOrMoreSymbolOnly(RepeatOneOrMoreSymbol that, RetType symbol_result) {
//        return forSyntaxSymbolOnly(that);
//    }
//
//    public RetType forWhitespaceSymbolOnly(WhitespaceSymbol that, RetType symbol_result) {
//        return forSyntaxSymbolOnly(that);
//    }
//
//    public RetType forItemSymbolOnly(ItemSymbol that) {
//        return forSyntaxSymbolOnly(that);
//    }
//
//    public RetType forNonterminalSymbolOnly(NonterminalSymbol that, RetType nonterminal_result) {
//        return forSyntaxSymbolOnly(that);
//    }
//
//    public RetType forKeywordSymbolOnly(KeywordSymbol that) {
//        return forSyntaxSymbolOnly(that);
//    }
//
//    public RetType forTokenSymbolOnly(TokenSymbol that) {
//        return forSyntaxSymbolOnly(that);
//    }
//
//    public RetType forNotPredicateSymbolOnly(NotPredicateSymbol that, RetType symbol_result) {
//        return forSyntaxSymbolOnly(that);
//    }
//
//    public RetType forAndPredicateSymbolOnly(AndPredicateSymbol that, RetType symbol_result) {
//        return forSyntaxSymbolOnly(that);
//    }
//
//    public RetType forExprOnly(Expr that) {
//        return forAbstractNodeOnly(that);
//    }
//
//    public RetType forAsIfExprOnly(AsIfExpr that, RetType expr_result, RetType type_result) {
//        return forExprOnly(that);
//    }
//
//    public RetType forAssignmentOnly(Assignment that, List<RetType> lhs_result, Option<RetType> opr_result, RetType rhs_result) {
//        return forExprOnly(that);
//    }
//
//    public RetType forDelimitedExprOnly(DelimitedExpr that) {
//        return forExprOnly(that);
//    }
//
//    public RetType forCaseExprOnly(CaseExpr that, Option<RetType> param_result, Option<RetType> compare_result, List<RetType> clauses_result, Option<RetType> elseClause_result) {
//        return forDelimitedExprOnly(that);
//    }
//
//    public RetType forForOnly(For that, List<RetType> gens_result, RetType body_result) {
//        return forDelimitedExprOnly(that);
//    }
//
//    public RetType forLabelOnly(Label that, RetType name_result, RetType body_result) {
//        return forDelimitedExprOnly(that);
//    }
//
//    public RetType forAbstractObjectExprOnly(AbstractObjectExpr that, List<RetType> extendsClause_result, List<RetType> decls_result) {
//        return forDelimitedExprOnly(that);
//    }
//
//    public RetType forObjectExprOnly(ObjectExpr that, List<RetType> extendsClause_result, List<RetType> decls_result) {
//        return forAbstractObjectExprOnly(that, extendsClause_result, decls_result);
//    }
//
//    public RetType for_RewriteObjectExprOnly(_RewriteObjectExpr that, List<RetType> extendsClause_result, List<RetType> decls_result, List<RetType> staticParams_result, List<RetType> staticArgs_result, Option<List<RetType>> params_result) {
//        return forAbstractObjectExprOnly(that, extendsClause_result, decls_result);
//    }
//
//    public RetType forTryOnly(Try that, RetType body_result, Option<RetType> catchClause_result, List<RetType> forbid_result, Option<RetType> finallyClause_result) {
//        return forDelimitedExprOnly(that);
//    }
//
//    public RetType forTypecaseOnly(Typecase that, List<RetType> bind_result, List<RetType> clauses_result, Option<RetType> elseClause_result) {
//        return forDelimitedExprOnly(that);
//    }
//
//    public RetType forWhileOnly(While that, RetType test_result, RetType body_result) {
//        return forDelimitedExprOnly(that);
//    }
//
//    public RetType forFlowExprOnly(FlowExpr that) {
//        return forExprOnly(that);
//    }
//
//    public RetType forAccumulatorOnly(Accumulator that, RetType opr_result, List<RetType> gens_result, RetType body_result) {
//        return forFlowExprOnly(that);
//    }
//
//    public RetType forArrayComprehensionOnly(ArrayComprehension that, List<RetType> clauses_result) {
//        return forFlowExprOnly(that);
//    }
//
//    public RetType forAtomicExprOnly(AtomicExpr that, RetType expr_result) {
//        return forFlowExprOnly(that);
//    }
//
//    public RetType forExitOnly(Exit that, Option<RetType> target_result, Option<RetType> returnExpr_result) {
//        return forFlowExprOnly(that);
//    }
//
//    public RetType forSpawnOnly(Spawn that, RetType body_result) {
//        return forFlowExprOnly(that);
//    }
//
//    public RetType forThrowOnly(Throw that, RetType expr_result) {
//        return forFlowExprOnly(that);
//    }
//
//    public RetType forTryAtomicExprOnly(TryAtomicExpr that, RetType expr_result) {
//        return forFlowExprOnly(that);
//    }
//
//    public RetType forFnExprOnly(FnExpr that, RetType name_result, List<RetType> staticParams_result, List<RetType> params_result, Option<RetType> returnType_result, RetType where_result, Option<List<RetType>> throwsClause_result, RetType body_result) {
//        return forExprOnly(that);
//    }
//
//    public RetType forLetExprOnly(LetExpr that, List<RetType> body_result) {
//        return forExprOnly(that);
//    }
//
//    public RetType forLocalVarDeclOnly(LocalVarDecl that, List<RetType> body_result, List<RetType> lhs_result, Option<RetType> rhs_result) {
//        return forLetExprOnly(that, body_result);
//    }
//
//    public RetType forGeneratedExprOnly(GeneratedExpr that, RetType expr_result, List<RetType> gens_result) {
//        return forExprOnly(that);
//    }
//
//    public RetType forOpExprOnly(OpExpr that) {
//        return forExprOnly(that);
//    }
//
//    public RetType forOprExprOnly(OprExpr that, List<RetType> ops_result, List<RetType> args_result) {
//        return forOpExprOnly(that);
//    }
//
//    public RetType forSubscriptExprOnly(SubscriptExpr that, RetType obj_result, List<RetType> subs_result, Option<RetType> op_result) {
//        return forOpExprOnly(that);
//    }
//
//    public RetType forPrimaryOnly(Primary that) {
//        return forOpExprOnly(that);
//    }
//
//    public RetType forCoercionInvocationOnly(CoercionInvocation that, RetType type_result, List<RetType> staticArgs_result, RetType arg_result) {
//        return forPrimaryOnly(that);
//    }
//
//    public RetType forMethodInvocationOnly(MethodInvocation that, RetType obj_result, RetType method_result, List<RetType> staticArgs_result, RetType arg_result) {
//        return forPrimaryOnly(that);
//    }
//
//    public RetType forAbstractFieldRefOnly(AbstractFieldRef that, RetType obj_result) {
//        return forPrimaryOnly(that);
//    }
//
//    public RetType forFieldRefOnly(FieldRef that, RetType obj_result, RetType field_result) {
//        return forAbstractFieldRefOnly(that, obj_result);
//    }
//
//    public RetType forFieldRefForSureOnly(FieldRefForSure that, RetType obj_result, RetType field_result) {
//        return forAbstractFieldRefOnly(that, obj_result);
//    }
//
//    public RetType for_RewriteFieldRefOnly(_RewriteFieldRef that, RetType obj_result, RetType field_result) {
//        return forAbstractFieldRefOnly(that, obj_result);
//    }
//
//    public RetType forJuxtOnly(Juxt that, List<RetType> exprs_result) {
//        return forPrimaryOnly(that);
//    }
//
//    public RetType forLooseJuxtOnly(LooseJuxt that, List<RetType> exprs_result) {
//        return forJuxtOnly(that, exprs_result);
//    }
//
//    public RetType forMathPrimaryOnly(MathPrimary that, RetType front_result, List<RetType> rest_result) {
//        return forPrimaryOnly(that);
//    }
//
//    public RetType for_RewriteFnRefOnly(_RewriteFnRef that, RetType fn_result, List<RetType> staticArgs_result) {
//        return forPrimaryOnly(that);
//    }
//
//    public RetType forBaseExprOnly(BaseExpr that) {
//        return forPrimaryOnly(that);
//    }
//
//    public RetType forVarRefOnly(VarRef that, RetType var_result) {
//        return forBaseExprOnly(that);
//    }
//
//    public RetType forArrayExprOnly(ArrayExpr that) {
//        return forBaseExprOnly(that);
//    }
//
//    public RetType forArrayElementOnly(ArrayElement that, RetType element_result) {
//        return forArrayExprOnly(that);
//    }
//
//    public RetType forArrayElementsOnly(ArrayElements that, List<RetType> elements_result) {
//        return forArrayExprOnly(that);
//    }
//
//    public RetType forTypeOnly(Type that) {
//        return forAbstractNodeOnly(that);
//    }
//
//    public RetType forArrowTypeOnly(ArrowType that, RetType domain_result, RetType range_result, Option<List<RetType>> throwsClause_result) {
//        return forTypeOnly(that);
//    }
//
//    public RetType for_RewriteGenericArrowTypeOnly(_RewriteGenericArrowType that, RetType domain_result, RetType range_result, Option<List<RetType>> throwsClause_result, List<RetType> staticParams_result, RetType where_result) {
//        return forArrowTypeOnly(that, domain_result, range_result, throwsClause_result);
//    }
//
//    public RetType forNonArrowTypeOnly(NonArrowType that) {
//        return forTypeOnly(that);
//    }
//
//    public RetType forBottomTypeOnly(BottomType that) {
//        return forNonArrowTypeOnly(that);
//    }
//
//    public RetType forTraitTypeOnly(TraitType that) {
//        return forNonArrowTypeOnly(that);
//    }
//
//    public RetType forArrayTypeOnly(ArrayType that, RetType element_result, RetType indices_result) {
//        return forTraitTypeOnly(that);
//    }
//
//    public RetType forIdTypeOnly(IdType that, RetType name_result) {
//        return forTraitTypeOnly(that);
//    }
//
//    public RetType forInferenceVarTypeOnly(InferenceVarType that) {
//        return forTraitTypeOnly(that);
//    }
//
//    public RetType forMatrixTypeOnly(MatrixType that, RetType element_result, List<RetType> dimensions_result) {
//        return forTraitTypeOnly(that);
//    }
//
//    public RetType forInstantiatedTypeOnly(InstantiatedType that, RetType name_result, List<RetType> args_result) {
//        return forTraitTypeOnly(that);
//    }
//
//    public RetType forVoidTypeOnly(VoidType that) {
//        return forNonArrowTypeOnly(that);
//    }
//
//    public RetType forIntersectionTypeOnly(IntersectionType that, Set<RetType> elements_result) {
//        return forNonArrowTypeOnly(that);
//    }
//
//    public RetType forUnionTypeOnly(UnionType that, Set<RetType> elements_result) {
//        return forNonArrowTypeOnly(that);
//    }
//
//    public RetType forAndTypeOnly(AndType that, RetType first_result, RetType second_result) {
//        return forNonArrowTypeOnly(that);
//    }
//
//    public RetType forOrTypeOnly(OrType that, RetType first_result, RetType second_result) {
//        return forNonArrowTypeOnly(that);
//    }
//
//    public RetType forDimTypeOnly(DimType that, RetType type_result) {
//        return forNonArrowTypeOnly(that);
//    }
//
//    public RetType forTaggedDimTypeOnly(TaggedDimType that, RetType type_result, RetType dim_result, Option<RetType> unit_result) {
//        return forDimTypeOnly(that, type_result);
//    }
//
//    public RetType forTaggedUnitTypeOnly(TaggedUnitType that, RetType type_result, RetType unit_result) {
//        return forDimTypeOnly(that, type_result);
//    }
//
//    public RetType forStaticArgOnly(StaticArg that) {
//        return forTypeOnly(that);
//    }
//
//    public RetType forIdArgOnly(IdArg that, RetType name_result) {
//        return forStaticArgOnly(that);
//    }
//
//    public RetType forTypeArgOnly(TypeArg that, RetType type_result) {
//        return forStaticArgOnly(that);
//    }
//
//    public RetType forIntArgOnly(IntArg that, RetType val_result) {
//        return forStaticArgOnly(that);
//    }
//
//    public RetType forBoolArgOnly(BoolArg that, RetType bool_result) {
//        return forStaticArgOnly(that);
//    }
//
//    public RetType forOprArgOnly(OprArg that, RetType name_result) {
//        return forStaticArgOnly(that);
//    }
//
//    public RetType forDimArgOnly(DimArg that, RetType dim_result) {
//        return forStaticArgOnly(that);
//    }
//
//    public RetType forUnitArgOnly(UnitArg that, RetType unit_result) {
//        return forStaticArgOnly(that);
//    }
//
//    public RetType for_RewriteImplicitTypeOnly(_RewriteImplicitType that) {
//        return forTypeOnly(that);
//    }
//
//    public RetType for_RewriteIntersectionTypeOnly(_RewriteIntersectionType that, List<RetType> elements_result) {
//        return forTypeOnly(that);
//    }
//
//    public RetType for_RewriteUnionTypeOnly(_RewriteUnionType that, List<RetType> elements_result) {
//        return forTypeOnly(that);
//    }
//
//    public RetType for_RewriteFixedPointTypeOnly(_RewriteFixedPointType that, RetType var_result, RetType body_result) {
//        return forTypeOnly(that);
//    }
//
//    public RetType forStaticExprOnly(StaticExpr that) {
//        return forAbstractNodeOnly(that);
//    }
//
//    public RetType forIntExprOnly(IntExpr that) {
//        return forStaticExprOnly(that);
//    }
//
//    public RetType forIntValOnly(IntVal that) {
//        return forIntExprOnly(that);
//    }
//
//    public RetType forNumberConstraintOnly(NumberConstraint that, RetType val_result) {
//        return forIntValOnly(that);
//    }
//
//    public RetType forIntRefOnly(IntRef that, RetType name_result) {
//        return forIntValOnly(that);
//    }
//
//    public RetType forIntOpExprOnly(IntOpExpr that, RetType left_result, RetType right_result) {
//        return forIntExprOnly(that);
//    }
//
//    public RetType forSumConstraintOnly(SumConstraint that, RetType left_result, RetType right_result) {
//        return forIntOpExprOnly(that, left_result, right_result);
//    }
//
//    public RetType forMinusConstraintOnly(MinusConstraint that, RetType left_result, RetType right_result) {
//        return forIntOpExprOnly(that, left_result, right_result);
//    }
//
//    public RetType forProductConstraintOnly(ProductConstraint that, RetType left_result, RetType right_result) {
//        return forIntOpExprOnly(that, left_result, right_result);
//    }
//
//    public RetType forExponentConstraintOnly(ExponentConstraint that, RetType left_result, RetType right_result) {
//        return forIntOpExprOnly(that, left_result, right_result);
//    }
//
//    public RetType forBoolExprOnly(BoolExpr that) {
//        return forStaticExprOnly(that);
//    }
//
//    public RetType forBoolValOnly(BoolVal that) {
//        return forBoolExprOnly(that);
//    }
//
//    public RetType forBoolConstantOnly(BoolConstant that) {
//        return forBoolValOnly(that);
//    }
//
//    public RetType forBoolRefOnly(BoolRef that, RetType name_result) {
//        return forBoolValOnly(that);
//    }
//
//    public RetType forBoolConstraintOnly(BoolConstraint that) {
//        return forBoolExprOnly(that);
//    }
//
//    public RetType forNotConstraintOnly(NotConstraint that, RetType bool_result) {
//        return forBoolConstraintOnly(that);
//    }
//
//    public RetType forBinaryBoolConstraintOnly(BinaryBoolConstraint that, RetType left_result, RetType right_result) {
//        return forBoolConstraintOnly(that);
//    }
//
//    public RetType forOrConstraintOnly(OrConstraint that, RetType left_result, RetType right_result) {
//        return forBinaryBoolConstraintOnly(that, left_result, right_result);
//    }
//
//    public RetType forAndConstraintOnly(AndConstraint that, RetType left_result, RetType right_result) {
//        return forBinaryBoolConstraintOnly(that, left_result, right_result);
//    }
//
//    public RetType forImpliesConstraintOnly(ImpliesConstraint that, RetType left_result, RetType right_result) {
//        return forBinaryBoolConstraintOnly(that, left_result, right_result);
//    }
//
//    public RetType forBEConstraintOnly(BEConstraint that, RetType left_result, RetType right_result) {
//        return forBinaryBoolConstraintOnly(that, left_result, right_result);
//    }
//
//    public RetType forDimExprOnly(DimExpr that) {
//        return forStaticExprOnly(that);
//    }
//
//    public RetType forBaseDimOnly(BaseDim that) {
//        return forDimExprOnly(that);
//    }
//
//    public RetType forDimRefOnly(DimRef that, RetType name_result) {
//        return forDimExprOnly(that);
//    }
//
//    public RetType forProductDimOnly(ProductDim that, RetType multiplier_result, RetType multiplicand_result) {
//        return forDimExprOnly(that);
//    }
//
//    public RetType forQuotientDimOnly(QuotientDim that, RetType numerator_result, RetType denominator_result) {
//        return forDimExprOnly(that);
//    }
//
//    public RetType forExponentDimOnly(ExponentDim that, RetType base_result, RetType power_result) {
//        return forDimExprOnly(that);
//    }
//
//    public RetType forOpDimOnly(OpDim that, RetType val_result, RetType op_result) {
//        return forDimExprOnly(that);
//    }
//
//    public RetType forWhereClauseOnly(WhereClause that, List<RetType> bindings_result, List<RetType> constraints_result) {
//        return forAbstractNodeOnly(that);
//    }
//
//    public RetType forWhereBindingOnly(WhereBinding that) {
//        return forAbstractNodeOnly(that);
//    }
//
//    public RetType forWhereTypeOnly(WhereType that, RetType name_result, List<RetType> supers_result) {
//        return forWhereBindingOnly(that);
//    }
//
//    public RetType forWhereNatOnly(WhereNat that, RetType name_result) {
//        return forWhereBindingOnly(that);
//    }
//
//    public RetType forWhereIntOnly(WhereInt that, RetType name_result) {
//        return forWhereBindingOnly(that);
//    }
//
//    public RetType forWhereBoolOnly(WhereBool that, RetType name_result) {
//        return forWhereBindingOnly(that);
//    }
//
//    public RetType forWhereUnitOnly(WhereUnit that, RetType name_result) {
//        return forWhereBindingOnly(that);
//    }
//
//    public RetType forWhereConstraintOnly(WhereConstraint that) {
//        return forAbstractNodeOnly(that);
//    }
//
//    public RetType forWhereExtendsOnly(WhereExtends that, RetType name_result, List<RetType> supers_result) {
//        return forWhereConstraintOnly(that);
//    }
//
//    public RetType forTypeAliasOnly(TypeAlias that, RetType name_result, List<RetType> staticParams_result, RetType type_result) {
//        return forWhereConstraintOnly(that);
//    }
//
//    public RetType forWhereCoercesOnly(WhereCoerces that, RetType left_result, RetType right_result) {
//        return forWhereConstraintOnly(that);
//    }
//
//    public RetType forWhereWidensOnly(WhereWidens that, RetType left_result, RetType right_result) {
//        return forWhereConstraintOnly(that);
//    }
//
//    public RetType forWhereWidensCoercesOnly(WhereWidensCoerces that, RetType left_result, RetType right_result) {
//        return forWhereConstraintOnly(that);
//    }
//
//    public RetType forWhereEqualsOnly(WhereEquals that, RetType left_result, RetType right_result) {
//        return forWhereConstraintOnly(that);
//    }
//
//    public RetType forUnitConstraintOnly(UnitConstraint that, RetType name_result) {
//        return forWhereConstraintOnly(that);
//    }
//
//    public RetType forIntConstraintOnly(IntConstraint that, RetType left_result, RetType right_result) {
//        return forWhereConstraintOnly(that);
//    }
//
//    public RetType forLEConstraintOnly(LEConstraint that, RetType left_result, RetType right_result) {
//        return forIntConstraintOnly(that, left_result, right_result);
//    }
//
//    public RetType forLTConstraintOnly(LTConstraint that, RetType left_result, RetType right_result) {
//        return forIntConstraintOnly(that, left_result, right_result);
//    }
//
//    public RetType forGEConstraintOnly(GEConstraint that, RetType left_result, RetType right_result) {
//        return forIntConstraintOnly(that, left_result, right_result);
//    }
//
//    public RetType forGTConstraintOnly(GTConstraint that, RetType left_result, RetType right_result) {
//        return forIntConstraintOnly(that, left_result, right_result);
//    }
//
//    public RetType forIEConstraintOnly(IEConstraint that, RetType left_result, RetType right_result) {
//        return forIntConstraintOnly(that, left_result, right_result);
//    }
//
//    public RetType forBoolConstraintExprOnly(BoolConstraintExpr that, RetType constraint_result) {
//        return forWhereConstraintOnly(that);
//    }
//
//    public RetType forContractOnly(Contract that, Option<List<RetType>> requires_result, Option<List<RetType>> ensures_result, Option<List<RetType>> invariants_result) {
//        return forAbstractNodeOnly(that);
//    }
//
//    public RetType forEnsuresClauseOnly(EnsuresClause that, RetType post_result, Option<RetType> pre_result) {
//        return forAbstractNodeOnly(that);
//    }
//
//    public RetType forModifierOnly(Modifier that) {
//        return forAbstractNodeOnly(that);
//    }
//
//    public RetType forModifierAbstractOnly(ModifierAbstract that) {
//        return forModifierOnly(that);
//    }
//
//    public RetType forModifierAtomicOnly(ModifierAtomic that) {
//        return forModifierOnly(that);
//    }
//
//    public RetType forModifierGetterOnly(ModifierGetter that) {
//        return forModifierOnly(that);
//    }
//
//    public RetType forModifierHiddenOnly(ModifierHidden that) {
//        return forModifierOnly(that);
//    }
//
//    public RetType forModifierIOOnly(ModifierIO that) {
//        return forModifierOnly(that);
//    }
//
//    public RetType forModifierOverrideOnly(ModifierOverride that) {
//        return forModifierOnly(that);
//    }
//
//    public RetType forModifierPrivateOnly(ModifierPrivate that) {
//        return forModifierOnly(that);
//    }
//
//    public RetType forModifierSettableOnly(ModifierSettable that) {
//        return forModifierOnly(that);
//    }
//
//    public RetType forModifierSetterOnly(ModifierSetter that) {
//        return forModifierOnly(that);
//    }
//
//    public RetType forModifierTestOnly(ModifierTest that) {
//        return forModifierOnly(that);
//    }
//
//    public RetType forModifierTransientOnly(ModifierTransient that) {
//        return forModifierOnly(that);
//    }
//
//    public RetType forModifierValueOnly(ModifierValue that) {
//        return forModifierOnly(that);
//    }
//
//    public RetType forModifierVarOnly(ModifierVar that) {
//        return forModifierOnly(that);
//    }
//
//    public RetType forModifierWidensOnly(ModifierWidens that) {
//        return forModifierOnly(that);
//    }
//
//    public RetType forModifierWrappedOnly(ModifierWrapped that) {
//        return forModifierOnly(that);
//    }
//
//    public RetType forStaticParamOnly(StaticParam that) {
//        return forAbstractNodeOnly(that);
//    }
//
//    public RetType forOperatorParamOnly(OperatorParam that, RetType name_result) {
//        return forStaticParamOnly(that);
//    }
//
//    public RetType forIdStaticParamOnly(IdStaticParam that, RetType name_result) {
//        return forStaticParamOnly(that);
//    }
//
//    public RetType forBoolParamOnly(BoolParam that, RetType name_result) {
//        return forIdStaticParamOnly(that, name_result);
//    }
//
//    public RetType forDimensionParamOnly(DimensionParam that, RetType name_result) {
//        return forIdStaticParamOnly(that, name_result);
//    }
//
//    public RetType forIntParamOnly(IntParam that, RetType name_result) {
//        return forIdStaticParamOnly(that, name_result);
//    }
//
//    public RetType forNatParamOnly(NatParam that, RetType name_result) {
//        return forIdStaticParamOnly(that, name_result);
//    }
//
//    public RetType forSimpleTypeParamOnly(SimpleTypeParam that, RetType name_result, List<RetType> extendsClause_result) {
//        return forIdStaticParamOnly(that, name_result);
//    }
//
//    public RetType forUnitParamOnly(UnitParam that, RetType name_result, Option<RetType> dim_result) {
//        return forIdStaticParamOnly(that, name_result);
//    }
//
//    public RetType forNameOnly(Name that) {
//        return forAbstractNodeOnly(that);
//    }
//
//    public RetType forAPINameOnly(APIName that, List<RetType> ids_result) {
//        return forNameOnly(that);
//    }
//
//    public RetType forQualifiedNameOnly(QualifiedName that, Option<RetType> api_result, RetType name_result) {
//        return forNameOnly(that);
//    }
//
//    public RetType forQualifiedIdNameOnly(QualifiedIdName that, Option<RetType> api_result, RetType name_result) {
//        return forQualifiedNameOnly(that, api_result, name_result);
//    }
//
//    public RetType forQualifiedOpNameOnly(QualifiedOpName that, Option<RetType> api_result, RetType name_result) {
//        return forQualifiedNameOnly(that, api_result, name_result);
//    }
//
//    public RetType forSimpleNameOnly(SimpleName that) {
//        return forNameOnly(that);
//    }
//
//    public RetType forIdOnly(Id that) {
//        return forSimpleNameOnly(that);
//    }
//
//    public RetType forOpNameOnly(OpName that) {
//        return forSimpleNameOnly(that);
//    }
//
//    public RetType forOpOnly(Op that) {
//        return forOpNameOnly(that);
//    }
//
//    public RetType forEnclosingOnly(Enclosing that, RetType open_result, RetType close_result) {
//        return forOpNameOnly(that);
//    }
//
//    public RetType forAnonymousFnNameOnly(AnonymousFnName that) {
//        return forSimpleNameOnly(that);
//    }
//
//    public RetType forConstructorFnNameOnly(ConstructorFnName that, RetType def_result) {
//        return forSimpleNameOnly(that);
//    }
//
//    public RetType forArrayComprehensionClauseOnly(ArrayComprehensionClause that, List<RetType> bind_result, RetType init_result, List<RetType> gens_result) {
//        return forAbstractNodeOnly(that);
//    }
//
//    public RetType forKeywordExprOnly(KeywordExpr that, RetType name_result, RetType init_result) {
//        return forAbstractNodeOnly(that);
//    }
//
//    public RetType forCaseClauseOnly(CaseClause that, RetType match_result, RetType body_result) {
//        return forAbstractNodeOnly(that);
//    }
//
//    public RetType forCatchOnly(Catch that, RetType name_result, List<RetType> clauses_result) {
//        return forAbstractNodeOnly(that);
//    }
//
//    public RetType forCatchClauseOnly(CatchClause that, RetType match_result, RetType body_result) {
//        return forAbstractNodeOnly(that);
//    }
//
//    public RetType forTypecaseClauseOnly(TypecaseClause that, List<RetType> match_result, RetType body_result) {
//        return forAbstractNodeOnly(that);
//    }
//
//    public RetType forExtentRangeOnly(ExtentRange that, Option<RetType> base_result, Option<RetType> size_result) {
//        return forAbstractNodeOnly(that);
//    }
//
//    public RetType forGeneratorClauseOnly(GeneratorClause that, List<RetType> bind_result, RetType init_result) {
//        return forAbstractNodeOnly(that);
//    }
//
//    public RetType forVarargsExprOnly(VarargsExpr that, RetType varargs_result) {
//        return forAbstractNodeOnly(that);
//    }
//
//    public RetType forVarargsTypeOnly(VarargsType that, RetType type_result) {
//        return forAbstractNodeOnly(that);
//    }
//
//    public RetType forKeywordTypeOnly(KeywordType that, RetType name_result, RetType type_result) {
//        return forAbstractNodeOnly(that);
//    }
//
//    public RetType forTraitTypeWhereOnly(TraitTypeWhere that, RetType type_result, RetType where_result) {
//        return forAbstractNodeOnly(that);
//    }
//
//    public RetType forIndicesOnly(Indices that, List<RetType> extents_result) {
//        return forAbstractNodeOnly(that);
//    }
//
//    public RetType forDimUnitOpOnly(DimUnitOp that) {
//        return forAbstractNodeOnly(that);
//    }
//
//    public RetType forSquareDimUnitOnly(SquareDimUnit that) {
//        return forDimUnitOpOnly(that);
//    }
//
//    public RetType forCubicDimUnitOnly(CubicDimUnit that) {
//        return forDimUnitOpOnly(that);
//    }
//
//    public RetType forInverseDimUnitOnly(InverseDimUnit that) {
//        return forDimUnitOpOnly(that);
//    }
//
//    public RetType forMathItemOnly(MathItem that) {
//        return forAbstractNodeOnly(that);
//    }
//
//    public RetType forExprMIOnly(ExprMI that, RetType expr_result) {
//        return forMathItemOnly(that);
//    }
//
//    public RetType forParenthesisDelimitedMIOnly(ParenthesisDelimitedMI that, RetType expr_result) {
//        return forExprMIOnly(that, expr_result);
//    }
//
//    public RetType forNonParenthesisDelimitedMIOnly(NonParenthesisDelimitedMI that, RetType expr_result) {
//        return forExprMIOnly(that, expr_result);
//    }
//
//    public RetType forNonExprMIOnly(NonExprMI that) {
//        return forMathItemOnly(that);
//    }
//
//    public RetType forExponentiationMIOnly(ExponentiationMI that, RetType op_result, Option<RetType> expr_result) {
//        return forNonExprMIOnly(that);
//    }
//
//    public RetType forSubscriptingMIOnly(SubscriptingMI that, RetType op_result, List<RetType> exprs_result) {
//        return forNonExprMIOnly(that);
//    }
//
//
//    /** Methods to recur on each child. */
//    public RetType forComponent(Component that) {
//        RetType name_result = that.getName().accept(this);
//        List<RetType> imports_result = recurOnListOfImport(that.getImports());
//        List<RetType> exports_result = recurOnListOfExport(that.getExports());
//        List<RetType> decls_result = recurOnListOfDecl(that.getDecls());
//        return forComponentOnly(that, name_result, imports_result, exports_result, decls_result);
//    }
//
//    public RetType forApi(Api that) {
//        RetType name_result = that.getName().accept(this);
//        List<RetType> imports_result = recurOnListOfImport(that.getImports());
//        List<RetType> decls_result = recurOnListOfAbsDecl(that.getDecls());
//        return forApiOnly(that, name_result, imports_result, decls_result);
//    }
//
//    public RetType forImportStar(ImportStar that) {
//        RetType api_result = that.getApi().accept(this);
//        List<RetType> except_result = recurOnListOfSimpleName(that.getExcept());
//        return forImportStarOnly(that, api_result, except_result);
//    }
//
//    public RetType forImportNames(ImportNames that) {
//        RetType api_result = that.getApi().accept(this);
//        List<RetType> aliasedNames_result = recurOnListOfAliasedSimpleName(that.getAliasedNames());
//        return forImportNamesOnly(that, api_result, aliasedNames_result);
//    }
//
//    public RetType forImportApi(ImportApi that) {
//        List<RetType> apis_result = recurOnListOfAliasedAPIName(that.getApis());
//        return forImportApiOnly(that, apis_result);
//    }
//
//    public RetType forAliasedSimpleName(AliasedSimpleName that) {
//        RetType name_result = that.getName().accept(this);
//        Option<RetType> alias_result = recurOnOptionOfSimpleName(that.getAlias());
//        return forAliasedSimpleNameOnly(that, name_result, alias_result);
//    }
//
//    public RetType forAliasedAPIName(AliasedAPIName that) {
//        RetType api_result = that.getApi().accept(this);
//        Option<RetType> alias_result = recurOnOptionOfId(that.getAlias());
//        return forAliasedAPINameOnly(that, api_result, alias_result);
//    }
//
//    public RetType forExport(Export that) {
//        List<RetType> apis_result = recurOnListOfAPIName(that.getApis());
//        return forExportOnly(that, apis_result);
//    }
//
//    public RetType forAbsTraitDecl(AbsTraitDecl that) {
//        List<RetType> mods_result = recurOnListOfModifier(that.getMods());
//        RetType name_result = that.getName().accept(this);
//        List<RetType> staticParams_result = recurOnListOfStaticParam(that.getStaticParams());
//        List<RetType> extendsClause_result = recurOnListOfTraitTypeWhere(that.getExtendsClause());
//        RetType where_result = that.getWhere().accept(this);
//        List<RetType> excludes_result = recurOnListOfTraitType(that.getExcludes());
//        Option<List<RetType>> comprises_result = recurOnOptionOfListOfTraitType(that.getComprises());
//        List<RetType> decls_result = recurOnListOfAbsDecl(that.getDecls());
//        return forAbsTraitDeclOnly(that, mods_result, name_result, staticParams_result, extendsClause_result, where_result, excludes_result, comprises_result, decls_result);
//    }
//
//    public RetType forTraitDecl(TraitDecl that) {
//        List<RetType> mods_result = recurOnListOfModifier(that.getMods());
//        RetType name_result = that.getName().accept(this);
//        List<RetType> staticParams_result = recurOnListOfStaticParam(that.getStaticParams());
//        List<RetType> extendsClause_result = recurOnListOfTraitTypeWhere(that.getExtendsClause());
//        RetType where_result = that.getWhere().accept(this);
//        List<RetType> excludes_result = recurOnListOfTraitType(that.getExcludes());
//        Option<List<RetType>> comprises_result = recurOnOptionOfListOfTraitType(that.getComprises());
//        List<RetType> decls_result = recurOnListOfDecl(that.getDecls());
//        return forTraitDeclOnly(that, mods_result, name_result, staticParams_result, extendsClause_result, where_result, excludes_result, comprises_result, decls_result);
//    }
//
//    public RetType forAbsObjectDecl(AbsObjectDecl that) {
//        List<RetType> mods_result = recurOnListOfModifier(that.getMods());
//        RetType name_result = that.getName().accept(this);
//        List<RetType> staticParams_result = recurOnListOfStaticParam(that.getStaticParams());
//        List<RetType> extendsClause_result = recurOnListOfTraitTypeWhere(that.getExtendsClause());
//        RetType where_result = that.getWhere().accept(this);
//        Option<List<RetType>> params_result = recurOnOptionOfListOfParam(that.getParams());
//        Option<List<RetType>> throwsClause_result = recurOnOptionOfListOfTraitType(that.getThrowsClause());
//        RetType contract_result = that.getContract().accept(this);
//        List<RetType> decls_result = recurOnListOfAbsDecl(that.getDecls());
//        return forAbsObjectDeclOnly(that, mods_result, name_result, staticParams_result, extendsClause_result, where_result, params_result, throwsClause_result, contract_result, decls_result);
//    }
//
//    public RetType forObjectDecl(ObjectDecl that) {
//        List<RetType> mods_result = recurOnListOfModifier(that.getMods());
//        RetType name_result = that.getName().accept(this);
//        List<RetType> staticParams_result = recurOnListOfStaticParam(that.getStaticParams());
//        List<RetType> extendsClause_result = recurOnListOfTraitTypeWhere(that.getExtendsClause());
//        RetType where_result = that.getWhere().accept(this);
//        Option<List<RetType>> params_result = recurOnOptionOfListOfParam(that.getParams());
//        Option<List<RetType>> throwsClause_result = recurOnOptionOfListOfTraitType(that.getThrowsClause());
//        RetType contract_result = that.getContract().accept(this);
//        List<RetType> decls_result = recurOnListOfDecl(that.getDecls());
//        return forObjectDeclOnly(that, mods_result, name_result, staticParams_result, extendsClause_result, where_result, params_result, throwsClause_result, contract_result, decls_result);
//    }
//
//    public RetType forAbsVarDecl(AbsVarDecl that) {
//        List<RetType> lhs_result = recurOnListOfLValueBind(that.getLhs());
//        return forAbsVarDeclOnly(that, lhs_result);
//    }
//
//    public RetType forVarDecl(VarDecl that) {
//        List<RetType> lhs_result = recurOnListOfLValueBind(that.getLhs());
//        RetType init_result = that.getInit().accept(this);
//        return forVarDeclOnly(that, lhs_result, init_result);
//    }
//
//    public RetType forLValueBind(LValueBind that) {
//        RetType name_result = that.getName().accept(this);
//        Option<RetType> type_result = recurOnOptionOfType(that.getType());
//        List<RetType> mods_result = recurOnListOfModifier(that.getMods());
//        return forLValueBindOnly(that, name_result, type_result, mods_result);
//    }
//
//    public RetType forUnpastingBind(UnpastingBind that) {
//        RetType name_result = that.getName().accept(this);
//        List<RetType> dim_result = recurOnListOfExtentRange(that.getDim());
//        return forUnpastingBindOnly(that, name_result, dim_result);
//    }
//
//    public RetType forUnpastingSplit(UnpastingSplit that) {
//        List<RetType> elems_result = recurOnListOfUnpasting(that.getElems());
//        return forUnpastingSplitOnly(that, elems_result);
//    }
//
//    public RetType forAbsFnDecl(AbsFnDecl that) {
//        List<RetType> mods_result = recurOnListOfModifier(that.getMods());
//        RetType name_result = that.getName().accept(this);
//        List<RetType> staticParams_result = recurOnListOfStaticParam(that.getStaticParams());
//        List<RetType> params_result = recurOnListOfParam(that.getParams());
//        Option<RetType> returnType_result = recurOnOptionOfType(that.getReturnType());
//        Option<List<RetType>> throwsClause_result = recurOnOptionOfListOfTraitType(that.getThrowsClause());
//        RetType where_result = that.getWhere().accept(this);
//        RetType contract_result = that.getContract().accept(this);
//        return forAbsFnDeclOnly(that, mods_result, name_result, staticParams_result, params_result, returnType_result, throwsClause_result, where_result, contract_result);
//    }
//
//    public RetType forFnDef(FnDef that) {
//        List<RetType> mods_result = recurOnListOfModifier(that.getMods());
//        RetType name_result = that.getName().accept(this);
//        List<RetType> staticParams_result = recurOnListOfStaticParam(that.getStaticParams());
//        List<RetType> params_result = recurOnListOfParam(that.getParams());
//        Option<RetType> returnType_result = recurOnOptionOfType(that.getReturnType());
//        Option<List<RetType>> throwsClause_result = recurOnOptionOfListOfTraitType(that.getThrowsClause());
//        RetType where_result = that.getWhere().accept(this);
//        RetType contract_result = that.getContract().accept(this);
//        RetType body_result = that.getBody().accept(this);
//        return forFnDefOnly(that, mods_result, name_result, staticParams_result, params_result, returnType_result, throwsClause_result, where_result, contract_result, body_result);
//    }
//
//    public RetType forNormalParam(NormalParam that) {
//        List<RetType> mods_result = recurOnListOfModifier(that.getMods());
//        RetType name_result = that.getName().accept(this);
//        Option<RetType> type_result = recurOnOptionOfType(that.getType());
//        Option<RetType> defaultExpr_result = recurOnOptionOfExpr(that.getDefaultExpr());
//        return forNormalParamOnly(that, mods_result, name_result, type_result, defaultExpr_result);
//    }
//
//    public RetType forVarargsParam(VarargsParam that) {
//        List<RetType> mods_result = recurOnListOfModifier(that.getMods());
//        RetType name_result = that.getName().accept(this);
//        RetType varargsType_result = that.getVarargsType().accept(this);
//        return forVarargsParamOnly(that, mods_result, name_result, varargsType_result);
//    }
//
//    public RetType forDimUnitDecl(DimUnitDecl that) {
//        Option<RetType> dim_result = recurOnOptionOfId(that.getDim());
//        Option<RetType> derived_result = recurOnOptionOfDimExpr(that.getDerived());
//        Option<RetType> default_result = recurOnOptionOfId(that.getDefault());
//        List<RetType> units_result = recurOnListOfId(that.getUnits());
//        Option<RetType> def_result = recurOnOptionOfExpr(that.getDef());
//        return forDimUnitDeclOnly(that, dim_result, derived_result, default_result, units_result, def_result);
//    }
//
//    public RetType forTestDecl(TestDecl that) {
//        RetType name_result = that.getName().accept(this);
//        List<RetType> gens_result = recurOnListOfGeneratorClause(that.getGens());
//        RetType expr_result = that.getExpr().accept(this);
//        return forTestDeclOnly(that, name_result, gens_result, expr_result);
//    }
//
//    public RetType forPropertyDecl(PropertyDecl that) {
//        Option<RetType> name_result = recurOnOptionOfId(that.getName());
//        List<RetType> params_result = recurOnListOfParam(that.getParams());
//        RetType expr_result = that.getExpr().accept(this);
//        return forPropertyDeclOnly(that, name_result, params_result, expr_result);
//    }
//
//    public RetType forAbsExternalSyntax(AbsExternalSyntax that) {
//        RetType openExpander_result = that.getOpenExpander().accept(this);
//        RetType name_result = that.getName().accept(this);
//        RetType closeExpander_result = that.getCloseExpander().accept(this);
//        return forAbsExternalSyntaxOnly(that, openExpander_result, name_result, closeExpander_result);
//    }
//
//    public RetType forExternalSyntax(ExternalSyntax that) {
//        RetType openExpander_result = that.getOpenExpander().accept(this);
//        RetType name_result = that.getName().accept(this);
//        RetType closeExpander_result = that.getCloseExpander().accept(this);
//        RetType expr_result = that.getExpr().accept(this);
//        return forExternalSyntaxOnly(that, openExpander_result, name_result, closeExpander_result, expr_result);
//    }
//
//    public RetType forGrammarDef(GrammarDef that) {
//        RetType name_result = that.getName().accept(this);
//        List<RetType> extends_result = recurOnListOfQualifiedIdName(that.getExtends());
//        List<RetType> productions_result = recurOnListOfProductionDef(that.getProductions());
//        return forGrammarDefOnly(that, name_result, extends_result, productions_result);
//    }
//
//    public RetType forProductionDef(ProductionDef that) {
//        Option<RetType> modifier_result = recurOnOptionOfModifier(that.getModifier());
//        RetType name_result = that.getName().accept(this);
//        RetType type_result = that.getType().accept(this);
//        Option<RetType> extends_result = recurOnOptionOfQualifiedIdName(that.getExtends());
//        List<RetType> syntaxDefs_result = recurOnListOfSyntaxDef(that.getSyntaxDefs());
//        return forProductionDefOnly(that, modifier_result, name_result, type_result, extends_result, syntaxDefs_result);
//    }
//
//    public RetType forSyntaxDef(SyntaxDef that) {
//        List<RetType> syntaxSymbols_result = recurOnListOfSyntaxSymbol(that.getSyntaxSymbols());
//        RetType transformationExpression_result = that.getTransformationExpression().accept(this);
//        return forSyntaxDefOnly(that, syntaxSymbols_result, transformationExpression_result);
//    }
//
//    public RetType forSyntaxSymbol(SyntaxSymbol that) {
//        return forSyntaxSymbolOnly(that);
//    }
//
//    public RetType forPrefixedSymbol(PrefixedSymbol that) {
//        Option<RetType> id_result = recurOnOptionOfId(that.getId());
//        RetType symbol_result = that.getSymbol().accept(this);
//        return forPrefixedSymbolOnly(that, id_result, symbol_result);
//    }
//
//    public RetType forOptionalSymbol(OptionalSymbol that) {
//        RetType symbol_result = that.getSymbol().accept(this);
//        return forOptionalSymbolOnly(that, symbol_result);
//    }
//
//    public RetType forRepeatSymbol(RepeatSymbol that) {
//        RetType symbol_result = that.getSymbol().accept(this);
//        return forRepeatSymbolOnly(that, symbol_result);
//    }
//
//    public RetType forRepeatOneOrMoreSymbol(RepeatOneOrMoreSymbol that) {
//        RetType symbol_result = that.getSymbol().accept(this);
//        return forRepeatOneOrMoreSymbolOnly(that, symbol_result);
//    }
//
//    public RetType forWhitespaceSymbol(WhitespaceSymbol that) {
//        RetType symbol_result = that.getSymbol().accept(this);
//        return forWhitespaceSymbolOnly(that, symbol_result);
//    }
//
//    public RetType forItemSymbol(ItemSymbol that) {
//        return forItemSymbolOnly(that);
//    }
//
//    public RetType forNonterminalSymbol(NonterminalSymbol that) {
//        RetType nonterminal_result = that.getNonterminal().accept(this);
//        return forNonterminalSymbolOnly(that, nonterminal_result);
//    }
//
//    public RetType forKeywordSymbol(KeywordSymbol that) {
//        return forKeywordSymbolOnly(that);
//    }
//
//    public RetType forTokenSymbol(TokenSymbol that) {
//        return forTokenSymbolOnly(that);
//    }
//
//    public RetType forNotPredicateSymbol(NotPredicateSymbol that) {
//        RetType symbol_result = that.getSymbol().accept(this);
//        return forNotPredicateSymbolOnly(that, symbol_result);
//    }
//
//    public RetType forAndPredicateSymbol(AndPredicateSymbol that) {
//        RetType symbol_result = that.getSymbol().accept(this);
//        return forAndPredicateSymbolOnly(that, symbol_result);
//    }
//
//    public RetType forAsExpr(AsExpr that) {
//        RetType expr_result = that.getExpr().accept(this);
//        RetType type_result = that.getType().accept(this);
//        return forAsExprOnly(that, expr_result, type_result);
//    }
//
//    public RetType forAsIfExpr(AsIfExpr that) {
//        RetType expr_result = that.getExpr().accept(this);
//        RetType type_result = that.getType().accept(this);
//        return forAsIfExprOnly(that, expr_result, type_result);
//    }
//
//    public RetType forAssignment(Assignment that) {
//        List<RetType> lhs_result = recurOnListOfLHS(that.getLhs());
//        Option<RetType> opr_result = recurOnOptionOfOp(that.getOpr());
//        RetType rhs_result = that.getRhs().accept(this);
//        return forAssignmentOnly(that, lhs_result, opr_result, rhs_result);
//    }
//
//    public RetType forCaseExpr(CaseExpr that) {
//        Option<RetType> param_result = recurOnOptionOfExpr(that.getParam());
//        Option<RetType> compare_result = recurOnOptionOfOp(that.getCompare());
//        List<RetType> clauses_result = recurOnListOfCaseClause(that.getClauses());
//        Option<RetType> elseClause_result = recurOnOptionOfBlock(that.getElseClause());
//        return forCaseExprOnly(that, param_result, compare_result, clauses_result, elseClause_result);
//    }
//
//    public RetType forDo(Do that) {
//        List<RetType> fronts_result = recurOnListOfDoFront(that.getFronts());
//        return forDoOnly(that, fronts_result);
//    }
//
//    public RetType forFor(For that) {
//        List<RetType> gens_result = recurOnListOfGeneratorClause(that.getGens());
//        RetType body_result = that.getBody().accept(this);
//        return forForOnly(that, gens_result, body_result);
//    }
//
//    public RetType forIf(If that) {
//        List<RetType> clauses_result = recurOnListOfIfClause(that.getClauses());
//        Option<RetType> elseClause_result = recurOnOptionOfBlock(that.getElseClause());
//        return forIfOnly(that, clauses_result, elseClause_result);
//    }
//
//    public RetType forLabel(Label that) {
//        RetType name_result = that.getName().accept(this);
//        RetType body_result = that.getBody().accept(this);
//        return forLabelOnly(that, name_result, body_result);
//    }
//
//    public RetType forObjectExpr(ObjectExpr that) {
//        List<RetType> extendsClause_result = recurOnListOfTraitTypeWhere(that.getExtendsClause());
//        List<RetType> decls_result = recurOnListOfDecl(that.getDecls());
//        return forObjectExprOnly(that, extendsClause_result, decls_result);
//    }
//
//    public RetType for_RewriteObjectExpr(_RewriteObjectExpr that) {
//        List<RetType> extendsClause_result = recurOnListOfTraitTypeWhere(that.getExtendsClause());
//        List<RetType> decls_result = recurOnListOfDecl(that.getDecls());
//        List<RetType> staticParams_result = recurOnListOfStaticParam(that.getStaticParams());
//        List<RetType> staticArgs_result = recurOnListOfStaticArg(that.getStaticArgs());
//        Option<List<RetType>> params_result = recurOnOptionOfListOfParam(that.getParams());
//        return for_RewriteObjectExprOnly(that, extendsClause_result, decls_result, staticParams_result, staticArgs_result, params_result);
//    }
//
//    public RetType forTry(Try that) {
//        RetType body_result = that.getBody().accept(this);
//        Option<RetType> catchClause_result = recurOnOptionOfCatch(that.getCatchClause());
//        List<RetType> forbid_result = recurOnListOfTraitType(that.getForbid());
//        Option<RetType> finallyClause_result = recurOnOptionOfBlock(that.getFinallyClause());
//        return forTryOnly(that, body_result, catchClause_result, forbid_result, finallyClause_result);
//    }
//
//    public RetType forArgExpr(ArgExpr that) {
//        List<RetType> exprs_result = recurOnListOfExpr(that.getExprs());
//        Option<RetType> varargs_result = recurOnOptionOfVarargsExpr(that.getVarargs());
//        List<RetType> keywords_result = recurOnListOfBinding(that.getKeywords());
//        return forArgExprOnly(that, exprs_result, varargs_result, keywords_result);
//    }
//
//    public RetType forTupleExpr(TupleExpr that) {
//        List<RetType> exprs_result = recurOnListOfExpr(that.getExprs());
//        return forArgExprOnly(that, exprs_result);
//    }
//
//    public RetType forTypecase(Typecase that) {
//        List<RetType> bind_result = recurOnListOfBinding(that.getBind());
//        List<RetType> clauses_result = recurOnListOfTypecaseClause(that.getClauses());
//        Option<RetType> elseClause_result = recurOnOptionOfBlock(that.getElseClause());
//        return forTypecaseOnly(that, bind_result, clauses_result, elseClause_result);
//    }
//
//    public RetType forWhile(While that) {
//        RetType test_result = that.getTest().accept(this);
//        RetType body_result = that.getBody().accept(this);
//        return forWhileOnly(that, test_result, body_result);
//    }
//
//    public RetType forAccumulator(Accumulator that) {
//        RetType opr_result = that.getOpr().accept(this);
//        List<RetType> gens_result = recurOnListOfGeneratorClause(that.getGens());
//        RetType body_result = that.getBody().accept(this);
//        return forAccumulatorOnly(that, opr_result, gens_result, body_result);
//    }
//
//    public RetType forArrayComprehension(ArrayComprehension that) {
//        List<RetType> clauses_result = recurOnListOfArrayComprehensionClause(that.getClauses());
//        return forArrayComprehensionOnly(that, clauses_result);
//    }
//
//    public RetType forAtomicExpr(AtomicExpr that) {
//        RetType expr_result = that.getExpr().accept(this);
//        return forAtomicExprOnly(that, expr_result);
//    }
//
//    public RetType forExit(Exit that) {
//        Option<RetType> target_result = recurOnOptionOfId(that.getTarget());
//        Option<RetType> returnExpr_result = recurOnOptionOfExpr(that.getReturnExpr());
//        return forExitOnly(that, target_result, returnExpr_result);
//    }
//
//    public RetType forSpawn(Spawn that) {
//        RetType body_result = that.getBody().accept(this);
//        return forSpawnOnly(that, body_result);
//    }
//
//    public RetType forThrow(Throw that) {
//        RetType expr_result = that.getExpr().accept(this);
//        return forThrowOnly(that, expr_result);
//    }
//
//    public RetType forTryAtomicExpr(TryAtomicExpr that) {
//        RetType expr_result = that.getExpr().accept(this);
//        return forTryAtomicExprOnly(that, expr_result);
//    }
//
//    public RetType forFnExpr(FnExpr that) {
//        RetType name_result = that.getName().accept(this);
//        List<RetType> staticParams_result = recurOnListOfStaticParam(that.getStaticParams());
//        List<RetType> params_result = recurOnListOfParam(that.getParams());
//        Option<RetType> returnType_result = recurOnOptionOfType(that.getReturnType());
//        RetType where_result = that.getWhere().accept(this);
//        Option<List<RetType>> throwsClause_result = recurOnOptionOfListOfTraitType(that.getThrowsClause());
//        RetType body_result = that.getBody().accept(this);
//        return forFnExprOnly(that, name_result, staticParams_result, params_result, returnType_result, where_result, throwsClause_result, body_result);
//    }
//
//    public RetType forGeneratedExpr(GeneratedExpr that) {
//        RetType expr_result = that.getExpr().accept(this);
//        List<RetType> gens_result = recurOnListOfGeneratorClause(that.getGens());
//        return forGeneratedExprOnly(that, expr_result, gens_result);
//    }
//
//    public RetType forOprExpr(OprExpr that) {
//        List<RetType> ops_result = recurOnListOfQualifiedOpName(that.getOps());
//        List<RetType> args_result = recurOnListOfExpr(that.getArgs());
//        return forOprExprOnly(that, ops_result, args_result);
//    }
//
//    public RetType forSubscriptExpr(SubscriptExpr that) {
//        RetType obj_result = that.getObj().accept(this);
//        List<RetType> subs_result = recurOnListOfExpr(that.getSubs());
//        Option<RetType> op_result = recurOnOptionOfEnclosing(that.getOp());
//        return forSubscriptExprOnly(that, obj_result, subs_result, op_result);
//    }
//
//    public RetType forCoercionInvocation(CoercionInvocation that) {
//        RetType type_result = that.getType().accept(this);
//        List<RetType> staticArgs_result = recurOnListOfStaticArg(that.getStaticArgs());
//        RetType arg_result = that.getArg().accept(this);
//        return forCoercionInvocationOnly(that, type_result, staticArgs_result, arg_result);
//    }
//
//    public RetType forMethodInvocation(MethodInvocation that) {
//        RetType obj_result = that.getObj().accept(this);
//        RetType method_result = that.getMethod().accept(this);
//        List<RetType> staticArgs_result = recurOnListOfStaticArg(that.getStaticArgs());
//        RetType arg_result = that.getArg().accept(this);
//        return forMethodInvocationOnly(that, obj_result, method_result, staticArgs_result, arg_result);
//    }
//
//    public RetType forFieldRef(FieldRef that) {
//        RetType obj_result = that.getObj().accept(this);
//        RetType field_result = that.getField().accept(this);
//        return forFieldRefOnly(that, obj_result, field_result);
//    }
//
//    public RetType forFieldRefForSure(FieldRefForSure that) {
//        RetType obj_result = that.getObj().accept(this);
//        RetType field_result = that.getField().accept(this);
//        return forFieldRefForSureOnly(that, obj_result, field_result);
//    }
//
//    public RetType for_RewriteFieldRef(_RewriteFieldRef that) {
//        RetType obj_result = that.getObj().accept(this);
//        RetType field_result = that.getField().accept(this);
//        return for_RewriteFieldRefOnly(that, obj_result, field_result);
//    }
//
//    public RetType forLooseJuxt(LooseJuxt that) {
//        List<RetType> exprs_result = recurOnListOfExpr(that.getExprs());
//        return forLooseJuxtOnly(that, exprs_result);
//    }
//
//    public RetType forTightJuxt(TightJuxt that) {
//        List<RetType> exprs_result = recurOnListOfExpr(that.getExprs());
//        return forTightJuxtOnly(that, exprs_result);
//    }
//
//    public RetType forMathPrimary(MathPrimary that) {
//        RetType front_result = that.getFront().accept(this);
//        List<RetType> rest_result = recurOnListOfMathItem(that.getRest());
//        return forMathPrimaryOnly(that, front_result, rest_result);
//    }
//
//    public RetType forFnRef(FnRef that) {
//        List<RetType> fns_result = recurOnListOfQualifiedIdName(that.getFns());
//        List<RetType> staticArgs_result = recurOnListOfStaticArg(that.getStaticArgs());
//        return forFnRefOnly(that, fns_result, staticArgs_result);
//    }
//
//    public RetType for_RewriteFnRef(_RewriteFnRef that) {
//        RetType fn_result = that.getFn().accept(this);
//        List<RetType> staticArgs_result = recurOnListOfStaticArg(that.getStaticArgs());
//        return for_RewriteFnRefOnly(that, fn_result, staticArgs_result);
//    }
//
//    public RetType forVarRef(VarRef that) {
//        RetType var_result = that.getVar().accept(this);
//        return forVarRefOnly(that, var_result);
//    }
//
//    public RetType forArrayElement(ArrayElement that) {
//        RetType element_result = that.getElement().accept(this);
//        return forArrayElementOnly(that, element_result);
//    }
//
//    public RetType forArrayElements(ArrayElements that) {
//        List<RetType> elements_result = recurOnListOfArrayExpr(that.getElements());
//        return forArrayElementsOnly(that, elements_result);
//    }
//
//    public RetType forArrowType(ArrowType that) {
//        RetType domain_result = that.getDomain().accept(this);
//        RetType range_result = that.getRange().accept(this);
//        Option<List<RetType>> throwsClause_result = recurOnOptionOfListOfType(that.getThrowsClause());
//        return forArrowTypeOnly(that, domain_result, range_result, throwsClause_result);
//    }
//
//    public RetType for_RewriteGenericArrowType(_RewriteGenericArrowType that) {
//        RetType domain_result = that.getDomain().accept(this);
//        RetType range_result = that.getRange().accept(this);
//        Option<List<RetType>> throwsClause_result = recurOnOptionOfListOfType(that.getThrowsClause());
//        List<RetType> staticParams_result = recurOnListOfStaticParam(that.getStaticParams());
//        RetType where_result = that.getWhere().accept(this);
//        return for_RewriteGenericArrowTypeOnly(that, domain_result, range_result, throwsClause_result, staticParams_result, where_result);
//    }
//
//    public RetType forBottomType(BottomType that) {
//        return forBottomTypeOnly(that);
//    }
//
//    public RetType forArrayType(ArrayType that) {
//        RetType element_result = that.getElement().accept(this);
//        RetType indices_result = that.getIndices().accept(this);
//        return forArrayTypeOnly(that, element_result, indices_result);
//    }
//
//    public RetType forIdType(IdType that) {
//        RetType name_result = that.getName().accept(this);
//        return forIdTypeOnly(that, name_result);
//    }
//
//    public RetType forInferenceVarType(InferenceVarType that) {
//        return forInferenceVarTypeOnly(that);
//    }
//
//    public RetType forMatrixType(MatrixType that) {
//        RetType element_result = that.getElement().accept(this);
//        List<RetType> dimensions_result = recurOnListOfExtentRange(that.getDimensions());
//        return forMatrixTypeOnly(that, element_result, dimensions_result);
//    }
//
//    public RetType forInstantiatedType(InstantiatedType that) {
//        RetType name_result = that.getName().accept(this);
//        List<RetType> args_result = recurOnListOfStaticArg(that.getArgs());
//        return forInstantiatedTypeOnly(that, name_result, args_result);
//    }
//
//    public RetType forArgType(ArgType that) {
//        List<RetType> elements_result = recurOnListOfType(that.getElements());
//        Option<RetType> varargs_result = recurOnOptionOfVarargsType(that.getVarargs());
//        List<RetType> keywords_result = recurOnListOfKeywordType(that.getKeywords());
//        return forArgTypeOnly(that, elements_result, varargs_result, keywords_result);
//    }
//
//    public RetType forTupleType(TupleType that) {
//        List<RetType> elements_result = recurOnListOfType(that.getElements());
//        return forTupleTypeOnly(that, elements_result, varargs_result, keywords_result);
//    }
//
//    public RetType forVoidType(VoidType that) {
//        return forVoidTypeOnly(that);
//    }
//
//    public RetType forIntersectionType(IntersectionType that) {
//        Set<RetType> elements_result = recurOnSetOfType(that.getElements());
//        return forIntersectionTypeOnly(that, elements_result);
//    }
//
//    public RetType forUnionType(UnionType that) {
//        Set<RetType> elements_result = recurOnSetOfType(that.getElements());
//        return forUnionTypeOnly(that, elements_result);
//    }
//
//    public RetType forAndType(AndType that) {
//        RetType first_result = that.getFirst().accept(this);
//        RetType second_result = that.getSecond().accept(this);
//        return forAndTypeOnly(that, first_result, second_result);
//    }
//
//    public RetType forOrType(OrType that) {
//        RetType first_result = that.getFirst().accept(this);
//        RetType second_result = that.getSecond().accept(this);
//        return forOrTypeOnly(that, first_result, second_result);
//    }
//
//    public RetType forTaggedDimType(TaggedDimType that) {
//        RetType type_result = that.getType().accept(this);
//        RetType dim_result = that.getDim().accept(this);
//        Option<RetType> unit_result = recurOnOptionOfExpr(that.getUnit());
//        return forTaggedDimTypeOnly(that, type_result, dim_result, unit_result);
//    }
//
//    public RetType forTaggedUnitType(TaggedUnitType that) {
//        RetType type_result = that.getType().accept(this);
//        RetType unit_result = that.getUnit().accept(this);
//        return forTaggedUnitTypeOnly(that, type_result, unit_result);
//    }
//
//    public RetType forIdArg(IdArg that) {
//        RetType name_result = that.getName().accept(this);
//        return forIdArgOnly(that, name_result);
//    }
//
//    public RetType forTypeArg(TypeArg that) {
//        RetType type_result = that.getType().accept(this);
//        return forTypeArgOnly(that, type_result);
//    }
//
//    public RetType forIntArg(IntArg that) {
//        RetType val_result = that.getVal().accept(this);
//        return forIntArgOnly(that, val_result);
//    }
//
//    public RetType forBoolArg(BoolArg that) {
//        RetType bool_result = that.getBool().accept(this);
//        return forBoolArgOnly(that, bool_result);
//    }
//
//    public RetType forOprArg(OprArg that) {
//        RetType name_result = that.getName().accept(this);
//        return forOprArgOnly(that, name_result);
//    }
//
//    public RetType forDimArg(DimArg that) {
//        RetType dim_result = that.getDim().accept(this);
//        return forDimArgOnly(that, dim_result);
//    }
//
//    public RetType forUnitArg(UnitArg that) {
//        RetType unit_result = that.getUnit().accept(this);
//        return forUnitArgOnly(that, unit_result);
//    }
//
//    public RetType for_RewriteImplicitType(_RewriteImplicitType that) {
//        return for_RewriteImplicitTypeOnly(that);
//    }
//
//    public RetType for_RewriteIntersectionType(_RewriteIntersectionType that) {
//        List<RetType> elements_result = recurOnListOfType(that.getElements());
//        return for_RewriteIntersectionTypeOnly(that, elements_result);
//    }
//
//    public RetType for_RewriteUnionType(_RewriteUnionType that) {
//        List<RetType> elements_result = recurOnListOfType(that.getElements());
//        return for_RewriteUnionTypeOnly(that, elements_result);
//    }
//
//    public RetType for_RewriteFixedPointType(_RewriteFixedPointType that) {
//        RetType var_result = that.getVar().accept(this);
//        RetType body_result = that.getBody().accept(this);
//        return for_RewriteFixedPointTypeOnly(that, var_result, body_result);
//    }
//
//    public RetType forNumberConstraint(NumberConstraint that) {
//        RetType val_result = that.getVal().accept(this);
//        return forNumberConstraintOnly(that, val_result);
//    }
//
//    public RetType forIntRef(IntRef that) {
//        RetType name_result = that.getName().accept(this);
//        return forIntRefOnly(that, name_result);
//    }
//
//    public RetType forSumConstraint(SumConstraint that) {
//        RetType left_result = that.getLeft().accept(this);
//        RetType right_result = that.getRight().accept(this);
//        return forSumConstraintOnly(that, left_result, right_result);
//    }
//
//    public RetType forMinusConstraint(MinusConstraint that) {
//        RetType left_result = that.getLeft().accept(this);
//        RetType right_result = that.getRight().accept(this);
//        return forMinusConstraintOnly(that, left_result, right_result);
//    }
//
//    public RetType forProductConstraint(ProductConstraint that) {
//        RetType left_result = that.getLeft().accept(this);
//        RetType right_result = that.getRight().accept(this);
//        return forProductConstraintOnly(that, left_result, right_result);
//    }
//
//    public RetType forExponentConstraint(ExponentConstraint that) {
//        RetType left_result = that.getLeft().accept(this);
//        RetType right_result = that.getRight().accept(this);
//        return forExponentConstraintOnly(that, left_result, right_result);
//    }
//
//    public RetType forBoolConstant(BoolConstant that) {
//        return forBoolConstantOnly(that);
//    }
//
//    public RetType forBoolRef(BoolRef that) {
//        RetType name_result = that.getName().accept(this);
//        return forBoolRefOnly(that, name_result);
//    }
//
//    public RetType forNotConstraint(NotConstraint that) {
//        RetType bool_result = that.getBool().accept(this);
//        return forNotConstraintOnly(that, bool_result);
//    }
//
//    public RetType forOrConstraint(OrConstraint that) {
//        RetType left_result = that.getLeft().accept(this);
//        RetType right_result = that.getRight().accept(this);
//        return forOrConstraintOnly(that, left_result, right_result);
//    }
//
//    public RetType forAndConstraint(AndConstraint that) {
//        RetType left_result = that.getLeft().accept(this);
//        RetType right_result = that.getRight().accept(this);
//        return forAndConstraintOnly(that, left_result, right_result);
//    }
//
//    public RetType forImpliesConstraint(ImpliesConstraint that) {
//        RetType left_result = that.getLeft().accept(this);
//        RetType right_result = that.getRight().accept(this);
//        return forImpliesConstraintOnly(that, left_result, right_result);
//    }
//
//    public RetType forBEConstraint(BEConstraint that) {
//        RetType left_result = that.getLeft().accept(this);
//        RetType right_result = that.getRight().accept(this);
//        return forBEConstraintOnly(that, left_result, right_result);
//    }
//
//    public RetType forBaseDim(BaseDim that) {
//        return forBaseDimOnly(that);
//    }
//
//    public RetType forDimRef(DimRef that) {
//        RetType name_result = that.getName().accept(this);
//        return forDimRefOnly(that, name_result);
//    }
//
//    public RetType forProductDim(ProductDim that) {
//        RetType multiplier_result = that.getMultiplier().accept(this);
//        RetType multiplicand_result = that.getMultiplicand().accept(this);
//        return forProductDimOnly(that, multiplier_result, multiplicand_result);
//    }
//
//    public RetType forQuotientDim(QuotientDim that) {
//        RetType numerator_result = that.getNumerator().accept(this);
//        RetType denominator_result = that.getDenominator().accept(this);
//        return forQuotientDimOnly(that, numerator_result, denominator_result);
//    }
//
//    public RetType forExponentDim(ExponentDim that) {
//        RetType base_result = that.getBase().accept(this);
//        RetType power_result = that.getPower().accept(this);
//        return forExponentDimOnly(that, base_result, power_result);
//    }
//
//    public RetType forOpDim(OpDim that) {
//        RetType val_result = that.getVal().accept(this);
//        RetType op_result = that.getOp().accept(this);
//        return forOpDimOnly(that, val_result, op_result);
//    }
//
//    public RetType forWhereType(WhereType that) {
//        RetType name_result = that.getName().accept(this);
//        List<RetType> supers_result = recurOnListOfTraitType(that.getSupers());
//        return forWhereTypeOnly(that, name_result, supers_result);
//    }
//
//    public RetType forWhereNat(WhereNat that) {
//        RetType name_result = that.getName().accept(this);
//        return forWhereNatOnly(that, name_result);
//    }
//
//    public RetType forWhereInt(WhereInt that) {
//        RetType name_result = that.getName().accept(this);
//        return forWhereIntOnly(that, name_result);
//    }
//
//    public RetType forWhereBool(WhereBool that) {
//        RetType name_result = that.getName().accept(this);
//        return forWhereBoolOnly(that, name_result);
//    }
//
//    public RetType forWhereUnit(WhereUnit that) {
//        RetType name_result = that.getName().accept(this);
//        return forWhereUnitOnly(that, name_result);
//    }
//
//    public RetType forWhereExtends(WhereExtends that) {
//        RetType name_result = that.getName().accept(this);
//        List<RetType> supers_result = recurOnListOfTraitType(that.getSupers());
//        return forWhereExtendsOnly(that, name_result, supers_result);
//    }
//
//    public RetType forTypeAlias(TypeAlias that) {
//        RetType name_result = that.getName().accept(this);
//        List<RetType> staticParams_result = recurOnListOfStaticParam(that.getStaticParams());
//        RetType type_result = that.getType().accept(this);
//        return forTypeAliasOnly(that, name_result, staticParams_result, type_result);
//    }
//
//    public RetType forWhereCoerces(WhereCoerces that) {
//        RetType left_result = that.getLeft().accept(this);
//        RetType right_result = that.getRight().accept(this);
//        return forWhereCoercesOnly(that, left_result, right_result);
//    }
//
//    public RetType forWhereWidens(WhereWidens that) {
//        RetType left_result = that.getLeft().accept(this);
//        RetType right_result = that.getRight().accept(this);
//        return forWhereWidensOnly(that, left_result, right_result);
//    }
//
//    public RetType forWhereWidensCoerces(WhereWidensCoerces that) {
//        RetType left_result = that.getLeft().accept(this);
//        RetType right_result = that.getRight().accept(this);
//        return forWhereWidensCoercesOnly(that, left_result, right_result);
//    }
//
//    public RetType forWhereEquals(WhereEquals that) {
//        RetType left_result = that.getLeft().accept(this);
//        RetType right_result = that.getRight().accept(this);
//        return forWhereEqualsOnly(that, left_result, right_result);
//    }
//
//    public RetType forUnitConstraint(UnitConstraint that) {
//        RetType name_result = that.getName().accept(this);
//        return forUnitConstraintOnly(that, name_result);
//    }
//
//    public RetType forLEConstraint(LEConstraint that) {
//        RetType left_result = that.getLeft().accept(this);
//        RetType right_result = that.getRight().accept(this);
//        return forLEConstraintOnly(that, left_result, right_result);
//    }
//
//    public RetType forLTConstraint(LTConstraint that) {
//        RetType left_result = that.getLeft().accept(this);
//        RetType right_result = that.getRight().accept(this);
//        return forLTConstraintOnly(that, left_result, right_result);
//    }
//
//    public RetType forGEConstraint(GEConstraint that) {
//        RetType left_result = that.getLeft().accept(this);
//        RetType right_result = that.getRight().accept(this);
//        return forGEConstraintOnly(that, left_result, right_result);
//    }
//
//    public RetType forGTConstraint(GTConstraint that) {
//        RetType left_result = that.getLeft().accept(this);
//        RetType right_result = that.getRight().accept(this);
//        return forGTConstraintOnly(that, left_result, right_result);
//    }
//
//    public RetType forIEConstraint(IEConstraint that) {
//        RetType left_result = that.getLeft().accept(this);
//        RetType right_result = that.getRight().accept(this);
//        return forIEConstraintOnly(that, left_result, right_result);
//    }
//
//    public RetType forBoolConstraintExpr(BoolConstraintExpr that) {
//        RetType constraint_result = that.getConstraint().accept(this);
//        return forBoolConstraintExprOnly(that, constraint_result);
//    }
//
//    public RetType forContract(Contract that) {
//        Option<List<RetType>> requires_result = recurOnOptionOfListOfExpr(that.getRequires());
//        Option<List<RetType>> ensures_result = recurOnOptionOfListOfEnsuresClause(that.getEnsures());
//        Option<List<RetType>> invariants_result = recurOnOptionOfListOfExpr(that.getInvariants());
//        return forContractOnly(that, requires_result, ensures_result, invariants_result);
//    }
//
//    public RetType forEnsuresClause(EnsuresClause that) {
//        RetType post_result = that.getPost().accept(this);
//        Option<RetType> pre_result = recurOnOptionOfExpr(that.getPre());
//        return forEnsuresClauseOnly(that, post_result, pre_result);
//    }
//
//    public RetType forModifierAbstract(ModifierAbstract that) {
//        return forModifierAbstractOnly(that);
//    }
//
//    public RetType forModifierAtomic(ModifierAtomic that) {
//        return forModifierAtomicOnly(that);
//    }
//
//    public RetType forModifierGetter(ModifierGetter that) {
//        return forModifierGetterOnly(that);
//    }
//
//    public RetType forModifierHidden(ModifierHidden that) {
//        return forModifierHiddenOnly(that);
//    }
//
//    public RetType forModifierIO(ModifierIO that) {
//        return forModifierIOOnly(that);
//    }
//
//    public RetType forModifierOverride(ModifierOverride that) {
//        return forModifierOverrideOnly(that);
//    }
//
//    public RetType forModifierPrivate(ModifierPrivate that) {
//        return forModifierPrivateOnly(that);
//    }
//
//    public RetType forModifierSettable(ModifierSettable that) {
//        return forModifierSettableOnly(that);
//    }
//
//    public RetType forModifierSetter(ModifierSetter that) {
//        return forModifierSetterOnly(that);
//    }
//
//    public RetType forModifierTest(ModifierTest that) {
//        return forModifierTestOnly(that);
//    }
//
//    public RetType forModifierTransient(ModifierTransient that) {
//        return forModifierTransientOnly(that);
//    }
//
//    public RetType forModifierValue(ModifierValue that) {
//        return forModifierValueOnly(that);
//    }
//
//    public RetType forModifierVar(ModifierVar that) {
//        return forModifierVarOnly(that);
//    }
//
//    public RetType forModifierWidens(ModifierWidens that) {
//        return forModifierWidensOnly(that);
//    }
//
//    public RetType forModifierWrapped(ModifierWrapped that) {
//        return forModifierWrappedOnly(that);
//    }
//
//    public RetType forOperatorParam(OperatorParam that) {
//        RetType name_result = that.getName().accept(this);
//        return forOperatorParamOnly(that, name_result);
//    }
//
//    public RetType forBoolParam(BoolParam that) {
//        RetType name_result = that.getName().accept(this);
//        return forBoolParamOnly(that, name_result);
//    }
//
//    public RetType forDimensionParam(DimensionParam that) {
//        RetType name_result = that.getName().accept(this);
//        return forDimensionParamOnly(that, name_result);
//    }
//
//    public RetType forIntParam(IntParam that) {
//        RetType name_result = that.getName().accept(this);
//        return forIntParamOnly(that, name_result);
//    }
//
//    public RetType forNatParam(NatParam that) {
//        RetType name_result = that.getName().accept(this);
//        return forNatParamOnly(that, name_result);
//    }
//
//    public RetType forSimpleTypeParam(SimpleTypeParam that) {
//        RetType name_result = that.getName().accept(this);
//        List<RetType> extendsClause_result = recurOnListOfTraitType(that.getExtendsClause());
//        return forSimpleTypeParamOnly(that, name_result, extendsClause_result);
//    }
//
//    public RetType forUnitParam(UnitParam that) {
//        RetType name_result = that.getName().accept(this);
//        Option<RetType> dim_result = recurOnOptionOfDimExpr(that.getDim());
//        return forUnitParamOnly(that, name_result, dim_result);
//    }
//
//    public RetType forAPIName(APIName that) {
//        List<RetType> ids_result = recurOnListOfId(that.getIds());
//        return forAPINameOnly(that, ids_result);
//    }
//
//    public RetType forQualifiedIdName(QualifiedIdName that) {
//        Option<RetType> api_result = recurOnOptionOfAPIName(that.getApi());
//        RetType name_result = that.getName().accept(this);
//        return forQualifiedIdNameOnly(that, api_result, name_result);
//    }
//
//    public RetType forQualifiedOpName(QualifiedOpName that) {
//        Option<RetType> api_result = recurOnOptionOfAPIName(that.getApi());
//        RetType name_result = that.getName().accept(this);
//        return forQualifiedOpNameOnly(that, api_result, name_result);
//    }
//
//    public RetType forId(Id that) {
//        return forIdOnly(that);
//    }
//
//    public RetType forOp(Op that) {
//        return forOpOnly(that);
//    }
//
//    public RetType forEnclosing(Enclosing that) {
//        RetType open_result = that.getOpen().accept(this);
//        RetType close_result = that.getClose().accept(this);
//        return forEnclosingOnly(that, open_result, close_result);
//    }
//
//    public RetType forAnonymousFnName(AnonymousFnName that) {
//        return forAnonymousFnNameOnly(that);
//    }
//
//    public RetType forConstructorFnName(ConstructorFnName that) {
//        RetType def_result = that.getDef().accept(this);
//        return forConstructorFnNameOnly(that, def_result);
//    }
//
//    public RetType forArrayComprehensionClause(ArrayComprehensionClause that) {
//        List<RetType> bind_result = recurOnListOfExpr(that.getBind());
//        RetType init_result = that.getInit().accept(this);
//        List<RetType> gens_result = recurOnListOfGeneratorClause(that.getGens());
//        return forArrayComprehensionClauseOnly(that, bind_result, init_result, gens_result);
//    }
//
//    public RetType forKeywordExpr(KeywordExpr that) {
//        RetType name_result = that.getName().accept(this);
//        RetType init_result = that.getInit().accept(this);
//        return forKeywordExprOnly(that, name_result, init_result);
//    }
//
//    public RetType forCaseClause(CaseClause that) {
//        RetType match_result = that.getMatch().accept(this);
//        RetType body_result = that.getBody().accept(this);
//        return forCaseClauseOnly(that, match_result, body_result);
//    }
//
//    public RetType forCatch(Catch that) {
//        RetType name_result = that.getName().accept(this);
//        List<RetType> clauses_result = recurOnListOfCatchClause(that.getClauses());
//        return forCatchOnly(that, name_result, clauses_result);
//    }
//
//    public RetType forCatchClause(CatchClause that) {
//        RetType match_result = that.getMatch().accept(this);
//        RetType body_result = that.getBody().accept(this);
//        return forCatchClauseOnly(that, match_result, body_result);
//    }
//
//    public RetType forDoFront(DoFront that) {
//        Option<RetType> loc_result = recurOnOptionOfExpr(that.getLoc());
//        RetType expr_result = that.getExpr().accept(this);
//        return forDoFrontOnly(that, loc_result, expr_result);
//    }
//
//    public RetType forIfClause(IfClause that) {
//        RetType test_result = that.getTest().accept(this);
//        RetType body_result = that.getBody().accept(this);
//        return forIfClauseOnly(that, test_result, body_result);
//    }
//
//    public RetType forTypecaseClause(TypecaseClause that) {
//        List<RetType> match_result = recurOnListOfType(that.getMatch());
//        RetType body_result = that.getBody().accept(this);
//        return forTypecaseClauseOnly(that, match_result, body_result);
//    }
//
//    public RetType forExtentRange(ExtentRange that) {
//        Option<RetType> base_result = recurOnOptionOfStaticArg(that.getBase());
//        Option<RetType> size_result = recurOnOptionOfStaticArg(that.getSize());
//        return forExtentRangeOnly(that, base_result, size_result);
//    }
//
//    public RetType forGeneratorClause(GeneratorClause that) {
//        List<RetType> bind_result = recurOnListOfId(that.getBind());
//        RetType init_result = that.getInit().accept(this);
//        return forGeneratorClauseOnly(that, bind_result, init_result);
//    }
//
//    public RetType forVarargsExpr(VarargsExpr that) {
//        RetType varargs_result = that.getVarargs().accept(this);
//        return forVarargsExprOnly(that, varargs_result);
//    }
//
//    public RetType forVarargsType(VarargsType that) {
//        RetType type_result = that.getType().accept(this);
//        return forVarargsTypeOnly(that, type_result);
//    }
//
//    public RetType forKeywordType(KeywordType that) {
//        RetType name_result = that.getName().accept(this);
//        RetType type_result = that.getType().accept(this);
//        return forKeywordTypeOnly(that, name_result, type_result);
//    }
//
//    public RetType forTraitTypeWhere(TraitTypeWhere that) {
//        RetType type_result = that.getType().accept(this);
//        RetType where_result = that.getWhere().accept(this);
//        return forTraitTypeWhereOnly(that, type_result, where_result);
//    }
//
//    public RetType forIndices(Indices that) {
//        List<RetType> extents_result = recurOnListOfExtentRange(that.getExtents());
//        return forIndicesOnly(that, extents_result);
//    }
//
//    public RetType forSquareDimUnit(SquareDimUnit that) {
//        return forSquareDimUnitOnly(that);
//    }
//
//    public RetType forCubicDimUnit(CubicDimUnit that) {
//        return forCubicDimUnitOnly(that);
//    }
//
//    public RetType forInverseDimUnit(InverseDimUnit that) {
//        return forInverseDimUnitOnly(that);
//    }
//
//    public RetType forParenthesisDelimitedMI(ParenthesisDelimitedMI that) {
//        RetType expr_result = that.getExpr().accept(this);
//        return forParenthesisDelimitedMIOnly(that, expr_result);
//    }
//
//    public RetType forNonParenthesisDelimitedMI(NonParenthesisDelimitedMI that) {
//        RetType expr_result = that.getExpr().accept(this);
//        return forNonParenthesisDelimitedMIOnly(that, expr_result);
//    }
//
//    public RetType forExponentiationMI(ExponentiationMI that) {
//        RetType op_result = that.getOp().accept(this);
//        Option<RetType> expr_result = recurOnOptionOfExpr(that.getExpr());
//        return forExponentiationMIOnly(that, op_result, expr_result);
//    }
//
//    public RetType forSubscriptingMI(SubscriptingMI that) {
//        RetType op_result = that.getOp().accept(this);
//        List<RetType> exprs_result = recurOnListOfExpr(that.getExprs());
//        return forSubscriptingMIOnly(that, op_result, exprs_result);
//    }
