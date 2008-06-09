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
import com.sun.fortress.compiler.index.CompilationUnitIndex;
import com.sun.fortress.compiler.index.ComponentIndex;
import com.sun.fortress.compiler.index.FunctionalMethod;
import com.sun.fortress.compiler.index.ObjectTraitIndex;
import com.sun.fortress.compiler.index.TraitIndex;
import com.sun.fortress.compiler.index.Method;
import com.sun.fortress.compiler.index.TypeConsIndex;
import com.sun.fortress.compiler.index.Variable;
import com.sun.fortress.compiler.typechecker.TypeEnv.BindingLookup;
import com.sun.fortress.compiler.typechecker.TypesUtil.ArgList;
import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.OprUtil;
import com.sun.fortress.nodes_util.Span;
import com.sun.fortress.useful.NI;

import edu.rice.cs.plt.collect.HashRelation;
import edu.rice.cs.plt.collect.Relation;
import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.lambda.Lambda;
import edu.rice.cs.plt.lambda.Lambda2;
import edu.rice.cs.plt.tuple.Option;
import edu.rice.cs.plt.tuple.Pair;

import java.util.*;

import java.io.PrintWriter;

import static com.sun.fortress.interpreter.evaluator.InterpreterBug.bug;
import static com.sun.fortress.compiler.TypeError.errorMsg;
import static edu.rice.cs.plt.tuple.Option.*;

public class TypeChecker extends NodeDepthFirstVisitor<TypeCheckerResult> {

    private TraitTable table;
    private StaticParamEnv staticParamEnv;
    private TypeEnv typeEnv;
    private final CompilationUnitIndex compilationUnit;
    //private final SubtypeChecker subtypeChecker;
    private final TypeAnalyzer subtypeChecker;
    private final Map<Id, Option<Set<Type>>> labelExitTypes; // Note: this is mutable state.

    public TypeChecker(TraitTable _table,
                       StaticParamEnv _staticParams,
                       TypeEnv _typeEnv,
                       CompilationUnitIndex _compilationUnit)
    {
        table = _table;
        staticParamEnv = _staticParams;
        typeEnv = _typeEnv;
        compilationUnit = _compilationUnit;
        subtypeChecker = TypeAnalyzer.make(table);
        labelExitTypes = new HashMap<Id, Option<Set<Type>>>();
    }

    private TypeChecker(TraitTable _table,
                       StaticParamEnv _staticParams,
                       TypeEnv _typeEnv,
                       CompilationUnitIndex _compilationUnit,
                       TypeAnalyzer _subtypeChecker,
                       Map<Id, Option<Set<Type>>> _labelExitTypes)
    {
        table = _table;
        staticParamEnv = _staticParams;
        typeEnv = _typeEnv;
        compilationUnit = _compilationUnit;
        subtypeChecker = _subtypeChecker;
        labelExitTypes = _labelExitTypes;
    }

    private static Type typeFromLValueBinds(List<LValueBind> bindings) {
        List<Type> results = new ArrayList<Type>();

        for (LValueBind binding: bindings) {
            if (binding.getType().isSome()) {
                results.add(binding.getType().unwrap());
            } else
                bug(binding, "Missing type.");
        }
        return NodeFactory.makeTupleType(results);
    }

    /**
     * Given a qualified ID, returns an unqualified version with the same span.
     */
    private static Id unqualifiedIdFromId(Id id) {
    	return new Id(id.getSpan(), id.getText());
    }
    
    private TypeChecker extend(List<StaticParam> newStaticParams, Option<List<Param>> newParams, WhereClause whereClause) {
        return new TypeChecker(table,
                               staticParamEnv.extend(newStaticParams, whereClause),
                               typeEnv.extend(newParams),
                               compilationUnit,
                               subtypeChecker.extend(newStaticParams, whereClause),
                               labelExitTypes);
    }

    private TypeChecker extend(List<StaticParam> newStaticParams, List<Param> newParams, WhereClause whereClause) {
        return new TypeChecker(table,
                               staticParamEnv.extend(newStaticParams, whereClause),
                               typeEnv.extendWithParams(newParams),
                               compilationUnit,
                               subtypeChecker.extend(newStaticParams, whereClause),
                               labelExitTypes);
    }

    private TypeChecker extend(List<StaticParam> newStaticParams, WhereClause whereClause) {
        return new TypeChecker(table,
                               staticParamEnv.extend(newStaticParams, whereClause),
                               typeEnv,
                               compilationUnit,
                               subtypeChecker.extend(newStaticParams, whereClause),
                               labelExitTypes);
    }

    private TypeChecker extend(WhereClause whereClause) {
        return new TypeChecker(table,
                               staticParamEnv.extend(Collections.<StaticParam>emptyList(), whereClause),
                               typeEnv,
                               compilationUnit,
                               subtypeChecker.extend(Collections.<StaticParam>emptyList(), whereClause),
                               labelExitTypes);
    }

    private TypeChecker extend(List<LValueBind> bindings) {
        return new TypeChecker(table, staticParamEnv,
                               typeEnv.extendWithLValues(bindings),
                               compilationUnit,
                               subtypeChecker,
                               labelExitTypes);
    }

    private TypeChecker extend(LocalVarDecl decl) {
        return new TypeChecker(table, staticParamEnv,
                               typeEnv.extend(decl),
                               compilationUnit,
                               subtypeChecker,
                               labelExitTypes);
    }

    private TypeChecker extend(Param newParam) {
        return new TypeChecker(table, staticParamEnv,
                               typeEnv.extend(newParam),
                               compilationUnit,
                               subtypeChecker,
                               labelExitTypes);
    }

    public TypeChecker extendWithMethods(Relation<IdOrOpOrAnonymousName, Method> methods) {
        return new TypeChecker(table, staticParamEnv,
                               typeEnv.extendWithMethods(methods),
                               compilationUnit,
                               subtypeChecker,
                               labelExitTypes);
    }

    public TypeChecker extendWithFunctions(Relation<IdOrOpOrAnonymousName, FunctionalMethod> methods) {
        return new TypeChecker(table, staticParamEnv,
                               typeEnv.extendWithFunctions(methods),
                               compilationUnit,
                               subtypeChecker,
                               labelExitTypes);
    }

    public TypeChecker extendWithFnDefs(Relation<IdOrOpOrAnonymousName, ? extends FnDef> fns) {
        return new TypeChecker(table, staticParamEnv,
                               typeEnv.extendWithFnDefs(fns),
                               compilationUnit,
                               subtypeChecker,
                               labelExitTypes);
    }

    public TypeChecker extendWithout(Set<? extends IdOrOpOrAnonymousName> names) {
        return new TypeChecker(table, staticParamEnv,
                               typeEnv.extendWithout(names),
                               compilationUnit,
                               subtypeChecker,
                               labelExitTypes);
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
                            errorMsg("Expected expression of type ", supertype, " ",
                                     "but was type ", subtype));
    }

    /**
     * Check the subtype relation for the given types.  If subtype <: supertype, then a TypeCheckerResult
     * for the given node and corresponding type constraints will be returned.  Otherwise, a TypeCheckerResult
     * for the given node with the a TypeError and the given error message will be returned.
     */
    private TypeCheckerResult checkSubtype(Type subtype, Type supertype, Node ast, String error) {
    	ConstraintFormula constraint = subtypeChecker.subtype(subtype, supertype); 
    	if( !constraint.isSatisfiable() ) {
    		// note that if it's satisfiable, it could still be later found to not be
            return new TypeCheckerResult(ast, TypeError.make(error, ast));
        } else {
            return new TypeCheckerResult(ast, constraint);
        }
    }


    /**
     * Check the subtype relation for the given types.  If subtype <: supertype, then a TypeCheckerResult
     * for the given node and corresponding type constraints will be returned, and the type of this node
     * will be the given type resultType.  Otherwise, a TypeCheckerResult
     * for the given node with the a TypeError and the given error message will be returned.
     */
    private TypeCheckerResult checkSubtype(Type subtype, Type supertype, Node ast, Type resultType, String error) {
    	ConstraintFormula constraint = subtypeChecker.subtype(subtype, supertype); 
    	if( !constraint.isSatisfiable() ) {
    		// note that if it's satisfiable, it could still be later found to not be
            return new TypeCheckerResult(ast, resultType, TypeError.make(error, ast));
        } else {
            return new TypeCheckerResult(ast, resultType, constraint);
        }
    }

    /**
     * Return an error complaining about usage of a label name as an identifier.
     * @param name the label name that is being used
     * @return a TypeError containing the error message and location
     */
    private StaticError makeLabelNameError(Id name) {
        return TypeError.make(errorMsg("Cannot use label name ", name.getText(),
                                       " as an identifier."),
                              name);
    }

    
    @Override
	public TypeCheckerResult forFnExpr(FnExpr that) {
    	
    	// Fn expressions have arrow type. They cannot have static arguments.
    	// They cannot have where clauses.
    	
    	// Ignore b/c we don't want to look up the name
    	// TypeCheckerResult name_result = that.getName().accept(this);
        
    	// Should be impossible
    	//List<TypeCheckerResult> staticParams_result = recurOnListOfStaticParam(that.getStaticParams());
        
        Option<TypeCheckerResult> returnType_result = recurOnOptionOfType(that.getReturnType());
        
        //TypeCheckerResult where_result = that.getWhere().accept(this);
        
        List<TypeCheckerResult> all_results = new ArrayList<TypeCheckerResult>();
        
        Option<List<TypeCheckerResult>> throwsClause_result = recurOnOptionOfListOfBaseType(that.getThrowsClause());
        
        
        List<TypeCheckerResult> params_result = recurOnListOfParam(that.getParams());
    	// Grab bindings and introduce them. For the time-being, they must have types.        
        TypeChecker extended_checker = this;
        for( Param p : that.getParams() ) {
        	extended_checker = extended_checker.extend(p);
        }
        TypeCheckerResult body_result = that.getBody().accept(extended_checker);

        // Get all results together
        all_results.addAll(params_result);
        all_results.add(body_result);
        if( returnType_result.isSome() )
        	all_results.add(returnType_result.unwrap());
        if( throwsClause_result.isSome() )
        	all_results.addAll(throwsClause_result.unwrap());
        
        // If return type is given, we check that it is a supertype of the inferred super-type
        // and we use it, otherwise the return type is what we infer.
        Type return_type;
        if( body_result.type().isNone() ) {
        	// We've got errors in the body
        	return_type = Types.BOTTOM;
        }
        else if( that.getReturnType().isSome() ) {
        	return_type = that.getReturnType().unwrap();
        	TypeCheckerResult subtype_result = 
        		this.checkSubtype(body_result.type().unwrap(), return_type, that.getBody(),
        				"Type of body of Fn expression must be a subtype of declared return type ("+
        				return_type +") but is " + body_result.type().unwrap() +".");
        	all_results.add(subtype_result);
        }
        else {
        	return_type = body_result.type().unwrap();
        }
        
        // All throws types must be a subtype of exception
        if( that.getThrowsClause().isSome() ) {
        	for( BaseType exn : that.getThrowsClause().unwrap() ) {
        		all_results.add(this.checkSubtype(exn, Types.EXCEPTION, that,
        				"Types in throws clause must be subtypes of Exception, but "+
        				exn + " is not."));
        	}
        }
        return TypeCheckerResult.compose(that, return_type, this.subtypeChecker, all_results);
	}

	/** Ignore unsupported nodes for now. */
    /*public TypeCheckerResult defaultCase(Node that) {
        return new TypeCheckerResult(that, Types.VOID, IterUtil.<TypeError>empty());
    }*/

    public TypeCheckerResult forFnDef(FnDef that) {
        TypeChecker newChecker = this.extend(that.getStaticParams(), that.getParams(), that.getWhere());

        TypeCheckerResult contractResult = that.getContract().accept(newChecker);
        TypeCheckerResult bodyResult = that.getBody().accept(newChecker);
        TypeCheckerResult result = new TypeCheckerResult(that);

        Option<Type> returnType = that.getReturnType();
        if (bodyResult.type().isSome()) {
            Type bodyType = bodyResult.type().unwrap();
            if (returnType.isNone()) {
                returnType = wrap(bodyType);
            }

            result = checkSubtype(bodyType,
                                  returnType.unwrap(),
                                  that,
                                  errorMsg("Function body has type ", bodyType, ", but ",
                                           "declared return type is ", returnType.unwrap()));
        }

        return TypeCheckerResult.compose(new FnDef(that.getSpan(),
                                               that.getMods(),
                                               that.getName(),
                                               that.getStaticParams(),
                                               that.getParams(),
                                               returnType,
                                               that.getThrowsClause(),
                                               that.getWhere(),
                                               (Contract)contractResult.ast(),
                                               that.getSelfName(),
                                               (Expr)bodyResult.ast()),
                                     subtypeChecker, contractResult, bodyResult, result);
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
                // System.err.println("varType: " + varType.unwrap());
                // System.err.println("initResult.type(): " + initResult.type());
                if (initResult.type().isNone()) {
                    // System.err.println("initResult.ast(): " + initResult.ast());
                    // The right hand side could not be typed, which must have resulted in a
                    // signaled error. No need to signal another error.
                    return TypeCheckerResult.compose(new VarDecl(that.getSpan(),
                                                                 lhs,
                                                                 (Expr)initResult.ast()),
                                                     subtypeChecker, initResult);
                }
                return checkSubtype(initResult.type().unwrap(),
                                    varType.unwrap(),
                                    that,
                                    errorMsg("Attempt to define variable ", var, " ",
                                             "with an expression of type ", initResult.type().unwrap()));
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
                                                 subtypeChecker, initResult);
            }
            return checkSubtype(initResult.type().unwrap(),
                                varType,
                                that,
                                errorMsg("Attempt to define variables ", lhs, " ",
                                         "with an expression of type ", initResult.type().unwrap()));
        }
    }
    

    
    public TypeCheckerResult forId(Id name) {
        Option<APIName> apiName = name.getApi();
        if (apiName.isSome()) {
            APIName api = apiName.unwrap();
            TypeEnv apiTypeEnv;
            if (compilationUnit.ast().getName().equals(api)) {
                apiTypeEnv = typeEnv;
            } else {
                apiTypeEnv = TypeEnv.make(table.compilationUnit(api));
            }

            Option<Type> type = apiTypeEnv.type(unqualifiedIdFromId(name));
            if (type.isSome()) {
                Type _type = type.unwrap();
                if (_type instanceof NamedType) { // Do we need to qualify?
                    NamedType _namedType = (NamedType)_type;

                    // Type was declared in that API, so it's not qualified;
                    // prepend it with the API.
                    if (_namedType.getName().getApi().isNone()) {
                        _type = NodeFactory.makeNamedType(api, (NamedType) type.unwrap());
                    }
                }
                return new TypeCheckerResult(name, _type);
            } else {
                // Operators are never qualified in source code, so if 'name' is qualified and not
                // found, it must be a Id, not a OpName.
                StaticError error = TypeError.make(errorMsg("Attempt to reference unbound variable: ", name),
                                                   name);
                return new TypeCheckerResult(name, error);
            }
        }
        Option<Type> type = typeEnv.type(name);
        if (type.isSome()) {
            Type _type = type.unwrap();
            if (_type instanceof LabelType) { // then name must be an Id
                return new TypeCheckerResult(name, makeLabelNameError((Id)name));
            } else {
                return new TypeCheckerResult(name, _type);
            }
        } else {
            StaticError error;
            error = TypeError.make(errorMsg("Variable '", name, "' not found."),
                                   name);
            return new TypeCheckerResult(name, error);
        }
    }

    public TypeCheckerResult forAPIName(APIName that) {
        return new TypeCheckerResult(that);
    }

    public TypeCheckerResult forExportOnly(Export that, List<TypeCheckerResult> apis_result) {
        return new TypeCheckerResult(that);
    }

    private TypeCheckerResult forIdOrOpOrAnonymousName(IdOrOpOrAnonymousName that) {
        Option<APIName> apiName = that.getApi();
        if (apiName.isSome()) {
            APIName api = apiName.unwrap();
            TypeEnv apiTypeEnv;
            if (compilationUnit.ast().getName().equals(api)) {
                apiTypeEnv = typeEnv;
            } else {
                apiTypeEnv = TypeEnv.make(table.compilationUnit(api));
            }

            Option<Type> type = apiTypeEnv.type(that);
            if (type.isSome()) {
                Type _type = type.unwrap();
                if (_type instanceof NamedType) { // Do we need to qualify?
                    NamedType _namedType = (NamedType)_type;

                    // Type was declared in that API, so it's not qualified;
                    // prepend it with the API.
                    if (_namedType.getName().getApi().isNone()) {
                        _type = NodeFactory.makeNamedType(api, (NamedType) type.unwrap());
                    }
                }
                return new TypeCheckerResult(that, _type);
            } else {
                // Operators are never qualified in source code, so if 'that' is qualified and not
                // found, it must be a Id, not a OpName.
                StaticError error = TypeError.make(errorMsg("Attempt to reference unbound variable: ", that),
                                                   that);
                return new TypeCheckerResult(that, error);
            }
        }
        Option<Type> type = typeEnv.type(that);
        if (type.isSome()) {
            Type _type = type.unwrap();
            if (_type instanceof LabelType) { // then name must be an Id
                return new TypeCheckerResult(that, makeLabelNameError((Id)that));
            } else {
                return new TypeCheckerResult(that, _type);
            }
        } else {
            StaticError error;
            if (that instanceof Id) {
                error = TypeError.make(errorMsg("Variable '", that, "' not found."),
                                       that);
            } else if (that instanceof Op) {
                error = TypeError.make(errorMsg("Operator '", OprUtil.decorateOperator((Op)that),
                                                "' not found."),
                                       that);
            } else { // must be Enclosing
                error = TypeError.make(errorMsg("Enclosing operator '", (Enclosing)that, "' not found."),
                                       that);
            }
            return new TypeCheckerResult(that, error);
        }
    }
    
	
	

	
	public TypeCheckerResult forVarRefOnly(VarRef that, TypeCheckerResult var_result) {
        Option<Type> varType = var_result.type();
        if (varType.isSome()) {
            return TypeCheckerResult.compose(that, varType.unwrap(), subtypeChecker, var_result);
        } else {
            return TypeCheckerResult.compose(that, subtypeChecker, var_result);
        }
    }

	private static TypeChecker addSelf(Id name, TypeChecker newChecker, List<StaticParam> static_params){
    	TraitType self_type = NodeFactory.makeTraitType(name,TypeEnv.staticParamsToArgs(static_params));
    	return newChecker.extend(Collections.singletonList(NodeFactory.makeLValue("self", self_type)));
    }
	
    public TypeCheckerResult forObjectDecl(ObjectDecl that) {
        TypeCheckerResult modsResult = TypeCheckerResult.compose(that, subtypeChecker, recurOnListOfModifier(that.getMods()));
        TypeCheckerResult nameResult = that.getName().accept(this);
        TypeCheckerResult staticParamsResult = TypeCheckerResult.compose(that, subtypeChecker, recurOnListOfStaticParam(that.getStaticParams()));
        TypeCheckerResult extendsClauseResult = TypeCheckerResult.compose(that, subtypeChecker, recurOnListOfTraitTypeWhere(that.getExtendsClause()));
        TypeCheckerResult whereResult = that.getWhere().accept(this);
        TypeCheckerResult paramsResult = TypeCheckerResult.compose(that, subtypeChecker, recurOnOptionOfListOfParam(that.getParams()));
        TypeCheckerResult throwsClauseResult = TypeCheckerResult.compose(that, subtypeChecker, recurOnOptionOfListOfBaseType(that.getThrowsClause()));

        TypeChecker newChecker = this.extend(that.getStaticParams(), that.getParams(), that.getWhere());
        TypeCheckerResult contractResult = that.getContract().accept(newChecker);

        // Check field declarations.
        TypeCheckerResult fieldsResult = new TypeCheckerResult(that);
        for (Decl decl: that.getDecls()) {
            if (decl instanceof VarDecl) {
                VarDecl _decl = (VarDecl)decl;
                fieldsResult = TypeCheckerResult.compose(that, subtypeChecker, _decl.accept(newChecker), fieldsResult);
                newChecker = newChecker.extend(_decl.getLhs());
            }
        }

        // Check method declarations.

        TraitIndex thatIndex = (TraitIndex)table.typeCons(that.getName());
        newChecker = newChecker.extendWithMethods(thatIndex.dottedMethods());
        newChecker = newChecker.extendWithFunctions(thatIndex.functionalMethods());

        // Extend checker with self
        
        newChecker = TypeChecker.addSelf(that.getName(),newChecker,thatIndex.staticParameters());

        
        TypeCheckerResult methodsResult = new TypeCheckerResult(that);
        for (Decl decl: that.getDecls()) {
            if (decl instanceof FnDecl) {
                methodsResult = TypeCheckerResult.compose(that, subtypeChecker, decl.accept(newChecker), methodsResult);
            }
        }

        return TypeCheckerResult.compose(that, subtypeChecker, modsResult, nameResult,
                                         staticParamsResult, extendsClauseResult, whereResult, paramsResult,
                                         throwsClauseResult, contractResult, fieldsResult, methodsResult);
    }

    public TypeCheckerResult forTraitDecl(TraitDecl that) {
        TypeCheckerResult modsResult = TypeCheckerResult.compose(that, subtypeChecker, recurOnListOfModifier(that.getMods()));
        TypeCheckerResult staticParamsResult = TypeCheckerResult.compose(that, subtypeChecker, recurOnListOfStaticParam(that.getStaticParams()));
        TypeCheckerResult extendsClauseResult = TypeCheckerResult.compose(that, subtypeChecker, recurOnListOfTraitTypeWhere(that.getExtendsClause()));
        TypeCheckerResult whereResult = that.getWhere().accept(this);
        TypeCheckerResult excludesResult = TypeCheckerResult.compose(that, subtypeChecker, recurOnListOfBaseType(that.getExcludes()));

        TypeCheckerResult comprisesResult = new TypeCheckerResult(that);
        Option<List<BaseType>> comprises = that.getComprises();
        if (comprises.isSome()) {
            comprisesResult =
                TypeCheckerResult.compose
                    (that, subtypeChecker, recurOnOptionOfListOfBaseType(that.getComprises()).unwrap());
        }

        TypeChecker newChecker = this.extend(that.getStaticParams(), that.getWhere());

        // Check "field" declarations (really getter and setter declarations).
        TypeCheckerResult fieldsResult = new TypeCheckerResult(that);
        for (Decl decl: that.getDecls()) {
            if (decl instanceof VarDecl) {
                VarDecl _decl = (VarDecl)decl;

                fieldsResult = TypeCheckerResult.compose(that, subtypeChecker, _decl.accept(newChecker), fieldsResult);
                newChecker = newChecker.extend(_decl.getLhs());
            }
        }

        // Check method declarations.

        TraitIndex thatIndex = (TraitIndex)table.typeCons(that.getName());
        newChecker = newChecker.extendWithMethods(thatIndex.dottedMethods());
        newChecker = newChecker.extendWithFunctions(thatIndex.functionalMethods());
        //add self param
        newChecker = TypeChecker.addSelf(that.getName(),newChecker,thatIndex.staticParameters());
        
        TypeCheckerResult methodsResult = new TypeCheckerResult(that);
        for (Decl decl: that.getDecls()) {
            if (decl instanceof FnDecl) {
                methodsResult = TypeCheckerResult.compose(that, subtypeChecker, decl.accept(newChecker), methodsResult);
            }
        }

        return TypeCheckerResult.compose(that, subtypeChecker, modsResult,
                                         staticParamsResult, extendsClauseResult, whereResult, excludesResult,
                                         comprisesResult, fieldsResult, methodsResult);
    }

    
    
    public TypeCheckerResult forIfOnly(If that,
                                       List<TypeCheckerResult> clauses_result,
                                       Option<TypeCheckerResult> elseClause_result) {

        if (elseClause_result.isSome()) {
            List<Type> clauseTypes = new ArrayList<Type>();
            TypeCheckerResult elseResult = elseClause_result.unwrap();

            // Get union of all clauses' types
            for (TypeCheckerResult clauseResult : clauses_result) {
                if (clauseResult.type().isSome()) {
                    clauseTypes.add(clauseResult.type().unwrap());
                }
            }
            if (elseResult.type().isSome()) {
                clauseTypes.add(elseResult.type().unwrap());
            }
            return TypeCheckerResult.compose(that,
                                             subtypeChecker.join(clauseTypes),
                                             subtypeChecker,
                                             TypeCheckerResult.compose(that, subtypeChecker, clauses_result), TypeCheckerResult.compose(that, subtypeChecker, elseResult));
        } else {
            // Check that each if/elif clause has void type
            TypeCheckerResult result = new TypeCheckerResult(that);
            for (TypeCheckerResult clauseResult : clauses_result) {
                if (clauseResult.type().isSome()) {
                    Type clauseType = clauseResult.type().unwrap();
                    result = TypeCheckerResult.compose(
                        that,
                        subtypeChecker,
                        result, checkSubtype(clauseType,
                                     Types.VOID,
                                     that,
                                     errorMsg("An 'if' clause without corresponding 'else' has type ",
                                              clauseType, " instead of type ()")));
                }
            }
            return TypeCheckerResult.compose(that,
                                             Types.VOID,
                                             subtypeChecker,
                                             TypeCheckerResult.compose(that, subtypeChecker, clauses_result), result);
        }
    }

    @Override
    public TypeCheckerResult forIfClause(IfClause that) {
    	// For generalized 'if' we must introduce new bindings.
    	Pair<TypeCheckerResult, List<LValueBind>> result_and_binds =
    		this.forGeneratorClauseGetBindings(that.getTest(), true);
    	
    	// Destruct result
    	TypeCheckerResult test_result = result_and_binds.first();
    	List<LValueBind> bindings = result_and_binds.second();
    	
    	// Check body with new bindings
    	TypeChecker tc_with_new_bindings = this.extend(bindings);
    	TypeCheckerResult body_result = that.getBody().accept(tc_with_new_bindings);
        return forIfClauseOnly(that, test_result, body_result);
    }
    
    public TypeCheckerResult forIfClauseOnly(IfClause that,
                                             TypeCheckerResult test_result,
                                             TypeCheckerResult body_result) {

        TypeCheckerResult result = new TypeCheckerResult(that);

        // Check that test condition is Boolean.
        if (test_result.type().isSome()) {
            Type testType = test_result.type().unwrap();
            result = TypeCheckerResult.compose(
                that,
                subtypeChecker,
                checkSubtype(testType,
                             Types.BOOLEAN,
                             that,
                             errorMsg("Attempt to use expression of type ", testType, " ",
                                      "as a test condition")), result);
        }

        // IfClause's type is body's type.
        if (body_result.type().isSome()) {
            return TypeCheckerResult.compose(that, 
            		body_result.type().unwrap(), subtypeChecker, test_result, body_result, result);
        } else {
            return TypeCheckerResult.compose(that, subtypeChecker, test_result, body_result, result);
        }
    }

    private Pair<TypeCheckerResult, List<LValueBind>> forGeneratorClauseGetBindings(GeneratorClause that, 
    		                                                                        boolean mustBeCondition) {
        // We just don't visit the Ids at all, and let a different pass handle shadowing    	
        TypeCheckerResult init_result = that.getInit().accept(this);
        return forGeneratorClauseOnlyGetBindings(that, init_result, mustBeCondition);
    }
    
    /**
     * Returns a type checker result and the a Type that is the type of the
     * generator. The given type is checked to be a sub-type of
     * Generator[\T\] where T is an inference variable, and the inferred type
     * T is returned.
     */
    private Pair<TypeCheckerResult, Type> getGeneratorType(Type sub, Node ast, String error) {
    	Type infer_type = NodeFactory.makeInferenceVarType();
    	Type generator_type = Types.makeGeneratorType(infer_type);
    	TypeCheckerResult result = this.checkSubtype(sub, generator_type, ast, error);
    	
    	return Pair.make( result, infer_type );
    }
    
    /**
     * Returns a type checker result and the a Type that is the type of the
     * condition. The given type is checked to be a sub-type of
     * Condition[\T\] where T is an inference variable, and the inferred type
     * T is returned.
     */
    private Pair<TypeCheckerResult, Type> getConditionType(Type sub, Node ast, String error) {
    	Type infer_type = NodeFactory.makeInferenceVarType();
    	Type generator_type = Types.makeConditionType(infer_type);
    	TypeCheckerResult result = this.checkSubtype(sub, generator_type, ast, error);
    	
    	return Pair.make( result, infer_type );
    }
    
    private Pair<TypeCheckerResult, List<LValueBind>> forGeneratorClauseOnlyGetBindings(GeneratorClause that,
            TypeCheckerResult init_result, boolean mustBeCondition) {
    	
    	if( init_result.type().isNone() ) {
    		// Subexpr failed, we can go no further
    		return Pair.make(TypeCheckerResult.compose(that, subtypeChecker, init_result), 
    				Collections.<LValueBind>emptyList());
    	}
    	
    	if( that.getBind().isEmpty() ) {
        	// If bindings are empty, then init_result must be of type boolean, a filter, 13.14
    		TypeCheckerResult bool_result =
    		this.checkSubtype(init_result.type().unwrap(), Types.BOOLEAN, that.getInit(), 
    				"Filter expressions in generator clauses must have type boolean, but " +
    				that.getInit() + " had type " + init_result.type().unwrap() + ".");
    		
    		return Pair.make(TypeCheckerResult.compose(that, subtypeChecker, init_result, bool_result),
    				Collections.<LValueBind>emptyList());
    	}
    	
    	// Otherwise, we may actually have bindings!
    	Type init_type = init_result.type().unwrap();
		String type_err_msg = "Init expression of generator must be a sub-type of " +
			(mustBeCondition ? "Condition" : "Generator") +
			" but is type " + init_result.type().unwrap() + ".";
    	// Get the type of the Generator
		Pair<TypeCheckerResult, Type> generator_pair = 
			mustBeCondition ? this.getConditionType(init_type, that.getInit(), type_err_msg) :
				 	          this.getGeneratorType(init_type, that.getInit(), type_err_msg);
		
		Type generator_type = generator_pair.second();
		TypeCheckerResult this_result = generator_pair.first();
		int bindings_count = that.getBind().size();
		
		List<LValueBind> result_bindings;
		// Now create the bindings
		if( bindings_count == 1 ){
    		// Just one binding
    		LValueBind lval = NodeFactory.makeLValue(that.getBind().get(0), generator_type);
    		result_bindings = Collections.singletonList(lval);
    	}
    	else {
    		// Because generator_type is almost certainly a InferenceVar, we have to declare a new tuple
    		// that is the size of the binding and declare one to be a sub-type of the other.
    		List<Type> inference_vars = new ArrayList<Type>(bindings_count);
    		for( int i = 0; i<bindings_count;i++ ) {
    			inference_vars.add(NodeFactory.makeInferenceVarType());
    		}
    		// Assert that this new tuple type is a subtype of the generator type
    		String tup_err_msg = "If more than one variable is bound in a generator, generator must have tuple type "+
    			"but " + that.getInit() + " does not or has different number of arguments.";
    		TypeCheckerResult tuple_result = 
    			this.checkSubtype(Types.makeTuple(inference_vars), generator_type, that.getInit(), tup_err_msg);
    		this_result = TypeCheckerResult.compose(that, subtypeChecker, tuple_result, this_result);
    		// Now just create the lvalues with the newly created inference variable type
    		Iterator<Id> id_iter = that.getBind().iterator();
    		result_bindings = new ArrayList<LValueBind>(bindings_count);
    		for( Type inference_var : inference_vars ) {
    			result_bindings.add(NodeFactory.makeLValue(id_iter.next(), inference_var));
    		}	
    	}
    	return Pair.make(TypeCheckerResult.compose(that, subtypeChecker, this_result, init_result), result_bindings);
    }
    
    // In the end, this whole method may be pointless. We probably want a method that will
    // return the bindings so that other methods can actually use this.
    public TypeCheckerResult forGeneratorClauseOnly(GeneratorClause that,
                                                    List<TypeCheckerResult> bind_result,
                                                    TypeCheckerResult init_result) {
    	
    	List<TypeCheckerResult> all_results = new ArrayList<TypeCheckerResult>(bind_result.size()+2);
    	all_results.addAll(bind_result);
    	all_results.add(init_result);
    	
    	if( init_result.type().isNone() ) {
    		// Subexpr failed, we can go no further
    		return TypeCheckerResult.compose(that, subtypeChecker, all_results);
    	}
    	
    	if( bind_result.isEmpty() ) {
        	// If bindings are empty, then init_result must be of type boolean, a filter, 13.14
    		TypeCheckerResult bool_result =
    		this.checkSubtype(init_result.type().unwrap(), Types.BOOLEAN, that.getInit(), 
    				"Filter expressions in generator clauses must have type boolean, but " +
    				that.getInit() + " had type " + init_result.type().unwrap() + ".");
    		
    		all_results.add(bool_result);
    		return TypeCheckerResult.compose(that, subtypeChecker, all_results);
    	}
    	
    	// Otherwise, init expr must have type generator, 13.14
    	// I don't think that passing 'any' to make generator type will work...
		TypeCheckerResult gen_result =
    		this.checkSubtype(init_result.type().unwrap(), Types.makeGeneratorType(Types.ANY), that.getInit(), 
    				"Generator initializers must have type Generator, but " +
    				that.getInit() + " had type " + init_result.type().unwrap() + ".");
    		
    	all_results.add(gen_result);
    	return TypeCheckerResult.compose(that, subtypeChecker, all_results);
    }

    public TypeCheckerResult forDoOnly(Do that, List<TypeCheckerResult> fronts_result) {
        // Get union of all clauses' types
        List<Type> frontTypes = new ArrayList<Type>();
        for (TypeCheckerResult frontResult : fronts_result) {
            if (frontResult.type().isSome()) {
                frontTypes.add(frontResult.type().unwrap());
            }
        }
        return TypeCheckerResult.compose(that, subtypeChecker.join(frontTypes), subtypeChecker, fronts_result);
    }

    public TypeCheckerResult forDoFront(DoFront that) {
        TypeCheckerResult bodyResult =
            that.isAtomic() ? forAtomic(that,
                                        that.getExpr(),
                                        errorMsg("A 'spawn' expression must not occur inside",
                                                 "an 'atomic' do block."))
                            : that.getExpr().accept(this);
        TypeCheckerResult result = new TypeCheckerResult(that);
        if (that.getLoc().isSome()) {
            Expr loc = that.getLoc().unwrap();
            result = loc.accept(this);
            if (result.type().isSome()) {
                Type locType = result.type().unwrap();
                result = TypeCheckerResult.compose(that,
                                                   subtypeChecker,
                                                   result, checkSubtype(locType,
                                                                Types.REGION,
                                                                loc,
                                                                errorMsg("Location of 'do' block must ",
                                                                         "have type Region: ", locType)));
            }
        }
        return TypeCheckerResult.compose(that, bodyResult.type(), subtypeChecker, bodyResult, result);
    }

    public TypeCheckerResult forFnRefOnly(FnRef that,
                                          List<TypeCheckerResult> fns_result,
                                          List<TypeCheckerResult> staticArgs_result) {

        // Get intersection of overloaded function types.
        LinkedList<Type> overloadedTypes = new LinkedList<Type>();
        for (TypeCheckerResult fn_result : fns_result) {
            if (fn_result.type().isSome()) {
              overloadedTypes.add(fn_result.type().unwrap());
            }
        }
        Option<Type> type = (overloadedTypes.isEmpty()) ?
                                Option.<Type>none() :
                                Option.<Type>some(new IntersectionType(overloadedTypes));

        return TypeCheckerResult.compose(that,
                                         type,
                                         subtypeChecker,
                                         TypeCheckerResult.compose(that, subtypeChecker, fns_result), TypeCheckerResult.compose(that, subtypeChecker, staticArgs_result));
    }

    
    
    @Override
    public TypeCheckerResult forFor(For that) {
        Pair<List<TypeCheckerResult>,List<LValueBind>> pair = recurOnListsOfGeneratorClauseBindings(that.getGens());
        TypeChecker extend = this.extend(pair.second());
        TypeCheckerResult body_result = that.getBody().accept(extend);
        return forForOnly(that, pair.first(), body_result);
    }
 
   private Pair<List<TypeCheckerResult>, List<LValueBind>> recurOnListsOfGeneratorClauseBindings(List<GeneratorClause> gens){
	   GeneratorClause gen = gens.get(0);
	   Pair<TypeCheckerResult,List<LValueBind>> pair = forGeneratorClauseGetBindings(gen, false);
	   if(gens.size()>1){
		   TypeChecker extend = this.extend(pair.second());
		   Pair<List<TypeCheckerResult>,List<LValueBind>> pair2 =extend.recurOnListsOfGeneratorClauseBindings(gens.subList(1, gens.size()-1));
		   pair2.first().add(0,pair.first());
		   pair2.second().addAll(0,pair.second());
		   return pair2;
	   } else
		   return Pair.make(Collections.singletonList(pair.first()), pair.second());
	   
   }
    
    @Override
    public TypeCheckerResult forForOnly(For that, List<TypeCheckerResult> gens_result, TypeCheckerResult body_result) {
    	
    	List<TypeCheckerResult> all_results = new ArrayList<TypeCheckerResult>(gens_result);
		all_results.add(body_result);
    	if(body_result.type().isNone()){
			return TypeCheckerResult.compose(that,Types.VOID, subtypeChecker, all_results);
		}
		
		if( !body_result.type().unwrap().equals(Types.VOID) ) {
			all_results.add(new TypeCheckerResult(that, 
					TypeError.make("Body of while loop must have type (), but had type " +
							body_result.type().unwrap(), that.getBody())));
		}
		
		return TypeCheckerResult.compose(that,Types.VOID, subtypeChecker, all_results);
    }
    
    public TypeCheckerResult forOpRefOnly(OpRef that,
                                          List<TypeCheckerResult> ops_result,
                                          List<TypeCheckerResult> staticArgs_result) {

        // Get intersection of overloaded operator types.
        LinkedList<Type> overloadedTypes = new LinkedList<Type>();
        for (TypeCheckerResult op_result : ops_result) {
            if (op_result.type().isSome()) {
              overloadedTypes.add(op_result.type().unwrap());
            }
        }
        Option<Type> type = (overloadedTypes.isEmpty()) ?
                                Option.<Type>none() :
                                Option.<Type>some(new IntersectionType(overloadedTypes));
        return TypeCheckerResult.compose(that,
                                         type,
                                         subtypeChecker,
                                         TypeCheckerResult.compose(that, subtypeChecker, ops_result), TypeCheckerResult.compose(that, subtypeChecker, staticArgs_result));
    }

    public TypeCheckerResult forBlockOnly(Block that, List<TypeCheckerResult> exprs_result) {
    	// Type is type of last expression or void if none.
        if (exprs_result.isEmpty()) {
            return TypeCheckerResult.compose(that, Types.VOID, subtypeChecker, exprs_result);
        } else {
        	List<TypeCheckerResult> all_results = new ArrayList<TypeCheckerResult>(2*exprs_result.size()-1);
        	all_results.addAll(exprs_result);
        	// every other expression except for the last must be void
        	String void_err = "All expressions except the last in a block must have VOID type.";
        	for( int i=0; i<exprs_result.size()-1; i++ ) {
        		TypeCheckerResult r_i = exprs_result.get(i);
        		if( r_i.type().isSome() ) {
        			all_results.add(this.checkSubtype(r_i.type().unwrap(), 
        					Types.VOID, that.getExprs().get(i), void_err));
        		}
        	}        	
            return TypeCheckerResult.compose(that, exprs_result.get(exprs_result.size()-1).type(), subtypeChecker, all_results);
        }
    }

    public TypeCheckerResult forLetFn(LetFn that) {
        TypeCheckerResult result = new TypeCheckerResult(that);
        Relation<IdOrOpOrAnonymousName, FnDef> fnDefs = new HashRelation<IdOrOpOrAnonymousName, FnDef>(true, false);
        for (FnDef fnDef : that.getFns()) {
            fnDefs.add(fnDef.getName(), fnDef);
        }

        TypeChecker newChecker = this.extendWithFnDefs(fnDefs);
        for (FnDef fnDef : that.getFns()) {
            result = TypeCheckerResult.compose(that, subtypeChecker, result, fnDef.accept(newChecker));
        }
        result = TypeCheckerResult.compose(that,
                                           subtypeChecker,
                                           result, TypeCheckerResult.compose(that,
                                                                     subtypeChecker, newChecker.recurOnListOfExpr(that.getBody())));
        return result;
    }

    /**
     * For method calls, return a constraint formula matching the parameters to 
     * the type of the argument. Assumes that this is being used for a method call,
     * because it makes some assumptions about the type of the argument.
     */
	private ConstraintFormula argsMatchParams(List<Param> params, Type arg_type) {
		final int arg_size;
		if( arg_type instanceof TupleType )
			arg_size = ((TupleType)arg_type).getElements().size();
		else if( arg_type instanceof VoidType )
			arg_size = 0;
		else
			arg_size = 1;
		
		//handle domain
		Type domain_type;
    	if(!params.isEmpty()) {
    		
    		// Regular parameters & var args
    		List<Type> param_types =
    		IterUtil.fold(params, new LinkedList<Type>(), new Lambda2<LinkedList<Type>,Param,LinkedList<Type>>(){
				// How many types have we accumulated thus far?
				int typeCount = 0;
    			
    			public LinkedList<Type> value(LinkedList<Type> arg0, Param arg1) {
    				if( arg1 instanceof NormalParam ) {
    					typeCount++;
    					arg0.add(((NormalParam)arg1).getType().unwrap());
    					return arg0;
    				}
    				else { // VarargParam, add until the sizes are equal
    					int to_add = arg_size - typeCount;
    					while( to_add > 0 ) { 
    					  arg0.add(((VarargsParam)arg1).getType());
    					  to_add--;
    					}
    					return arg0;
    				}
				}});
			
			//handle defaults (nyi)
    		if(params.get(params.size()-1) instanceof NormalParam
    			&& ((NormalParam)params.get(params.size()-1)).getDefaultExpr().isSome()){
    			return NI.nyi();
    		}
			
			domain_type = NodeFactory.makeTupleType(param_types);
    	}
    	else {
			//is void
	    	domain_type = Types.VOID;
    	}
    	return this.subtypeChecker.subtype(arg_type, domain_type);
    }
		
	
	@Override
	public TypeCheckerResult forFieldRefOnly(FieldRef that,
			TypeCheckerResult obj_result, TypeCheckerResult field_result) {
		
		List<TypeCheckerResult> all_results = new ArrayList<TypeCheckerResult>(3);
    	all_results.add(obj_result);
    	// We need the type of the receiver
    	if( obj_result.type().isNone() ) {
    		return TypeCheckerResult.compose(that, subtypeChecker, all_results);
    	}
    	//check whether receiver can have fields
    	Type recvr_type=obj_result.type().unwrap();
    	Option<TraitIndex> is_trait=getIndexOfType(recvr_type);
    	if(is_trait.isSome()){
    		TraitIndex trait_index=is_trait.unwrap();
    		// check if trait has a getter
    		Map<Id,Method> getters=trait_index.getters();
    		if(getters.containsKey(that.getField())){
    			Method field=getters.get(that.getField());
    			return TypeCheckerResult.compose(that,field.getReturnType(),this.subtypeChecker,all_results);
    		}
    		else{
    			//check if trait is an object
    			if(trait_index instanceof ObjectTraitIndex){
    				//Check if object has field
    				ObjectTraitIndex object_index=(ObjectTraitIndex)trait_index;
    				Map<Id,Variable> fields=object_index.fields();
    				if(fields.containsKey(that.getField())){
    					Variable field=fields.get(that.getField());
    					Option<BindingLookup> type=this.typeEnv.binding(that.getField());
    					return TypeCheckerResult.compose(that,type.unwrap().getType().unwrap(),this.subtypeChecker,all_results);
    				}
    			}
    			//error no such field
    		}
    		//error receiver not a trait
    	}
		return NI.nyi();
	}
	
	private Option<TraitIndex> getIndexOfType(Type rcvr_type){
    	if( rcvr_type instanceof NamedType ) {
    		NamedType named_rcvr_type = (NamedType)rcvr_type;
    		TypeConsIndex index = table.typeCons(named_rcvr_type.getName());
    		if( index instanceof TraitIndex ) {
    			return Option.some((TraitIndex)index);
    		}
    	}
    	return Option.none();
	}
    
	private Pair<List<Method>,List<TypeCheckerResult>> findMethodsInTraitHierarchy(List<Type> supers, Type arg_result, MethodInvocation that){
		Id method_name = that.getMethod();
		List<TypeCheckerResult> all_results= new ArrayList<TypeCheckerResult>();
		List<Method> candidates=new ArrayList<Method>();
		List<Type> new_supers=new ArrayList<Type>();
		for(Type type: supers){
			Option<TraitIndex> is_trait=getIndexOfType(type);
			if(is_trait.isSome()){
				TraitIndex trait_index= is_trait.unwrap();
				List<Type> temp = IterUtil.asList(IterUtil.map(trait_index.extendsTypes(),
					new Lambda<TraitTypeWhere,Type>(){

						public Type value(TraitTypeWhere arg0) {
							return arg0.getType();
						}}
				));
				new_supers.addAll(temp);
				Set<Method> methods_with_name = trait_index.dottedMethods().getSeconds(method_name);
				//get methods with the right name
				for( Method m : methods_with_name ) {
					List<StaticArg> static_args = new ArrayList<StaticArg>(that.getStaticArgs());
					// same number of static args
					if( m.staticParameters().size() > 0 && that.getStaticArgs().size() == 0 ) {
						// we need to infer static arguments
						List<StaticArg> static_inference_params =
							IterUtil.asList(
									IterUtil.map(m.staticParameters(), new Lambda<StaticParam,StaticArg>(){
										public StaticArg value(StaticParam arg0) {
											Type ivt = NodeFactory.makeInferenceVarType();
											return new TypeArg(ivt);
										}}
									));
						static_args = static_inference_params;
					}
					else if(m.staticParameters().size()!=static_args.size()) {
						// we don't need to infer, and they have different numbers of args
						continue;
					}
					// Same number of static parameters			
					// Do they have the same number of parameters, or at least can they be matched.
					// we know this cast works
					Method im = (Method)m.instantiate(static_args);
					ConstraintFormula mc = this.argsMatchParams(im.parameters(), arg_result);
					// constraint satisfiable?
					if(mc.isSatisfiable()) {
						//add method to candidates
						candidates.add(im);
						all_results.add(new TypeCheckerResult(that,Option.<Type>none(),mc));
					}
	    				
	    		}
			}
		}
		if(candidates.isEmpty() && !new_supers.isEmpty()){
			return findMethodsInTraitHierarchy(new_supers, arg_result,that);
		}
		return Pair.make(candidates,all_results);
	}
	
    @Override
	public TypeCheckerResult forMethodInvocationOnly(MethodInvocation that,
			TypeCheckerResult obj_result, TypeCheckerResult method_result,
			List<TypeCheckerResult> staticArgs_result,
			TypeCheckerResult arg_result) {
    	
    	List<TypeCheckerResult> all_results = new ArrayList<TypeCheckerResult>(staticArgs_result.size() + 3);
    	all_results.add(obj_result);
    	//all_results.add(method_result); Ignore! This will try to look up method in local context
    	all_results.addAll(staticArgs_result);
    	all_results.add(arg_result);
    	// Did the arguments typecheck?
    	if( arg_result.type().isNone() ) {
    		return TypeCheckerResult.compose(that, subtypeChecker, all_results);
    	}
    	// We need the type of the receiver
    	if( obj_result.type().isNone() ) {
    		return TypeCheckerResult.compose(that, subtypeChecker, all_results);
    	}
    	// Check whether receiver can have methods
    	Type recvr_type=obj_result.type().unwrap();
    	Option<TraitIndex> is_trait=getIndexOfType(recvr_type);
    	if(!is_trait.isSome()){
    		//error receiver not a trait
    		String trait_err = "Target of a method invocation must have trait type, while this receiver has type " + recvr_type + ".";
    		all_results.add(new TypeCheckerResult(that.getObj(), TypeError.make(trait_err, that.getObj())));
    		return TypeCheckerResult.compose(that, this.subtypeChecker, all_results);
    	}
    	else{
    		Pair<List<Method>,List<TypeCheckerResult>> candidate_pair = findMethodsInTraitHierarchy(Collections.singletonList(recvr_type),arg_result.type().unwrap(),that);
    		List<Method> candidates = candidate_pair.first();
    		all_results.addAll(candidate_pair.second());
			// Now we join together the results, or return an error if there are no candidates.
			if(candidates.isEmpty()){
				String err = "No candidate methods found for '" + that.getMethod() + "' with argument types (" + arg_result.type().unwrap() + ").";
				all_results.add(new TypeCheckerResult(that,TypeError.make(err,that)));
			}
			
			List<Type> ranges = IterUtil.asList(IterUtil.map(candidates, new Lambda<Method,Type>(){
				public Type value(Method arg0) {
					return arg0.getReturnType();
				}}));
			
			Type range = this.subtypeChecker.join(ranges);
			return TypeCheckerResult.compose(that,range,this.subtypeChecker,all_results);
    	}
	}

	public TypeCheckerResult forLocalVarDecl(LocalVarDecl that) {
        TypeCheckerResult result = new TypeCheckerResult(that);
        if (that.getRhs().isSome()) {
            result = TypeCheckerResult.compose(that, subtypeChecker, result, that.getRhs().unwrap().accept(this));
        }
        TypeChecker newChecker = this.extend(that);
        
        // A LocalVarDecl is like a let. It has a body, and it's type is the type of the body
        List<TypeCheckerResult> body_results = newChecker.recurOnListOfExpr(that.getBody());
        Option<Type> body_type = that.getBody().size() == 0 ? 
        		Option.<Type>some(Types.VOID) :
        	    body_results.get(body_results.size()-1).type();
        
        return TypeCheckerResult.compose(that,
        		                         body_type,
                                         subtypeChecker,
                                         result, TypeCheckerResult.compose(that,
                                                                   subtypeChecker, body_results));
    }

    public TypeCheckerResult forArgExprOnly(ArgExpr that,
                                            List<TypeCheckerResult> exprs_result,
                                            Option<TypeCheckerResult> varargs_result,
                                            List<TypeCheckerResult> keywords_result) {
        if (varargs_result.isSome()) {
            return TypeCheckerResult.compose(that,
                                             subtypeChecker,
                                             TypeCheckerResult.compose(that, subtypeChecker, exprs_result),
                                             TypeCheckerResult.compose(that, subtypeChecker, varargs_result.unwrap()), TypeCheckerResult.compose(that, subtypeChecker, keywords_result));
        } else {
            return TypeCheckerResult.compose(that,
                                             subtypeChecker,
                                             TypeCheckerResult.compose(that, subtypeChecker, exprs_result), TypeCheckerResult.compose(that, subtypeChecker, keywords_result));
        }
    }

    
   

	private TypeCheckerResult forTypeAnnotatedExprOnly(TypeAnnotatedExpr that,
                                                       TypeCheckerResult expr_result,
                                                       String errorMsg) {
        // Check that expression type <: annotated type.
        Type annotatedType = that.getType();
        if (expr_result.type().isSome()) {
            Type exprType = expr_result.type().unwrap();
            return TypeCheckerResult.compose(
                that,
                annotatedType,
                subtypeChecker,
                expr_result, checkSubtype(exprType,
                             annotatedType,
                             expr_result.ast(),
                             errorMsg));
        } else {
            return TypeCheckerResult.compose(that,
                                             annotatedType,
                                             subtypeChecker, expr_result);
        }
    }

    public TypeCheckerResult forAsExpr(AsExpr that) {
        Type ascriptedType = that.getType();
        TypeCheckerResult expr_result = that.getExpr().accept(this);
        Type exprType = expr_result.type().isSome() ? expr_result.type().unwrap() : Types.BOTTOM;
        return forTypeAnnotatedExprOnly(that,
                                        expr_result,
                                        errorMsg("Attempt to ascribe expression of type ",
                                                 exprType, " to non-supertype ", ascriptedType));
    }

    public TypeCheckerResult forAsIfExprOnly(AsIfExpr that) {
        Type assumedType = that.getType();
        TypeCheckerResult expr_result = that.getExpr().accept(this);
        Type exprType = expr_result.type().isSome() ? expr_result.type().unwrap() : Types.BOTTOM;
        return forTypeAnnotatedExprOnly(that,
                                        expr_result,
                                        errorMsg("Attempt to assume type ", assumedType,
                                                 " from non-subtype ", exprType));
    }


    /**
     * Returns the Id of the lhs variable or none for LHS that do not have Ids.
     */
    private static Option<? extends IdOrOpOrAnonymousName> getIdOfLHS(LHS lhs) {
    
    	Option<? extends IdOrOpOrAnonymousName> result =
    	lhs.accept(new NodeDepthFirstVisitor<Option<? extends IdOrOpOrAnonymousName>>(){
			@Override
			public Option<? extends IdOrOpOrAnonymousName> defaultCase(Node that) {
				assert(false);
				return Option.none();
			}
			@Override
			public Option<? extends IdOrOpOrAnonymousName> for_RewriteFieldRef(
					_RewriteFieldRef that) {
				// NEB: Pretty sure this case is wrong. Don't we need to look up
				// the api if it's different from this one?
	    		Name n = that.getField();
	    		assert(n instanceof IdOrOpOrAnonymousName);
	    		return Option.some(((IdOrOpOrAnonymousName)n));
			}
			@Override
			public Option<? extends IdOrOpOrAnonymousName> forFieldRef(
					FieldRef that) {
				return Option.some(that.getField());
			}
			@Override
			public Option<? extends IdOrOpOrAnonymousName> forFieldRefForSure(
					FieldRefForSure that) {
				return Option.some(that.getField());
			}
			@Override
			public Option<? extends IdOrOpOrAnonymousName> forLValueBind(
					LValueBind that) {
				return Option.some(that.getName());
			}
			@Override
			public Option<? extends IdOrOpOrAnonymousName> forSubscriptExpr(
					SubscriptExpr that) {
				return Option.none();
			}
			@Override
			public Option<? extends IdOrOpOrAnonymousName> forVarRef(VarRef that) {
				return Option.some(that.getVar());
			}    		
    	});
    	return result;
    }
    
    
    @Override
	public TypeCheckerResult forAssignmentOnly(Assignment that,
			                                   List<TypeCheckerResult> lhs_results,
			                                   Option<TypeCheckerResult> opr_result,
			                                   TypeCheckerResult rhs_result) {
    	// If LHS.size() > 1 then rhs must be a tuple and their types must match.
    	// LHS vars must have already been declared
    	// must be assignable
    	// if oper != :=, then do we need to look up the types it expects?
    	// result of the entire thing is not clear in spec, making it type of expr
    	
    	List<TypeCheckerResult> all_results = new ArrayList<TypeCheckerResult>(lhs_results.size()+2);
    	all_results.addAll(lhs_results);
    	if( opr_result.isSome() ) 
    		all_results.add(opr_result.unwrap());
    	all_results.add(rhs_result);

    	// assert that lhs variables are assignable
    	for( LHS lhs : that.getLhs() ) {
    		Option<? extends IdOrOpOrAnonymousName> id = getIdOfLHS(lhs);
    		if( id.isSome() ) {
    			Option<BindingLookup> binding = typeEnv.binding(id.unwrap());
    			if( binding.isSome() ) {
    				if( !binding.unwrap().isMutable() ) {
    					// THIS is an error. Must be declared mutable
    					TypeCheckerResult r = new TypeCheckerResult(lhs, TypeError.make("Left-hand side of assignment must be mutable, and " +
    							lhs + " is not.", lhs));
    					all_results.add(r);
    					//return TypeCheckerResult.compose(that, all_results);
    					return r;
    				}
    			}	
    		}
    	}
    	
    	if( rhs_result.type().isNone() ) {
    		// Typechecking of subexpr already failed...
    		return TypeCheckerResult.compose(that, subtypeChecker, all_results);
    	}
    	Type rhs_type = rhs_result.type().unwrap();
    	    	
    	// Construct type of LHS for comparison purposes
    	Type lhs_type; 
    	if( lhs_results.size() == 1 ) {
    		// single expression
    		TypeCheckerResult lhs_result = lhs_results.get(0);
    		if( lhs_result.type().isNone() ) {
        		// Typechecking of subexpr already failed...
        		return TypeCheckerResult.compose(that, subtypeChecker, all_results);    			
    		}
    		else {
    			lhs_type = lhs_result.type().unwrap();
    		}
    	}
    	else {
    		// tuple
    		List<Type> element_types = new ArrayList<Type>(lhs_results.size());
    		for( TypeCheckerResult lhs_result : lhs_results ) {
    			if( lhs_result.type().isNone() ) {
            		// Typechecking of subexpr already failed...
            		return TypeCheckerResult.compose(that, subtypeChecker, all_results);    				
    			}
    			else {
    				element_types.add(lhs_result.type().unwrap());
    			}
    		}
    		lhs_type = NodeFactory.makeTupleType(element_types);
    	}
    	
    	TypeCheckerResult result;
    	
    	// If opr is not :=, then rules are more like function call rules
    	if( that.getOpr().isSome() ) {
    		TypeCheckerResult opr_result_ = opr_result.unwrap();
    		if( opr_result_.type().isNone() ) {
        		// Typechecking of subexpr already failed...
        		return TypeCheckerResult.compose(that, subtypeChecker, all_results);    			
    		}
    		else {
    			// By this point, all subexpressions have typechecked properly
    			Type opr_type = opr_result_.type().unwrap();
    			// Now we get the type of the resulting application
    			Option<Type> result_type = 
    			TypesUtil.applicationType(subtypeChecker, opr_type,
    					new ArgList(lhs_type, rhs_type));
    			
    			if( result_type.isSome() ) {
    				// successful
    				all_results.add(new TypeCheckerResult(that));
    				result = TypeCheckerResult.compose(that, result_type.unwrap(), subtypeChecker, all_results);
    			}
    			else {
					// no operator found for these types
					result = new TypeCheckerResult(that, 
							TypeError.make("No applicable call to " + that.getOpr().unwrap() +
									" can be found for arguments of type " + lhs_type + " and " +
									rhs_type + ".", 
									that));
    			}
    		}
    	}
    	else {
    		// The whole thing is just regular assignment
    		// Now make sure RHS <: LHS
    		TypeCheckerResult subtype_result = 
        	this.checkSubtype(rhs_type, lhs_type, that);
    		
    		all_results.add(subtype_result);
    		result = TypeCheckerResult.compose(that, Types.VOID, subtypeChecker, all_results);
    	}
    	
		return result;
	}

	public TypeCheckerResult forTupleExprOnly(TupleExpr that,
                                              List<TypeCheckerResult> exprs_result) {
        List<Type> types = new ArrayList<Type>(exprs_result.size());
        for (TypeCheckerResult r : exprs_result) {
            if (r.type().isNone()) {
                return TypeCheckerResult.compose(that, subtypeChecker, exprs_result);
            }
            types.add(r.type().unwrap());
        }
        return TypeCheckerResult.compose(that, NodeFactory.makeTupleType(types), subtypeChecker, exprs_result);
    }

    public TypeCheckerResult forContractOnly(Contract that,
                                             Option<List<TypeCheckerResult>> requires_result,
                                             Option<List<TypeCheckerResult>> ensures_result,
                                             Option<List<TypeCheckerResult>> invariants_result) {
        TypeCheckerResult result = new TypeCheckerResult(that);

        // Check that each 'requires' expression is Boolean
        if (requires_result.isSome()) {
            for (TypeCheckerResult r : requires_result.unwrap()) {
                if (r.type().isNone()) continue;
                Type exprType = r.type().unwrap();
                result = TypeCheckerResult.compose(
                        that,
                        subtypeChecker,
                        result, checkSubtype(exprType,
                                Types.BOOLEAN,
                                r.ast(),
                                errorMsg("Attempt to use expression of type ", exprType,
                                         " in a 'requires' clause, instead of ",Types.BOOLEAN)));
            }
        }

        return TypeCheckerResult.compose(that,
                subtypeChecker,
                TypeCheckerResult.compose(that, subtypeChecker, requires_result),
                TypeCheckerResult.compose(that, subtypeChecker, ensures_result),
                                         TypeCheckerResult.compose(that, subtypeChecker, invariants_result), result);
    }

    @Override
    public TypeCheckerResult forCaseExpr(CaseExpr that) {
        TypeCheckerResult result;
        Type caseType = null;

        // Check if we are dealing with a normal case (i.e. not a "most" case)
        if (that.getParam().isSome()) {
            Expr param = that.getParam().unwrap();
            result = TypeCheckerResult.compose(that, subtypeChecker, forCaseExprNormal(that, param));
        } else {
            result = TypeCheckerResult.compose(that, subtypeChecker, forCaseExprMost(that));
        }
        return TypeCheckerResult.compose(that, wrap(caseType), subtypeChecker, result);
    }

    private TypeCheckerResult forCaseExprNormal(CaseExpr that, Expr param) {
        TypeCheckerResult result = new TypeCheckerResult(that);

        // Try to type check everything before giving up on an error.
        TypeCheckerResult paramResult = param.accept(this);
        result = TypeCheckerResult.compose(that, subtypeChecker, result, paramResult);

        // Maps a distinct guard types to the first guard expr with said type
        Relation<Type, Expr> guards = new HashRelation<Type, Expr>(true, false);
        List<Type> clauseTypes = new ArrayList<Type>(that.getClauses().size()+1);
        int numClauses = 0;

        // Type check each guard and block
        for (CaseClause clause : that.getClauses()) {
            TypeCheckerResult guardResult = clause.getMatch().accept(this);
            result = TypeCheckerResult.compose(that, subtypeChecker, result, guardResult);
            if (guardResult.type().isSome()) {
                guards.add(guardResult.type().unwrap(), clause.getMatch());
            }
            TypeCheckerResult blockResult = clause.getBody().accept(this);
            result = TypeCheckerResult.compose(that, subtypeChecker, result, blockResult);
            if (blockResult.type().isSome()) {
                clauseTypes.add(blockResult.type().unwrap());
            }
            ++numClauses;
        }

        // Type check the else clause
        if (that.getElseClause().isSome()) {
            TypeCheckerResult blockResult = that.getElseClause().unwrap().accept(this);
            result = TypeCheckerResult.compose(that, subtypeChecker, result, blockResult);
            if (blockResult.type().isSome()) {
                clauseTypes.add(blockResult.type().unwrap());
            }
            ++numClauses;
        }

        // Type check compare operator if given, otherwise check IN and EQ
        Type givenOpType = null;
        Type inOpType = null;
        Type eqOpType = null;
        // TODO: Change these to be qualified operators
        Op givenOp = that.getCompare().unwrap(null);
        Op inOp = new Op("IN", some((Fixity)new InFixity()));
        Op eqOp = new Op("EQ", some((Fixity)new InFixity()));
        if (that.getCompare().isSome()) {
            TypeCheckerResult opResult = givenOp.accept(this);
            result = TypeCheckerResult.compose(that, subtypeChecker, result, opResult);
            givenOpType = opResult.type().unwrap(null);
        } else {
            inOpType = inOp.accept(this).type().unwrap(null);
            eqOpType = eqOp.accept(this).type().unwrap(null);
        }

        // Check if failures prevent us from continuing
        if ((givenOpType == null && inOpType == null && eqOpType == null)
                || paramResult.type().isNone()) {
            return result;
        }

        // Init some types
        Type paramType = paramResult.type().unwrap();
        Type paramGeneratorType = Types.makeGeneratorType(paramType);

        // Type check "paramExpr OP guardExpr" for each distinct type
        for (Type guardType : guards.firstSet()) {

            Op op = givenOp;
            Type opType = givenOpType;
            if (opType == null) {
            	// This check used to just check for sub-typing, but had to change after
            	// type inference was introduced.
                if (subtypeChecker.subtype(guardType, paramGeneratorType).isSatisfiable()) {
                    op = inOp;
                    opType = inOpType;
                } else {
                    op = eqOp;
                    opType = eqOpType;
                }
            }

            Option<Type> applicationType =
                TypesUtil.applicationType(subtypeChecker, opType,
                                          new ArgList(paramType, guardType));

            // Check if "opType paramType guardType" application has type Boolean
//            if (applicationType.isSome() && subtypeChecker.subtype(applicationType.unwrap(), Types.BOOLEAN)) {
//            	for (Expr guardExpr : guards.getSeconds(guardType)) {
//            		result = TypeCheckerResult.compose(that, result,
//            				new TypeCheckerResult(guardExpr,
//            						TypeError.make(errorMsg("Guard expression has type ", guardType, ", which is invalid ",
//            								"for 'case' parameter type ", paramType, " and operator ",
//            								op.getText(), "."),
//            								guardExpr)));
//            	}
//            }
            
            // Old check didn't work with type inference. Error messages may suck here.
            // Check if "opType paramType guardType" application has type Boolean
            if( applicationType.isSome() ) {
            	result = 
            		TypeCheckerResult.compose(that,
            				subtypeChecker,
            				this.checkSubtype(applicationType.unwrap(), Types.BOOLEAN, that, 
            				"Guard expression is invalid for 'case' parameter type and operator."), result);
            }
            
        }

        // Get the type of the whole expression
        Type caseType = null;
        if (numClauses == clauseTypes.size()) {
            // Only set a type for this node if all clauses were typed
            caseType = subtypeChecker.join(clauseTypes);
        }
        return TypeCheckerResult.compose(that, caseType, subtypeChecker, result);
    }

    private TypeCheckerResult forCaseExprMost(CaseExpr that) {
        TypeCheckerResult result = new TypeCheckerResult(that);

        // Try to type check everything before giving up on an error.
        assert(that.getCompare().isSome());
        Op op = that.getCompare().unwrap();
        TypeCheckerResult opResult = op.accept(this);
        result = TypeCheckerResult.compose(that, subtypeChecker, result, opResult);
        assert(opResult.type().isSome());
        Type opType = opResult.type().unwrap();

        // Maps a distinct guard types to the first guard expr with said type
        Relation<Type, Expr> guards = new HashRelation<Type, Expr>(true, false);
        List<Type> clauseTypes = new ArrayList<Type>(that.getClauses().size());

        // Type check each guard and block
        for (CaseClause clause : that.getClauses()) {
            TypeCheckerResult guardResult = clause.getMatch().accept(this);
            result = TypeCheckerResult.compose(that, subtypeChecker, result, guardResult);
            if (guardResult.type().isSome()) {
                guards.add(guardResult.type().unwrap(), clause.getMatch());
            }
            TypeCheckerResult blockResult = clause.getBody().accept(this);
            result = TypeCheckerResult.compose(that, subtypeChecker, result, blockResult);
            if (blockResult.type().isSome()) {
                clauseTypes.add(blockResult.type().unwrap());
            }
        }

        // Check if failures prevent us from continuing
        if (opResult.type().isNone()) {
            return result;
        }

        // Type check "guardExpr_i OP guardExpr_j" for each expr types i, j
        for (Pair<Type, Type> guardTypePair : IterUtil.cross(guards.firstSet(), guards.firstSet())) {
            Type guardTypeL = guardTypePair.first();
            Type guardTypeR = guardTypePair.second();

            Option<Type> applicationType =
                TypesUtil.applicationType(subtypeChecker, opType,
                                          new ArgList(guardTypeL, guardTypeR));

//            // Check if "opType guardType_i guardType_j" application has type Boolean
//            if (applicationType.isSome() && subtypeChecker.subtype(applicationType.unwrap(), Types.BOOLEAN)) {
//
//                // The list of expressions for which to generate errors is got from both types'
//                // lists of guard expressions. If the types are equal, do not compose these lists.
//                Iterable<Expr> guardExprsForTypes =
//                    (guardTypeL.equals(guardTypeR)) ? guards.getSeconds(guardTypeL)
//                                                    : IterUtil.compose(guards.getSeconds(guardTypeL),
//                                                                       guards.getSeconds(guardTypeR));
//                for (Expr guardExpr : guardExprsForTypes) {
//                    result = TypeCheckerResult.compose(that, result,
//                            new TypeCheckerResult(guardExpr,
//                                    TypeError.make(errorMsg("Guard expression types are invalid for ",
//                                                            "extremum operator: ", guardTypeL, " ",
//                                                            op.getText(), " ", guardTypeR),
//                                                   guardExpr)));
//                }
//            }
            
            // Old check didn't work with type inference. Error messages may suck here.
            // Check if "opType guardType_i guardType_j" application has type Boolean
            if( applicationType.isSome() ) {
            	result = 
            		TypeCheckerResult.compose(that,
            				subtypeChecker,
            				this.checkSubtype(applicationType.unwrap(), Types.BOOLEAN, that, 
            				"Guard expression is invalid for 'case' parameter type and operator."), result);
            }
        }

        // Get the type of the whole expression
        Type caseType = null;
        if (that.getClauses().size() == clauseTypes.size()) {
            // Only set a type for this node if all clauses were typed
            caseType = subtypeChecker.join(clauseTypes);
        }
        return TypeCheckerResult.compose(that, caseType, subtypeChecker, result);
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

    public TypeCheckerResult forOp(Op that) {
        Option<BindingLookup> binding = typeEnv.binding(that);
        if (binding.isSome()) {
            return new TypeCheckerResult(that, binding.unwrap().getType());
        } else {
            return new TypeCheckerResult(that,
                                         TypeError.make(errorMsg("Operator not found: ",
                                                                 OprUtil.decorateOperator(that)),
                                                        that));
        }
    }

    public TypeCheckerResult forEnclosing(Enclosing that) {
        Option<BindingLookup> binding = typeEnv.binding(that);
        if (binding.isSome()) {
            return new TypeCheckerResult(that, binding.unwrap().getType());
        } else {
            return new TypeCheckerResult(that,
                                         TypeError.make(errorMsg("Enclosing operator not found: ",
                                                                 that),
                                                        that));
        }
    }

    public TypeCheckerResult forOpExprOnly(OpExpr that,
                                           TypeCheckerResult op_result,
                                           List<TypeCheckerResult> args_result) {
        Option<Type> applicationType = none();
        if (op_result.type().isSome()) {
            Type arrowType = op_result.type().unwrap();
            ArgList argTypes = new ArgList();
            boolean success = true;
            for (TypeCheckerResult r : args_result) {
                if (r.type().isNone()) {
                    success = false;
                    break;
                }
                argTypes.add(r.type().unwrap());
            }
            if (success) {
                applicationType = TypesUtil.applicationType(subtypeChecker, arrowType, argTypes);
                if (applicationType.isNone()) {
                    // Guaranteed at least one operator because all the overloaded operators
                    // are created by disambiguation, not by the user.
                    OpName opName = that.getOp().getOps().get(0);
                    return TypeCheckerResult.compose(that,
                            subtypeChecker,
                            op_result,
                            TypeCheckerResult.compose(that, subtypeChecker, args_result), new TypeCheckerResult(that, TypeError.make(errorMsg("Call to operator ",
                                                                                opName,
                                                                                " has invalid arguments."),
                                                                       that)));
                }
            }
        }
        return TypeCheckerResult.compose(that,
                                         applicationType,
                                         subtypeChecker,
                                         op_result, TypeCheckerResult.compose(that, subtypeChecker, args_result));
    }

    public TypeCheckerResult forTightJuxtOnly(TightJuxt that,
                                              List<TypeCheckerResult> exprs_result) {
        // The expressions list contains at least two elements.
        assert (exprs_result.size() >= 2);
        assert (exprs_result.get(0).type().isSome());
        assert (exprs_result.get(1).type().isSome());

        Type lhsType = exprs_result.get(0).type().unwrap();
        Type rhsType = exprs_result.get(1).type().unwrap();

        // TODO: If LHS is not a function, treat juxtaposition as operator.
        return TypeCheckerResult.compose(that,
                                         TypesUtil.applicationType(subtypeChecker,
                                                                   lhsType,
                                                                   new ArgList(rhsType)),
                                         subtypeChecker, exprs_result);
    }

    @Override
	public TypeCheckerResult forThrowOnly(Throw that,
			TypeCheckerResult expr_result) {
		// A throw expression has type bottom, pretty much regardless
    	// but expr must have type Exception
    	if( expr_result.type().isNone() ) {
    		// Failure in subexpr
    		return TypeCheckerResult.compose(that, Types.BOTTOM, subtypeChecker, expr_result);
    	}
    	else {
    		List<TypeCheckerResult> results = new ArrayList<TypeCheckerResult>(2);
    		TypeCheckerResult expr_is_exn = this.checkSubtype(expr_result.type().unwrap(),
    				Types.EXCEPTION, that.getExpr(), "'throw' can only throw objects of Exception type. " +
    				"This expression is of type " + expr_result.type().unwrap());
    		results.add(expr_is_exn);
    		results.add(expr_result);
    		return TypeCheckerResult.compose(that, Types.BOTTOM, subtypeChecker, results);
    	}
	}

	public TypeCheckerResult forLabel(Label that) {

        // Make sure this label isn't already bound
        Option<BindingLookup> b = typeEnv.binding(that.getName());
        if (b.isSome()) {
            TypeCheckerResult bodyResult = that.getBody().accept(this);
            return TypeCheckerResult.compose(that,
                subtypeChecker,
                new TypeCheckerResult(that, TypeError.make(errorMsg("Cannot use an existing identifier ",
                                                                    "for a 'label' expression: ",
                                                                    that.getName()),
                                                           that)), bodyResult);
        }

        // Check for nested label of same name
        if (labelExitTypes.containsKey(that.getName())) {
            TypeCheckerResult bodyResult = that.getBody().accept(this);
            return TypeCheckerResult.compose(that,
                subtypeChecker,
                new TypeCheckerResult(that, TypeError.make(errorMsg("Name of 'label' expression ",
                                                                    "already in scope: ", that.getName()),
                                                           that)), bodyResult);
        }

        // Initialize the set of exit types
        labelExitTypes.put(that.getName(), some((Set<Type>)new HashSet<Type>()));

        // Extend the checker with this label name in the type env
        TypeChecker newChecker = this.extend(Collections.singletonList(NodeFactory.makeLValue(that.getName(), Types.LABEL)));
        TypeCheckerResult bodyResult = that.getBody().accept(newChecker);

        // If the body was typed, union all the exit types with it.
        // If any exit type is none, then don't type this label.
        Option<Type> labelType = none();
        if (bodyResult.type().isSome()) {
            Option<Set<Type>> exitTypes = labelExitTypes.get(that.getName());
            if (exitTypes.isSome()) {
                Set<Type> _exitTypes = exitTypes.unwrap();
                _exitTypes.add(bodyResult.type().unwrap());
                labelType = some(subtypeChecker.join(_exitTypes));
            }
        }

        // Destroy the mappings for this label
        labelExitTypes.remove(that.getName());
        return TypeCheckerResult.compose(that, labelType, subtypeChecker, bodyResult);
    }

    public TypeCheckerResult forExit(Exit that) {
        assert (that.getTarget().isSome()); // Filled in by disambiguator
        Id labelName = that.getTarget().unwrap();
        Option<BindingLookup> b = typeEnv.binding(labelName);
        assert (that.getReturnExpr().isSome()); // Filled in by disambiguator
        if (b.isNone()) {
            TypeCheckerResult withResult = that.getReturnExpr().unwrap().accept(this);
            return TypeCheckerResult.compose(
                    that,
                    Types.BOTTOM,
                    subtypeChecker,
                    new TypeCheckerResult(that,
                                          TypeError.make(errorMsg("Could not find 'label' expression with name: ",
                                                                  labelName),
                                                         labelName)), withResult);
        }
        Type targetType = b.unwrap().getType().unwrap(null);
        if (!(targetType instanceof LabelType)) {
            TypeCheckerResult withResult = that.getReturnExpr().unwrap().accept(this);
            return TypeCheckerResult.compose(
                    that,
                    Types.BOTTOM,
                    subtypeChecker,
                    new TypeCheckerResult(that,
                                         TypeError.make(errorMsg("Target of 'exit' expression is not a label name: ",
                                                                 labelName),
                                                        labelName)), withResult);
        }

        // Append the 'with' type to the list for this label
        TypeCheckerResult withResult = that.getReturnExpr().unwrap().accept(this);
        if (withResult.type().isNone()) {
            labelExitTypes.put(labelName, Option.<Set<Type>>none());
        } else {
            assert (labelExitTypes.get(labelName).isSome());
            labelExitTypes.get(labelName).unwrap().add(withResult.type().unwrap());
        }
        return TypeCheckerResult.compose(that, Types.BOTTOM, subtypeChecker, withResult);
    }

    public TypeCheckerResult forSpawn(Spawn that) {
        // Create a new type checker that conceals any labels
        TypeChecker newChecker = this.extendWithout(labelExitTypes.keySet());
        TypeCheckerResult bodyResult = that.getBody().accept(newChecker);
        if (bodyResult.type().isSome()) {
            return TypeCheckerResult.compose(that,
                                             Types.makeThreadType(bodyResult.type().unwrap()),
                                             subtypeChecker, bodyResult);
        } else {
            return TypeCheckerResult.compose(that, subtypeChecker, bodyResult);
        }
    }

    public TypeCheckerResult forAtomicExpr(AtomicExpr that) {
        return forAtomic(that,
                         that.getExpr(),
                         errorMsg("A 'spawn' expression must not occur inside an 'atomic' expression."));
    }

	@Override
	public TypeCheckerResult forCatch(Catch that) {
		// We have to pass the name down so it can be bound to each exn type in turn
		Id bound_name = that.getName();
		List<TypeCheckerResult> clause_results = 
			recurOnCatchClausesWithIdToBind(that.getClauses(),bound_name);
		// Gather all the types of the catch clauses
		List<Type> clause_types = new ArrayList<Type>(clause_results.size());
		for( TypeCheckerResult r : clause_results ) {
			if( r.type().isSome() )
				clause_types.add(r.type().unwrap());
		}
		// resulting type is the join of those types
		return TypeCheckerResult.compose(that, this.subtypeChecker.join(clause_types), subtypeChecker, clause_results);
	}

	/**
	 * Recur on the given list of catch clauses, binding the Id passed along to whatever exn type
	 * the clause declares it catches. This is needed because the bound Id comes from a 
	 * super-expression, Catch, but it cannot be bound until we are down in the CatchClause,
	 * where the exception type is known.
	 */
	private List<TypeCheckerResult> recurOnCatchClausesWithIdToBind(List<CatchClause> catch_clauses, 
			                                                        Id id_to_bind) {
		List<TypeCheckerResult> result = new ArrayList<TypeCheckerResult>(catch_clauses.size());
		for( CatchClause catch_clause : catch_clauses ) {
			result.add(forCatchClauseWithIdToBind(catch_clause, id_to_bind));
		}
		return result;
	}
	
	/**
	 * Given the CatchClause and an Id, the Id will be bound to the exception type that the catch
	 * clause declares to catch, and then its body will be type-checked.
	 */
	private TypeCheckerResult forCatchClauseWithIdToBind(CatchClause that, 
			                                             Id id_to_bind) {
		TypeCheckerResult match_result = that.getMatch().accept(this);
		// type must be an exception
		TypeCheckerResult is_exn_result = checkSubtype(that.getMatch(), Types.EXCEPTION, that.getMatch(), 
				"Catch clauses must catch sub-types of Exception, but " + that.getMatch() + " is not.");
		
		// bind id and check the body
		LValueBind lval = NodeFactory.makeLValue(id_to_bind, that.getMatch());
		TypeChecker extend_tc = this.extend(Collections.singletonList(lval));
		TypeCheckerResult body_result = that.getBody().accept(extend_tc);

		// result has body type
		return TypeCheckerResult.compose(that, body_result.type(), subtypeChecker, match_result, is_exn_result, body_result);
	}
	
	@Override
	public TypeCheckerResult forTryAtomicExpr(TryAtomicExpr that) {
		return forAtomic(that,
                that.getExpr(),
                errorMsg("A 'spawn' expression must not occur inside a 'try atomic' expression."));
	}
	
	@Override
	public TypeCheckerResult forTryOnly(Try that,
			                            TypeCheckerResult body_result,
			                            Option<TypeCheckerResult> catch_result,
			                            List<TypeCheckerResult> forbid_result,
			                            Option<TypeCheckerResult> finally_result) {
		// gather all sub-results
		List<TypeCheckerResult> all_results = new LinkedList<TypeCheckerResult>();
		all_results.add(body_result);
		if( catch_result.isSome() ) all_results.add(catch_result.unwrap());
		all_results.addAll(forbid_result);
		if( finally_result.isSome() ) all_results.add(finally_result.unwrap());
		
		// Check that all forbids are subtypes of exception 
		for( Type t : that.getForbid() ) {
			TypeCheckerResult r =
			this.checkSubtype(t, Types.EXCEPTION, t,"All types in 'forbids' clause must be subtypes of " +
					"Exception, but "+ t + " is not.");
			all_results.add(r);
		}
		// the resulting type is the join of try, catches, and finally
		List<Type> all_types = new LinkedList<Type>();
		if( body_result.type().isSome() ) 
			all_types.add(body_result.type().unwrap());
		if( catch_result.isSome() && catch_result.unwrap().type().isSome() )
			all_types.add(catch_result.unwrap().type().unwrap());
		if( finally_result.isSome() && finally_result.unwrap().type().isSome() )
			all_types.add(finally_result.unwrap().type().unwrap());
		
		return TypeCheckerResult.compose(that, this.subtypeChecker.join(all_types), subtypeChecker, all_results);
	}

	private TypeCheckerResult forAtomic(Node that, Expr body, final String errorMsg) {
        TypeChecker newChecker = new TypeChecker(table,
                                                 staticParamEnv,
                                                 typeEnv,
                                                 compilationUnit,
                                                 subtypeChecker,
                                                 labelExitTypes) {
            @Override public TypeCheckerResult forSpawn(Spawn that) {
                // Use TypeChecker's forSpawn method, but compose an error onto the result
                return TypeCheckerResult.compose(
                        that,
                        subtypeChecker,
                        new TypeCheckerResult(that,
                                             TypeError.make(errorMsg,
                                                            that)), that.accept(TypeChecker.this));
            }
        };
        TypeCheckerResult bodyResult = body.accept(newChecker);
        return TypeCheckerResult.compose(that, bodyResult.type(), subtypeChecker, bodyResult);
    }

    // TRIVIAL NODES ---------------------
    @Override
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

    public TypeCheckerResult forAnyType(AnyType that) {
        return new TypeCheckerResult(that);
    }

    public TypeCheckerResult forTraitType(TraitType that) {
        return new TypeCheckerResult(that);
    }

    public TypeCheckerResult forNormalParam(NormalParam that) {
        // No checks needed to be performed on a NormalParam.
        return new TypeCheckerResult(that);
    }

    public TypeCheckerResult forTypeParam(TypeParam that) {
        // No checks needed to be performed on a TypeParam.
        return new TypeCheckerResult(that);
    }

    
    
    @Override
	public TypeCheckerResult forTypecase(Typecase that) {
    	// typecase is a somewhat interesting case
    	List<TypeCheckerResult> clauses_result;
    	List<TypeCheckerResult> bind_results = new LinkedList<TypeCheckerResult>();
    	List<Id> bindings = that.getBind().getA();
    	
    	if( that.getBind().getB().isSome()){
    		TypeCheckerResult expr=that.getBind().getB().unwrap().accept(this);
    		if(expr.type().isNone()){
    			return expr;
    		}
    		Type original_type=expr.type().unwrap();
    		
			if(bindings.size()>1){
				// typecase (a,b,c) = someX of ...
    			if(!(original_type instanceof TupleType)){
    				String err="Right hand side of binding not a tuple";
    				return TypeCheckerResult.compose(that, 
    						subtypeChecker,new TypeCheckerResult(that,TypeError.make(err, that.getBind().getB().unwrap())), expr);
    			}
    			TupleType tuple=(TupleType)original_type;
    			if(tuple.getElements().size()!=bindings.size()){
    				String err="Number of bindings does not match type of expression";
    				return TypeCheckerResult.compose(that, 
    						subtypeChecker,new TypeCheckerResult(that,TypeError.make(err, that.getBind().getB().unwrap())), expr);
    			}
    			clauses_result = recurOnListOfTypecaseClauseWithBindings(that.getClauses(),bindings,original_type);
    		}
    		else{
    			// x = some_expr
    			clauses_result = recurOnListOfTypecaseClauseWithBindings(that.getClauses(),bindings,original_type);
    		}
    	}
    	else {
    		// typecase x of ...
    		bind_results = this.recurOnListOfId(bindings);
    		
    		List<Type> bind_types = new ArrayList<Type>(bindings.size());
    		for( TypeCheckerResult bind_result : bind_results ) {
    			if( bind_result.type().isNone() ) {
    				return TypeCheckerResult.compose(that, subtypeChecker, bind_results);
    			}
    			else {
    				bind_types.add(bind_result.type().unwrap());
    			}
    		}
    		
    		// make sure that all bound vars are immutable
    		for( Id id : bindings ) {
    			Option<BindingLookup> lookup = this.typeEnv.binding(id);
    			assert(lookup.isSome());
    			// According to spec, ids must be immutable b/c they are rebound
    			if( lookup.unwrap().isMutable() ) {
    				String err = "Bound identifiers in a typecase expresion must be immutable and " + 
    					id + " is not.";
    				TypeCheckerResult mut_result = new TypeCheckerResult(that, TypeError.make(err, id));
    				bind_results.add(mut_result);
    				return TypeCheckerResult.compose(that, subtypeChecker, bind_results);
    			}
    		}
    		
    		// make type
    		Type original_type;
    		if( bindings.size() == 1 ) {
    			original_type = bind_types.get(0);
    		}
    		else {
    			original_type = NodeFactory.makeTupleType(bind_types);
    		}
    		clauses_result = recurOnListOfTypecaseClauseWithBindings(that.getClauses(), bindings, original_type);
    	}
    	
        Option<TypeCheckerResult> elseClause_result = recurOnOptionOfBlock(that.getElseClause());
        if( elseClause_result.isSome() ) {
        	clauses_result.add(elseClause_result.unwrap());
        }
        
        List<Type> all_types = new ArrayList<Type>(clauses_result.size()+1);
        for( TypeCheckerResult result : clauses_result ) {
        	if( result.type().isSome() ) {
        		all_types.add(result.type().unwrap());
        	}
        }
        
        clauses_result.addAll(bind_results);
        return TypeCheckerResult.compose(that, this.subtypeChecker.join(all_types), subtypeChecker, clauses_result);
	}

    /**
     * Recursively type-check the TypecaseClauses given, using the given Ids to bind to the type of
     * each TypecaseClause.
     * @param original_type Since typecase clauses use the intersection of the original type and the clause
     * type, the original type must be passed along.
     */
    private List<TypeCheckerResult> recurOnListOfTypecaseClauseWithBindings(List<TypecaseClause> clauses,
    		List<Id> to_bind, Type original_type) {
    	List<TypeCheckerResult> result = new ArrayList<TypeCheckerResult>(clauses.size());
    	for( TypecaseClause clause : clauses ) {
    		result.add(forTypecaseClauseWithBindings(clause, to_bind, original_type));
    	}
    	return result;
    }
    
    private TypeCheckerResult forTypecaseClauseWithBindings(TypecaseClause clause, List<Id> to_bind, Type original_type) {
        //Assumption: forTypeCase assures that original_type is a tuple of the same size as to_bind.size()
    	List<TypeCheckerResult> match_result = recurOnListOfType(clause.getMatch());
    	TypeChecker extend;
        if(to_bind.size()==1){
    		Type meet;
    		if(clause.getMatch().size()>1){
    			meet=this.subtypeChecker.meet(NodeFactory.makeTupleType(clause.getMatch()),original_type);
    		}
    		else{
    			meet=this.subtypeChecker.meet(clause.getMatch().get(0),original_type);
    		}
    		LValueBind bind = NodeFactory.makeLValue(to_bind.get(0), meet);
    		extend=this.extend(Collections.singletonList(bind));
    	}else{
    		if(to_bind.size()!=clause.getMatch().size()){
    			return new TypeCheckerResult(clause, TypeError.make("Tuple sizes don't match",clause));
    		}
    		else{
    			List<LValueBind> binds=new ArrayList<LValueBind>(to_bind.size());
    			Iterator<Type> mitr=clause.getMatch().iterator();
    			assert(original_type instanceof TupleType);
    			TupleType tuple=(TupleType)original_type;
    			Iterator<Type> titr=tuple.getElements().iterator();
    			for(Id id : to_bind){
    				Type meet = this.subtypeChecker.meet(mitr.next(),titr.next());
    				binds.add(NodeFactory.makeLValue(id, meet));
    			}
    			extend=this.extend(binds);
    		}
    	}
    	TypeCheckerResult result=clause.getBody().accept(extend);
    	match_result.add(result);
    	return TypeCheckerResult.compose(clause,result.type(),subtypeChecker, match_result);
    }

	public TypeCheckerResult forTypeArg(TypeArg that) {
        // No checks needed to be performed on a TypeArg.
        return new TypeCheckerResult(that);
    }

    public TypeCheckerResult forInFixity(InFixity that) {
        // No checks needed to be performed on a InFixity.
        return new TypeCheckerResult(that);
    }

    public TypeCheckerResult forPreFixity(PreFixity that) {
        // No checks needed to be performed on a PreFixity.
        return new TypeCheckerResult(that);
    }

    public TypeCheckerResult forPostFixity(PostFixity that) {
        // No checks needed to be performed on a PostFixity.
        return new TypeCheckerResult(that);
    }

    public TypeCheckerResult forNoFixity(NoFixity that) {
        // No checks needed to be performed on a NoFixity.
        return new TypeCheckerResult(that);
    }

    public TypeCheckerResult forMultiFixity(MultiFixity that) {
        // No checks needed to be performed on a MultiFixity.
        return new TypeCheckerResult(that);
    }

    public TypeCheckerResult forEnclosingFixity(EnclosingFixity that) {
        // No checks needed to be performed on a EnclosingFixity.
        return new TypeCheckerResult(that);
    }

    public TypeCheckerResult forBigFixity(BigFixity that) {
        // No checks needed to be performed on a BigFixity.
        return new TypeCheckerResult(that);
    }

    public TypeCheckerResult forImportStar(ImportStar that) {
        // No checks needed since all imports are handled by the trait table.
        return new TypeCheckerResult(that);
    }

    public TypeCheckerResult forTraitTypeWhereOnly(TraitTypeWhere that,
                                                   TypeCheckerResult type_result,
                                                   TypeCheckerResult where_result) {
        return TypeCheckerResult.compose(that, subtypeChecker, type_result, where_result);
    }

    // STUBS -----------------------------

    public TypeCheckerResult forComponentOnly(Component that,
                                              TypeCheckerResult name_result,
                                              List<TypeCheckerResult> imports_result,
                                              List<TypeCheckerResult> exports_result,
                                              List<TypeCheckerResult> decls_result) {
        return TypeCheckerResult.compose(that,
                                         subtypeChecker,
                                         name_result,
                                         TypeCheckerResult.compose(that, subtypeChecker, imports_result),
                                         TypeCheckerResult.compose(that, subtypeChecker, exports_result), TypeCheckerResult.compose(that, subtypeChecker, decls_result));
    }

    public TypeCheckerResult forWhereClause(WhereClause that) {
        if (that.getBindings().isEmpty() && that.getConstraints().isEmpty()) {
            return new TypeCheckerResult(that);
        } else {
            return defaultCase(that);
        }
    }

    
    
@Override
	public TypeCheckerResult forGeneratedExpr(GeneratedExpr that) {
		Pair<List<TypeCheckerResult>,List<LValueBind>> pair = recurOnListsOfGeneratorClauseBindings(that.getGens());
		TypeChecker extend=this.extend(pair.second());
		TypeCheckerResult body_result = that.getExpr().accept(extend);
		//make sure body has type void?
		List<TypeCheckerResult> res = pair.first();
		res.add(body_result);
		if( !body_result.type().unwrap().equals(Types.VOID) ) {
			res.add(new TypeCheckerResult(that,TypeError.make("Body of generated expression must have type (), but had type " +
									body_result.type().unwrap(), that.getExpr())));
		}
		return TypeCheckerResult.compose(that,Types.VOID, subtypeChecker, res);
	}

	@Override
    public TypeCheckerResult forWhile(While that) {
	
		Pair<TypeCheckerResult,List<LValueBind>> res=this.forGeneratorClauseGetBindings(that.getTest(), true);
		TypeChecker extended=this.extend(res.second());
		TypeCheckerResult body_result = that.getBody().accept(extended);
		return forWhileOnly(that, res.first(), body_result);
	}

	@Override
	public TypeCheckerResult forWhileOnly(While that,
			                              TypeCheckerResult test_result,
			                              TypeCheckerResult body_result) {

		//is test a type
		if(body_result.type().isNone()){
			return TypeCheckerResult.compose(that,Types.VOID, subtypeChecker, test_result, body_result);
		}

		String void_err = "Body of while loop must have type (), but had type " +
			body_result.type().unwrap();
		TypeCheckerResult void_result = this.checkSubtype(body_result.type().unwrap(), Types.VOID, that.getBody(), void_err);
		return TypeCheckerResult.compose(that,Types.VOID, subtypeChecker, test_result, body_result, void_result);
	}


	
	
}
