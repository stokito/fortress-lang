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


import static com.sun.fortress.exceptions.InterpreterBug.bug;
import static com.sun.fortress.exceptions.StaticError.errorMsg;
import static edu.rice.cs.plt.tuple.Option.none;
import static edu.rice.cs.plt.tuple.Option.some;
import static edu.rice.cs.plt.tuple.Option.wrap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import com.sun.fortress.compiler.IndexBuilder;
import com.sun.fortress.compiler.Types;
import com.sun.fortress.compiler.disambiguator.ExprDisambiguator.HierarchyHistory;
import com.sun.fortress.compiler.index.CompilationUnitIndex;
import com.sun.fortress.compiler.index.DeclaredVariable;
import com.sun.fortress.compiler.index.Functional;
import com.sun.fortress.compiler.index.FunctionalMethod;
import com.sun.fortress.compiler.index.Method;
import com.sun.fortress.compiler.index.ObjectTraitIndex;
import com.sun.fortress.compiler.index.ParamVariable;
import com.sun.fortress.compiler.index.ProperTraitIndex;
import com.sun.fortress.compiler.index.TraitIndex;
import com.sun.fortress.compiler.index.TypeConsIndex;
import com.sun.fortress.compiler.index.Variable;
import com.sun.fortress.compiler.typechecker.TypeEnv.BindingLookup;
import com.sun.fortress.compiler.typechecker.TypesUtil.ArgList;
import com.sun.fortress.exceptions.StaticError;
import com.sun.fortress.exceptions.TypeError;
import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.ExprFactory;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.OprUtil;
import com.sun.fortress.nodes_util.Span;
import com.sun.fortress.useful.NI;
import com.sun.fortress.useful.Useful;

import edu.rice.cs.plt.collect.ConsList;
import edu.rice.cs.plt.collect.EmptyRelation;
import edu.rice.cs.plt.collect.IndexedRelation;
import edu.rice.cs.plt.collect.Relation;
import edu.rice.cs.plt.collect.CollectUtil;
import edu.rice.cs.plt.collect.UnionRelation;
import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.lambda.Lambda;
import edu.rice.cs.plt.lambda.Lambda2;
import edu.rice.cs.plt.tuple.Option;
import edu.rice.cs.plt.tuple.Pair;

public class TypeChecker extends NodeDepthFirstVisitor<TypeCheckerResult> {

	private static TypeChecker addSelf(APIName api, Id name, TypeChecker newChecker, List<StaticParam> static_params){
		 Id qualified_type_name = NodeFactory.makeId(api, name);
		 TraitType self_type = NodeFactory.makeTraitType(qualified_type_name,TypeEnv.staticParamsToArgs(static_params));
		 return newChecker.extend(Collections.singletonList(NodeFactory.makeLValue("self", self_type)));
	 }
	/** Returns an error result if item is NOT an ExprMI */
	 private static Option<TypeCheckerResult> expectExprMI(MathItem item) {
		 boolean is_expr_item = isExprMI(item);
		 if( !is_expr_item ) {
			 String err = "Item at this location must be an expression, not an operator.";
			 TypeCheckerResult err_result = new TypeCheckerResult(item,TypeError.make(err, item));
			 return Option.some(err_result);
		 }
		 else {
			 return Option.none();
		 }
	 }
	/** Returns an error result if item is NOT a ParenthesisDelimitedMI */
	 private static Option<TypeCheckerResult> expectParenedExprItem(MathItem item) {
		 boolean is_parened = isParenedExprItem(item);
		 if( !is_parened ) {
			 String err = "Argument to function must be parenthesized.";
			 TypeCheckerResult err_result = new TypeCheckerResult(item, TypeError.make(err, item));
			 return Option.some(err_result);
		 }
		 else {
			 return Option.none();
		 }
	 }
	static private Type getTypeOfLValue(LValue lval) {
		 // TODO Implement unpasting
		 return
		 lval.accept(new NodeDepthFirstVisitor<Type>(){
			 @Override
			 public Type forLValueBind(LValueBind that) {
				 // All types must exist by the typechecker
				 return that.getType().unwrap();
			 }
		 });
	 }
	private static boolean isExprMI(MathItem item) {
		 return item.accept(new NodeDepthFirstVisitor<Boolean>(){
			 @Override public Boolean defaultCase(Node that) { return false; }
			 @Override public Boolean forNonParenthesisDelimitedMI(NonParenthesisDelimitedMI that) { return true; }
			 @Override public Boolean forParenthesisDelimitedMI(ParenthesisDelimitedMI that) { return true; }
		 });
	 }
	private static boolean isParenedExprItem(MathItem item) {
		 return item.accept(new NodeDepthFirstVisitor<Boolean>(){
			 @Override public Boolean defaultCase(Node that) { return false; }
			 @Override public Boolean forParenthesisDelimitedMI(ParenthesisDelimitedMI that) { return true; }
		 });
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

	private final CompilationUnitIndex compilationUnit;

	private final Map<Id, Option<Set<Type>>> labelExitTypes; // Note: this is mutable state.

	private StaticParamEnv staticParamEnv;

	private final TypeAnalyzer subtypeChecker;

	private TraitTable table;

	private TypeEnv typeEnv;

	public TypeChecker(TraitTable _table,
			StaticParamEnv _staticParams,
			TypeEnv _typeEnv,    
			CompilationUnitIndex _compilationUnit) {
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
			Map<Id, Option<Set<Type>>> _labelExitTypes) {
		table = _table;
		staticParamEnv = _staticParams;
		typeEnv = _typeEnv;
		compilationUnit = _compilationUnit;
		subtypeChecker = _subtypeChecker;
		labelExitTypes = _labelExitTypes;
	}

	/**
	  * Checks whether all the expressions in the block have type void
	  * Puts results in all_results
	  */
	 private List<TypeCheckerResult> allVoidButLast(List<TypeCheckerResult> results, List<Expr> block){
		 // every other expression except for the last must be void
		 List<TypeCheckerResult> all_results = new ArrayList<TypeCheckerResult>(results);
		 String void_err = "All expressions except the last in a block must have () type.";
		 for( int i=0; i<results.size()-1; i++ ) {
			 TypeCheckerResult r_i = results.get(i);
			 if( r_i.type().isSome() ) {
				 all_results.add(this.checkSubtype(r_i.type().unwrap(),
						 Types.VOID, block.get(i), void_err));
			 }
		 }
		 return all_results;
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

	/**
	  * Return a failing TypecheckerResult with error message if the given type {@code t} is
	  * not a trait.
	  */
	 private TypeCheckerResult assertTrait(BaseType t, Node ast, String msg, Node error_loc) {
		 TypeCheckerResult err_result = new TypeCheckerResult(ast, TypeError.make(msg, error_loc));

		 if( !(t instanceof TraitType) )
			 return err_result;

		 Option<TypeConsIndex> type_cons_ = this.table.typeCons(((TraitType)t).getName());
		 if( type_cons_.isSome() && type_cons_.unwrap() instanceof ProperTraitIndex ) {
			 return new TypeCheckerResult(ast);
		 }
		 else {
			 return err_result;
		 }
	 }

	// Checks the chunk given, and returns the result and a new expression.
	 // Requires that all TypeCheckerResults passed in actually have a type.
	 // Must be called on non-empty list.
	 private Pair<TypeCheckerResult,Expr> checkChunk(List<Pair<TypeCheckerResult,Expr>> chunk, OpRef infix_juxt) {
		 assert(!chunk.isEmpty());
		 // The non-functions in each chunk, if any, are replaced by a single element consisting of the non-functions grouped
		 // left-associatively into binary juxtapositions.
		 Option<Expr> last_non_fn = Option.none();
		 List<Expr> fns = new LinkedList<Expr>();
		 for( Pair<TypeCheckerResult,Expr> chunk_element : chunk ) {
			 if( !TypesUtil.isArrows(chunk_element.first().type().unwrap()) ) {
				 // If we've already seen an expr, created a binary juxt with previous
				 if( last_non_fn.isNone() )
					 last_non_fn = Option.some(chunk_element.second());
				 else
					 last_non_fn = Option.<Expr>some(ExprFactory.makeOpExpr(infix_juxt, last_non_fn.unwrap(), chunk_element.second()));
			 }
			 else {
				 fns.add(chunk_element.second());
			 }
		 }
		 // What remains in each chunk is then grouped right-associatively, as fn applications.
		 Option<Expr> result_expr = last_non_fn;
		 Collections.reverse(fns); // reverse so right assoc becomes left assoc
		 for( Expr fn : fns ) {
			 if( result_expr.isNone() )
				 result_expr = Option.some(fn);
			 else
				 result_expr = Option.<Expr>some(ExprFactory.make_RewriteFnApp(fn, result_expr.unwrap()));
		 }
		 // We are done. result_expr must be some or this method wasn't implemented correctly.
		 TypeCheckerResult result = TypeCheckerResult.compose(result_expr.unwrap(), subtypeChecker,
				 CollectUtil.makeList(IterUtil.pairFirsts(chunk)));
		 return Pair.make(result, result_expr.unwrap());
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


	 @Override
	 public TypeCheckerResult defaultCase(Node that) {
		 return new TypeCheckerResult(that);
	 }

	 private List<TraitIndex> expectTraitIndeces(List<TraitType> traits) {
		 List<TraitIndex> result = new ArrayList<TraitIndex>(traits.size());
		 for( TraitType type : traits ) {
			 result.add(expectTraitIndex(type));
		 }
		 return result;
	 }

	 /**
	  * Look up the index of the given type, expecting it to exist. If it
	  * does not, that represents a bug.
	  */
	 private TraitIndex expectTraitIndex(TraitType trait) {
		 Option<TypeConsIndex> tc_ = table.typeCons(trait.getName());
		 if( tc_.isNone() || !(tc_.unwrap() instanceof TraitIndex) ) {
			 return bug("This should never be anything but a legitimate trait index.");
		 }
		 else {
			 return (TraitIndex)tc_.unwrap();
		 }
	 }
	 
	 /** Check for ^ followed by ^ or ^ followed by [], both static errors. */
	 private Option<TypeCheckerResult> exponentiationStaticCheck(Node ast, List<MathItem> items) {

		 //Visitor checks for two exponentiations or an exponentiation and a subscript in a row
		 NodeDepthFirstVisitor<TypeCheckerResult> static_error = new NodeDepthFirstVisitor<TypeCheckerResult>() {
			 Option<Node> exponent = Option.none();
			 @Override
			 public TypeCheckerResult defaultCase(Node that) {
				 exponent=Option.none();
				 return new TypeCheckerResult(that);
			 }

			 @Override
			 public TypeCheckerResult forExponentiationMI(ExponentiationMI that) {
				 if(exponent.isNone()){
					 exponent=Option.<Node>some(that);
					 return new TypeCheckerResult(that);
				 }
				 else{
					 String err_message = "Two consecutive ^s";
					 StaticError err=TypeError.make(err_message, new Span(exponent.unwrap().getSpan(),that.getSpan()).toString());
					 return new TypeCheckerResult(that,err);
				 }
			 }

			 @Override
			 public TypeCheckerResult forSubscriptingMI(SubscriptingMI that) {
				 if(exponent.isNone()){
					 return new TypeCheckerResult(that);
				 }
				 else{
					 String err_message = "Exponentiation followed by subscripting is illegal";
					 StaticError err=TypeError.make(err_message, new Span(exponent.unwrap().getSpan(),that.getSpan()).toString());
					 exponent = Option.none();
					 return new TypeCheckerResult(that,err);
				 }
			 }
		 };
		 List<TypeCheckerResult> static_errors = static_error.recurOnListOfMathItem(items);
		 TypeCheckerResult static_error_result = TypeCheckerResult.compose(ast, subtypeChecker, static_errors);
		 if(!static_error_result.isSuccessful()){
			 return Option.some(static_error_result);
		 }
		 else {
			 return Option.none();
		 }
	 }

	 private TypeChecker extend(List<LValueBind> bindings) {
		return new TypeChecker(table, staticParamEnv,
				typeEnv.extendWithLValues(bindings),
				compilationUnit,
				subtypeChecker,
				labelExitTypes);
	}

	 private TypeChecker extend(List<StaticParam> newStaticParams, List<Param> newParams, WhereClause whereClause) {
		return new TypeChecker(table,
				staticParamEnv.extend(newStaticParams, whereClause),
				typeEnv.extendWithParams(newParams).extendWithStaticParams(newStaticParams),
				compilationUnit,
				subtypeChecker.extend(newStaticParams, whereClause),
				labelExitTypes);
	}

	 private TypeChecker extend(List<StaticParam> newStaticParams, Option<List<Param>> newParams, WhereClause whereClause) {
		return new TypeChecker(table,
				staticParamEnv.extend(newStaticParams, whereClause),
				typeEnv.extend(newParams).extendWithStaticParams(newStaticParams),
				compilationUnit,
				subtypeChecker.extend(newStaticParams, whereClause),
				labelExitTypes);
	}
	 
	 private TypeChecker extend(List<StaticParam> newStaticParams, WhereClause whereClause) {
		return new TypeChecker(table,
				staticParamEnv.extend(newStaticParams, whereClause),
				typeEnv.extendWithStaticParams(newStaticParams),
				compilationUnit,
				subtypeChecker.extend(newStaticParams, whereClause),
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

	 private TypeChecker extend(WhereClause whereClause) {
		return new TypeChecker(table,
				staticParamEnv.extend(Collections.<StaticParam>emptyList(), whereClause),
				typeEnv,
				compilationUnit,
				subtypeChecker.extend(Collections.<StaticParam>emptyList(), whereClause),
				labelExitTypes);
	}

	 public TypeChecker extendWithFnDefs(Relation<IdOrOpOrAnonymousName, ? extends FnDef> fns) {
		return new TypeChecker(table, staticParamEnv,
				typeEnv.extendWithFnDefs(fns),
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

	 public TypeChecker extendWithMethods(Relation<IdOrOpOrAnonymousName, Method> methods) {
		return new TypeChecker(table, staticParamEnv,
				typeEnv.extendWithMethods(methods),
				compilationUnit,
				subtypeChecker,
				labelExitTypes);
	}
	 
	 public TypeChecker extendWithout(Node declSite, Set<? extends IdOrOpOrAnonymousName> names) {
		return new TypeChecker(table, staticParamEnv,
				typeEnv.extendWithout(declSite, names),
				compilationUnit,
				subtypeChecker,
				labelExitTypes);
	}
	 
	 private Option<Type> findFieldInTraitHierarchy(List<TraitType> supers, FieldRef that) {

		 List<TraitType> new_supers = new ArrayList<TraitType>();
		 Option<Type> result = Option.none();

		 for( TraitType my_super : supers ) {
			 TraitIndex index = expectTraitIndex(my_super);

			 // Map to list of supertypes
			 for( TraitTypeWhere ttw : index.extendsTypes() ) {
				 Type t = ttw.getType();
				 new_supers.addAll(traitTypesCallable(t));
			 }

			 // check if trait has a getter
			 Map<Id,Method> getters=index.getters();
			 if(getters.containsKey(that.getField())) {
				 Method field=getters.get(that.getField());
				 return Option.some(field.getReturnType());
			 }
			 else {
				 //check if trait is an object
				 if(index instanceof ObjectTraitIndex) {
					 //Check if object has field
					 ObjectTraitIndex object_index=(ObjectTraitIndex)index;
					 Map<Id,Variable> fields=object_index.fields();
					 if(fields.containsKey(that.getField())){
						 Variable field=fields.get(that.getField());

						 if( field instanceof ParamVariable ) {
							 ParamVariable param = (ParamVariable)field;
							 Param field_node = param.ast();

							 Type field_type = 
								 field_node.accept(new NodeAbstractVisitor<Type>() {
									 @Override
									 public Type forNormalParam(NormalParam that) { return that.getType().unwrap(); }

									 @Override
									 public Type forVarargsParam(VarargsParam that) { return that.getType(); }
								 });

							 return Option.some(field_type);
						 }
						 else if( field instanceof DeclaredVariable ) {
							 DeclaredVariable var = (DeclaredVariable)field;
							 LValueBind bind = var.ast();
							 return Option.some(bind.getType().unwrap()); 
						 }
						 else {
							 return bug("Field of an object should not be a Singleton Object." + field);
						 }
					 }
				 }
				 //error no such field

			 }
		 }

		 if( result.isNone() && !new_supers.isEmpty() ) {
			 // recur
			 return this.findFieldInTraitHierarchy(new_supers, that);
		 }
		 else {
			 return Option.none();
		 }
	 }
	 
	 // This method does a lot: Basically all of the hard work of finding an appropriate overloading, including looking
	 // in parent traits.
	 private Pair<List<Method>,List<TypeCheckerResult>> findMethodsInTraitHierarchy(final IdOrOpOrAnonymousName method_name, List<TraitType> supers,
			 Type arg_type, List<StaticArg> in_static_args,
			 Node that) {
		 List<TypeCheckerResult> all_results= new ArrayList<TypeCheckerResult>();
		 List<Method> candidates=new ArrayList<Method>();
		 List<TraitType> new_supers=new ArrayList<TraitType>();
		 
		 for(TraitType type: supers) {
			 TraitIndex trait_index = expectTraitIndex(type);

			 final List<StaticArg> extended_type_args = type.getArgs();
			 final List<StaticParam> extended_type_params = trait_index.staticParameters();
			 // get all of the types this type extends. Note that because we can extend types with our own static args,
			 // we must visit the extended type with a static type replacer.
			 for( TraitTypeWhere ttw : trait_index.extendsTypes() ) {
				Type t = (Type)ttw.getType().accept(new StaticTypeReplacer(extended_type_params, extended_type_args));
				new_supers.addAll(traitTypesCallable(t));
			 }

			 // Get methods with the right name:
			 // Add all dotted methods,
			 Set<Method> methods_with_name = trait_index.dottedMethods().matchFirst(method_name);

			 for( Method m : methods_with_name ) {
				 List<StaticArg> static_args = new ArrayList<StaticArg>(in_static_args);
				 // same number of static args
				 if( m.staticParameters().size() > 0 && in_static_args.size() == 0 ) {
					 // we need to infer static arguments

					 // TODO if parameters are anything but TypeParam, we don't know
					 // how to infer it yet.
					 for( StaticParam p : m.staticParameters() )
						 if( !(p instanceof TypeParam) ) continue;
					 // Otherwise, we've got all static parameters
					 List<StaticArg> static_inference_params =
						 CollectUtil.makeList(
								 IterUtil.map(m.staticParameters(), new Lambda<StaticParam,StaticArg>(){
									 public StaticArg value(StaticParam arg0) {
										 Type ivt = NodeFactory.make_InferenceVarType(method_name.getSpan());
										 return new TypeArg(ivt);
									 }}));
					 static_args = static_inference_params;
				 }
				 else if(m.staticParameters().size()!=static_args.size()) {
					 // we don't need to infer, and they have different numbers of args
					 continue;
				 }
				 // instantiate method params with method and type args
				 Functional im_maybe = m.instantiate(Useful.concat(m.staticParameters(), extended_type_params),
						 Useful.concat(static_args, extended_type_args));
				 // we know this cast works, instantiate contract
				 Method im = (Method)im_maybe;
				 // Do they have the same number of parameters, or at least can they be matched.
				 ConstraintFormula mc = this.argsMatchParams(im.parameters(), arg_type);
				 // constraint satisfiable?
				 if(mc.isSatisfiable()) {
					 //add method to candidates
					 candidates.add(im);
					 all_results.add(new TypeCheckerResult(that,Option.<Type>none(),mc));
				 }

			 }
		 }

		 if(candidates.isEmpty() && !new_supers.isEmpty()){
			 return findMethodsInTraitHierarchy(method_name, new_supers, arg_type, in_static_args, that);
		 }
		 return Pair.make(candidates,all_results);
	 }
	 
	 private TypeCheckerResult findSetterInTraitHierarchy(IdOrOpOrAnonymousName field_name, List<TraitType> supers,
			 Type arg_type, Node ast){
		 List<TraitType> new_supers = new ArrayList<TraitType>();
		 Option<Type> result = Option.none();

		 for( TraitType my_super : supers ) {
			 TraitIndex trait_index = expectTraitIndex(my_super);


			 // Map to list of supertypes
			 for( TraitTypeWhere ttw : trait_index.extendsTypes() ) {
				 new_supers.addAll(traitTypesCallable(ttw.getType()));
			 }

			 // check if trait has a getter
			 Map<Id,Method> setters=trait_index.setters();
			 if(setters.containsKey(field_name)) {
				 Method field=setters.get(field_name);
				 ConstraintFormula works =  argsMatchParams(field.parameters(),arg_type);
				 if(!works.isSatisfiable()){
					 String errmes = "Argument to setter has wrong type";
					 StaticError err = TypeError.make(errmes, ast);
					 return new TypeCheckerResult(ast,Option.<Type>none(),Collections.singletonList(err),works);
				 }
				 else{
					 return new TypeCheckerResult(ast,works);
				 }
			 }
			 else {
				 // we used to check for a field but we don't think that's right.
				 //check if trait is an object
//				 if(trait_index instanceof ObjectTraitIndex){
//				 //Check if object has field
//				 ObjectTraitIndex object_index=(ObjectTraitIndex)trait_index;
//				 Map<Id,Variable> fields=object_index.fields();
//				 if(fields.containsKey(field_name)){
//				 Variable field=fields.get(field_name);
//				 Option<BindingLookup> type=this.typeEnv.binding(field_name);
//				 return this.checkSubtype(arg_type,type.unwrap().getType().unwrap(),ast, "Argument to field has wrong type");
//				 }
//				 }
				 //error no such field
			 }
			 //error receiver not a trait
		 }

		 if( result.isNone() && !new_supers.isEmpty() ) {
			 // recur
			 return this.findSetterInTraitHierarchy(field_name, new_supers, arg_type, ast );
		 }
		 else {
			 String errmes = "Setter for field "+ field_name +" not found.";
			 return new TypeCheckerResult(ast,TypeError.make(errmes, ast));
		 }

	 }
	 
    @Override
	 public TypeCheckerResult for_RewriteFnAppOnly(_RewriteFnApp that, Option<TypeCheckerResult> exprType_result, 
			 TypeCheckerResult function_result, TypeCheckerResult argument_result) {
		 // check sub expressions
		 if( function_result.type().isNone() || argument_result.type().isNone() )
			 return TypeCheckerResult.compose(that, subtypeChecker, function_result, argument_result);

		 Option<Pair<Type,ConstraintFormula>> app_result =
			 TypesUtil.applicationType(subtypeChecker, function_result.type().unwrap(),
					 new ArgList(argument_result.type().unwrap()));

		 Option<Type> result_type;
		 TypeCheckerResult result;

		 if( app_result.isSome() ) {
			 result = new TypeCheckerResult(that,app_result.unwrap().second());
			 result_type = Option.some(app_result.unwrap().first());
		 }
		 else {
			 String err = "Applicable overloading of function " + that.getFunction() + " could not be found for argument type " + argument_result.type(); // error message needs work
			 result = new TypeCheckerResult(that, TypeError.make(err, that));
			 result_type = Option.none();
		 }
		 // The result should be a _RewriteFnApp
		 return TypeCheckerResult.compose(that, result_type,
				 subtypeChecker, function_result, argument_result, result);
	 }

	 @Override
	 public TypeCheckerResult for_RewriteObjectRefOnly(_RewriteObjectRef that, Option<TypeCheckerResult> exprType_result, 
			 TypeCheckerResult obj_result,
			 List<TypeCheckerResult> staticArgs_result) {

		 if( obj_result.type().isNone() ) {
			 return TypeCheckerResult.compose(that, subtypeChecker, obj_result,
					 TypeCheckerResult.compose(that, subtypeChecker, staticArgs_result));
		 }
		 else if( (obj_result.type().unwrap() instanceof TraitType) ) {

			 return TypeCheckerResult.compose(that,obj_result.type().unwrap() ,subtypeChecker, obj_result,
					 TypeCheckerResult.compose(that, subtypeChecker, staticArgs_result));
		 }
		 else if( (obj_result.type().unwrap() instanceof _RewriteGenericSingletonType) ) {
			 // instantiate with static parameters
			 _RewriteGenericSingletonType uninstantiated_t = (_RewriteGenericSingletonType)obj_result.type().unwrap();

			 boolean match = StaticTypeReplacer.argsMatchParams(that.getStaticArgs(), uninstantiated_t.getStaticParams());
			 if( match ) {
				 // make a trait type that is GenericType instantiated
				 Type t = NodeFactory.makeTraitType(uninstantiated_t.getSpan(),
						 uninstantiated_t.isParenthesized(), uninstantiated_t.getName(), that.getStaticArgs());
				 return TypeCheckerResult.compose(that, t, subtypeChecker, obj_result,
						 TypeCheckerResult.compose(that, subtypeChecker, staticArgs_result));
			 }
			 else {
				 // error
				 String err = "Generic object, " + uninstantiated_t + " instantiated with invalid arguments, " + that.getStaticArgs();
				 TypeCheckerResult e_result = new TypeCheckerResult(that, TypeError.make(err, that));
				 return TypeCheckerResult.compose(that, subtypeChecker, obj_result, e_result,
						 TypeCheckerResult.compose(that, subtypeChecker, staticArgs_result));
			 }
		 }
		 else {
			 return bug("Unexpected type for ObjectRef.");
		 }
	 }

	 @Override
	 public TypeCheckerResult forAmbiguousMultifixOpExpr(final AmbiguousMultifixOpExpr that) {
		 // See if we can successfully typecheck this expression as a multifix one.
		 TypeCheckerResult multi_result = 
			 (new OpExpr(that.getSpan(),that.isParenthesized(),that.getMultifix_op(),that.getArgs()).accept(this));

		 if( multi_result.isSuccessful() ) {
			 return multi_result;
		 }
		 else {
			 if( that.getArgs().size() < 2 )
				 bug("This never should have been neither multifix nor infix.");

			 // If not, do it as a collection of left-associated infix expressions
			 return
			 IterUtil.fold(IterUtil.skipFirst(that.getArgs()), IterUtil.first(that.getArgs()), 
					 new Lambda2<Expr,Expr,Expr>(){
				 public Expr value(Expr arg0, Expr arg1) {
					 return ExprFactory.makeOpExpr(that.getInfix_op(), arg0, arg1);
				 }}).accept(this);
		 }
	 }

	 @Override
	 public TypeCheckerResult forAnyType(AnyType that) {
		 return new TypeCheckerResult(that);
	 }

	 @Override
	 public TypeCheckerResult forAPIName(APIName that) {
		 return new TypeCheckerResult(that);
	 }

	 @Override
	 public TypeCheckerResult forArgExprOnly(ArgExpr that, Option<TypeCheckerResult> exprType_result, 
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

	 // This case is only called for single element arrays ( e.g., [5] )
	 // and not for pieces of ArrayElements
	 @Override
	 public TypeCheckerResult forArrayElementOnly(ArrayElement that, Option<TypeCheckerResult> exprType_result, 
			 List<TypeCheckerResult> staticArgs_result,
			 TypeCheckerResult element_result) {

		 if( element_result.type().isNone() ) {
			 return TypeCheckerResult.compose(that, subtypeChecker, element_result,
					 TypeCheckerResult.compose(that, subtypeChecker, staticArgs_result));
		 }

		 List<StaticArg> staticArgs = that.getStaticArgs();
		 Id array = Types.getArrayKName(1);
		 Option<TypeConsIndex> ind=table.typeCons(array);
		 if(ind.isNone()){
			 array = Types.ARRAY_NAME;
			 ind = table.typeCons(array);
			 if(ind.isNone()){
				 bug(array+"not in table");
			 }
		 }
		 TraitIndex index = (TraitIndex)ind.unwrap();
		 if(staticArgs.isEmpty()){
			 TypeArg elem = NodeFactory.makeTypeArg(element_result.type().unwrap());
			 IntArg lower = NodeFactory.makeIntArgVal(""+0);
			 IntArg size = NodeFactory.makeIntArgVal(""+1);
			 return TypeCheckerResult.compose(that,Types.makeArrayKType(1, Useful.list(elem, lower, size)),this.subtypeChecker, element_result);
		 }
		 else{
			 if(StaticTypeReplacer.argsMatchParams(that.getStaticArgs(), index.staticParameters())){
				 TypeCheckerResult res=this.checkSubtype(element_result.type().unwrap(),
						 ((TypeArg)that.getStaticArgs().get(0)).getType(), that,
						 element_result.type().unwrap()+" must be a subtype of "+((TypeArg)that.getStaticArgs().get(0)).getType());
				 return TypeCheckerResult.compose(that,Types.makeArrayKType(1, that.getStaticArgs()),this.subtypeChecker, element_result, res,
						 TypeCheckerResult.compose(that,this.subtypeChecker,staticArgs_result));
			 }
			 else{
				 String err = "Explicit static arguments do not match required arguments for Array1 (" + index.staticParameters() + ".)";
				 TypeCheckerResult err_result = new TypeCheckerResult(that, TypeError.make(err, that));
				 return TypeCheckerResult.compose(that, subtypeChecker, err_result, element_result,
						 TypeCheckerResult.compose(that, subtypeChecker, staticArgs_result));
			 }
		 }

	 }


	 // This method is pretty long because we have to create a new visitor that visitis ArrayElements and ArrayElement
	 // knowing that we are inside of another ArrayElement.
	 @Override
	 public TypeCheckerResult forArrayElements(ArrayElements that) {
		 // Create a new visitor whose responsibility is the gathering of types and element sizes from lower dimensions
		 NodeDepthFirstVisitor<Pair<TypeCheckerResult,ConsList<Integer>>> elements_visitor =
			 new NodeDepthFirstVisitor<Pair<TypeCheckerResult,ConsList<Integer>>>() {
			 final NodeDepthFirstVisitor<Pair<TypeCheckerResult,ConsList<Integer>>> stored_this = this;
			 @Override
			 public Pair<TypeCheckerResult, ConsList<Integer>> forArrayElement(
					 ArrayElement that) {
				 Integer size = 1;
				 TypeCheckerResult elem_result = that.getElement().accept(TypeChecker.this);
				 // that.getStaticArgs() TODO, check me
				 return Pair.<TypeCheckerResult,ConsList<Integer>>make(elem_result, ConsList.singleton(size));
			 }

			 @Override
			 public Pair<TypeCheckerResult, ConsList<Integer>> forArrayElements(
					 ArrayElements that) {
				 // recur on sub-elements
				 Iterable<Pair<TypeCheckerResult,ConsList<Integer>>> elems_results =
					 IterUtil.map(that.getElements(), new Lambda<ArrayExpr,Pair<TypeCheckerResult,ConsList<Integer>>>(){
						 public Pair<TypeCheckerResult, ConsList<Integer>> value(
								 ArrayExpr arg0) {
							 return arg0.accept(stored_this);
						 }});

				 List<TypeCheckerResult> all_results = CollectUtil.makeList(IterUtil.pairFirsts(elems_results));
				 List<ConsList<Integer>> all_sizes = CollectUtil.makeList(IterUtil.pairSeconds(elems_results));

				 Integer size = that.getElements().size();
				 ConsList<Integer> last_size_list = IterUtil.first(all_sizes);
				 ConsList<Integer> new_size_list = ConsList.cons(size, last_size_list);

				 // Check that all sub-elements are typed
				 for( TypeCheckerResult r : all_results ) {
					 if( r.type().isNone() ) {
						 return Pair.make(TypeCheckerResult.compose(that, subtypeChecker, all_results), new_size_list);
					 }
				 }
				 // Make sizes in each list are the same as the equivalent position in last_size_list
				 for( ConsList<Integer> size_list : all_sizes ) {
					 Boolean sizes_match =
						 IterUtil.fold(IterUtil.zip(size_list, last_size_list), true, new Lambda2<Boolean,Pair<Integer,Integer>,Boolean>(){
							 public Boolean value(Boolean arg0, Pair<Integer, Integer> arg1) {
								 return arg0 & (arg1.first().equals(arg1.second()));
							 }});
					 if(size_list.isEmpty()){
						 ConsList<Integer> empty = ConsList.<Integer>empty();
						 return Pair.make(TypeCheckerResult.compose(that,subtypeChecker,all_results),empty);
					 }
					 if( !sizes_match) {
						 String err = "Sizes of elements in array dimension are not equal.";
						 TypeCheckerResult result = new TypeCheckerResult(that, TypeError.make(err, that));
						 result = TypeCheckerResult.compose(that, subtypeChecker, result,
								 TypeCheckerResult.compose(that, subtypeChecker, all_results));
						 ConsList<Integer> empty = ConsList.<Integer>empty();
						 return Pair.make(result, empty);
					 }
				 }

				 // Now create result type from elements
				 Type result_type = subtypeChecker.join(IterUtil.map(all_results, new Lambda<TypeCheckerResult,Type>(){
					 public Type value(TypeCheckerResult arg0) {
						 return arg0.type().unwrap();
					 }}));
				 TypeCheckerResult result = TypeCheckerResult.compose(that, result_type,subtypeChecker, all_results);
				 return Pair.make(result, new_size_list);
			 }
		 };

		 List<TypeCheckerResult> staticArgs_result = this.recurOnListOfStaticArg(that.getStaticArgs());
		 // Get type and dimension sizes from elements
		 Pair<TypeCheckerResult, ConsList<Integer>> elements_result_ = that.accept(elements_visitor);
		 TypeCheckerResult elements_result = elements_result_.first();
		 ConsList<? extends Integer> dim_sizes = elements_result_.second();

		 // Make sure it actually has a type
		 if( elements_result.type().isNone() )
			 return elements_result;

		 Type array_type = elements_result.type().unwrap();

		 // Now try to get array type for the dimension we have
		 int dim = that.getDimension();
		 Id k_array = Types.getArrayKName(dim);

		 Option<TypeConsIndex> ind = table.typeCons(k_array);
		 if( ind.isNone() ) {
			 bug("Array"+dim +" has not yet been implemented.");
		 }

		 TraitIndex trait_index = (TraitIndex)ind.unwrap();
		 List<StaticArg> sargs = that.getStaticArgs();

		 if( !sargs.isEmpty() ) {
			 if( StaticTypeReplacer.argsMatchParams(sargs, trait_index.staticParameters()) ) {
				 // First arg MUST BE a TypeArg, and it must be a supertype of the elements
				 Type declared_type = ((TypeArg)that.getStaticArgs().get(0)).getType();
				 TypeCheckerResult subtype_result =
					 this.checkSubtype(array_type, declared_type, that, "Array elements must be a subtype of explicity declared type" + declared_type + ".");
				 // then instantiate and return
				 Type return_type = Types.makeArrayKType(dim, sargs);
				 return TypeCheckerResult.compose(that, return_type, subtypeChecker, subtype_result,
						 TypeCheckerResult.compose(that, subtypeChecker, elements_result),
						 TypeCheckerResult.compose(that, subtypeChecker, staticArgs_result));
			 }
			 else {
				 // wrong args passed
				 String err = "Explicit static arguments don't matched required arguments for " + trait_index + ".";
				 TypeCheckerResult e_result = new TypeCheckerResult(that, TypeError.make(err, that));
				 return TypeCheckerResult.compose(that, subtypeChecker, e_result,
						 TypeCheckerResult.compose(that, subtypeChecker, staticArgs_result),
						 TypeCheckerResult.compose(that, subtypeChecker, elements_result));
			 }
		 }
		 else {
			 // then we just use what we determine to be true
			 List<StaticArg> inferred_args = new ArrayList<StaticArg>(1+dim*2);
			 inferred_args.add(NodeFactory.makeTypeArg(array_type));
			 for(int i=0;i<dim;i++) {
				 Integer s = ConsList.first(dim_sizes);
				 dim_sizes = ConsList.rest(dim_sizes);

				 IntArg lower_bound = NodeFactory.makeIntArgVal("0");
				 IntArg size = NodeFactory.makeIntArgVal(s.toString());

				 inferred_args.add(lower_bound);
				 inferred_args.add(size);
			 }
			 // then instantiate and return
			 Type return_type = Types.makeArrayKType(dim, inferred_args);
			 return TypeCheckerResult.compose(that, return_type, subtypeChecker,
					 TypeCheckerResult.compose(that, subtypeChecker, elements_result),
					 TypeCheckerResult.compose(that, subtypeChecker, staticArgs_result));
		 }
	 }


	 @Override
	 public TypeCheckerResult forAsExpr(AsExpr that) {
		 Type ascriptedType = that.getType();
		 TypeCheckerResult expr_result = that.getExpr().accept(this);
		 Type exprType = expr_result.type().isSome() ? expr_result.type().unwrap() : Types.BOTTOM;
		 return forTypeAnnotatedExprOnly(that,
				 expr_result,
				 errorMsg("Attempt to ascribe expression of type ",
						 exprType, " to non-supertype ", ascriptedType));
	 }

	 @Override
	 public TypeCheckerResult forAsIfExpr(AsIfExpr that) {
		 Type assumedType = that.getType();
		 TypeCheckerResult expr_result = that.getExpr().accept(this);
		 Type exprType = expr_result.type().isSome() ? expr_result.type().unwrap() : Types.BOTTOM;
		 return forTypeAnnotatedExprOnly(that,
				 expr_result,
				 errorMsg("Attempt to assume type ", assumedType,
						 " from non-subtype ", exprType));
	 }

	 @Override
	 public TypeCheckerResult forAssignment(Assignment that) {
		 // The procedures for assignment differ greatly depending on the type of the LHS
		 final Option<TypeCheckerResult> opr_result = recurOnOptionOfOpRef(that.getOpr());
		 final TypeCheckerResult rhs_result = that.getRhs().accept(this);

		 // Check subexprs
		 if( rhs_result.type().isNone() || (opr_result.isSome() && opr_result.unwrap().type().isNone()) ) {
			 if( opr_result.isSome() )
				 return TypeCheckerResult.compose(that, subtypeChecker, rhs_result, opr_result.unwrap());
			 else
				 return TypeCheckerResult.compose(that, subtypeChecker, rhs_result);
		 }

		 // create a tuple of inference vars the same size as the LHS list
		 // then we will contstraint it to be a subtype of the rhs and each
		 // element a super type of each lhs element.
		 List<Type> inf_types = NodeFactory.make_InferenceVarTypes(that.getSpan(),that.getLhs().size());
		 Type inf_tuple = NodeFactory.makeTupleType(inf_types);
		 TypeCheckerResult tuple_result = this.checkSubtype(rhs_result.type().unwrap(), inf_tuple, that);

		 List<TypeCheckerResult> all_results = new LinkedList<TypeCheckerResult>();
		 all_results.add(tuple_result);
		 all_results.add(rhs_result);
		 if( opr_result.isSome() ) all_results.add(opr_result.unwrap());

		 // Go through each lhs, and typecheck it with our visitor, which handles each subtype
		 // of LHS differently.
		 final Iterator<Type> inf_type_iter = inf_types.iterator();
		 for( final Lhs lhs : that.getLhs() ) {
			 final Type rhs_type = inf_type_iter.next();

			 NodeDepthFirstVisitor<TypeCheckerResult> visitor =
				 new NodeDepthFirstVisitor<TypeCheckerResult>() {
				 @Override
				 public TypeCheckerResult forFieldRef(FieldRef that) {
					 // If there is an op, we must typecheck that as a normal read reference
					 TypeCheckerResult read_result;
					 if( opr_result.isSome() )
						 read_result = that.accept(TypeChecker.this);
					 else
						 read_result = new TypeCheckerResult(that);

					 TypeCheckerResult obj_result = that.getObj().accept(TypeChecker.this);
					 if(obj_result.type().isSome()){
						 Type obj_type = obj_result.type().unwrap();
						 List<TraitType> traits = traitTypesCallable(obj_type);
						 
						 TypeCheckerResult r = findSetterInTraitHierarchy(that.getField(),traits,rhs_type, that);
						 return TypeCheckerResult.compose(that, read_result.type(), subtypeChecker, r, read_result);
					 }
					 else{
						 return obj_result;
					 }
				 }
				 // The two cases for variables are pretty similar
				 @Override
				 public TypeCheckerResult forLValueBind(LValueBind that) {
					 TypeCheckerResult r = that.accept(TypeChecker.this);
					 if( r.type().isNone() ) return r;
					 Type lhs_type = r.type().unwrap();
					 TypeCheckerResult r_sub = checkSubtype(rhs_type,lhs_type,that);
					 // make sure it's immutable
					 if( !that.isMutable() ) {
						 String err = "Variable " + that + " is immutable.";
						 TypeCheckerResult e_r = new TypeCheckerResult(that, TypeError.make(err, that));
						 return TypeCheckerResult.compose(that, subtypeChecker, r, e_r);
					 }
					 else {
						 // Happy path
						 return TypeCheckerResult.compose(that, lhs_type, subtypeChecker, r, r_sub);
					 }
				 }

				 @Override
				 public TypeCheckerResult forSubscriptExpr(SubscriptExpr that) {
					 // If there is an op, we must typechecker that as a normal read reference
					 TypeCheckerResult read_result;
					 if( opr_result.isSome() )
						 read_result = that.accept(TypeChecker.this);
					 else
						 read_result = new TypeCheckerResult(that);

					 // make sure there is a subscript setter for type
					 // This method is very similar to forSubscriptExprOnly in Typechecker
					 // except that we must graft the new RHS type onto the end of subs_types
					 // to see if there is an appropriate setter method.
					 TypeCheckerResult obj_result = that.getObj().accept(TypeChecker.this);
					 List<TypeCheckerResult> subs_result = TypeChecker.this.recurOnListOfExpr(that.getSubs());
					 // ignore op_result...
					 // Option<TypeCheckerResult> op_result = TypeChecker.this.recurOnOptionOfEnclosing(that.getOp());
					 List<TypeCheckerResult> staticArgs_result = TypeChecker.this.recurOnListOfStaticArg(that.getStaticArgs());

					 TypeCheckerResult all_result =
						 TypeCheckerResult.compose(that, subtypeChecker, obj_result, read_result,
								 TypeCheckerResult.compose(that, subtypeChecker, subs_result),
								 TypeCheckerResult.compose(that, subtypeChecker, staticArgs_result));

					 if( obj_result.type().isNone() ) return all_result;
					 for( TypeCheckerResult r : subs_result )
						 if( r.type().isNone() ) return all_result;

					 // get types
					 Type obj_type = obj_result.type().unwrap();
					 List<Type> subs_types = CollectUtil.makeList(IterUtil.map(subs_result, new Lambda<TypeCheckerResult,Type>(){
						 public Type value(TypeCheckerResult arg0) { return arg0.type().unwrap(); }}));
					 // put rhs type on the end
					 subs_types = Useful.concat(subs_types, Collections.singletonList(rhs_type));

					 TypeCheckerResult final_result =
						 TypeChecker.this.subscriptHelper(that, that.getOp(), obj_type, subs_types, that.getStaticArgs());
					 return TypeCheckerResult.compose(that, read_result.type(), subtypeChecker, final_result, all_result);
				 }
				 @Override
				 public TypeCheckerResult forVarRef(VarRef that) {
					 TypeCheckerResult r = that.accept(TypeChecker.this);
					 if( r.type().isNone() ) return r;
					 Type lhs_type = r.type().unwrap();
					 TypeCheckerResult r_sub = checkSubtype(rhs_type,lhs_type,that);
					 TypeEnv env = that.getVar().getApi().isSome() ?
							 returnTypeEnvForApi(that.getVar().getApi().unwrap()) :
								 typeEnv;
							 Option<BindingLookup> bl = env.binding(that.getVar());
							 if( bl.isNone() ) return r;
							 // make sure it's immutable
							 if( !(bl.unwrap().isMutable()) ) {
								 String err = "Variable " + that + " is immutable.";
								 TypeCheckerResult e_r = new TypeCheckerResult(that, TypeError.make(err, that));
								 return TypeCheckerResult.compose(that, subtypeChecker, r, e_r);
							 }
							 else {
								 // Happy path
								 return TypeCheckerResult.compose(that, lhs_type, subtypeChecker, r, r_sub);
							 }
				 }
			 };
			 TypeCheckerResult lhs_result = lhs.accept(visitor);

			 // if we need to check the operator, just try to apply it like a normal method.
			 if( lhs_result.type().isSome() && that.getOpr().isSome() && opr_result.unwrap().type().isSome() ) {
				 Type lhs_type = lhs_result.type().unwrap();
				 Type opr_type = opr_result.unwrap().type().unwrap();
				 Option<Pair<Type,ConstraintFormula>> application_result =
					 TypesUtil.applicationType(subtypeChecker, opr_type, new ArgList(lhs_type, rhs_type));

				 if( application_result.isSome() ) {
					 ConstraintFormula constr = application_result.unwrap().second();
					 all_results.add(new TypeCheckerResult(that, constr));
				 }
				 else {
					 String err = "Compound operator, " + that.getOpr().unwrap().getOriginalName() + " not defined on types (" + lhs_type + "," +rhs_type + ").";
					 all_results.add(new TypeCheckerResult(that, TypeError.make(err, that)));
				 }
			 }

			 all_results.add(lhs_result);
		 }
		 return TypeCheckerResult.compose(that, Types.VOID, subtypeChecker, all_results);
	 }

	 private TypeCheckerResult forAtomic(Expr body, final String errorMsg) {
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
		 return body.accept(newChecker);
		 //return TypeCheckerResult.compose(that, bodyResult.type(), subtypeChecker, bodyResult);
	 }
//
//	 private TypeCheckerResult forAtomic(Node that, Expr body, final String errorMsg) {
//		 TypeChecker newChecker = new TypeChecker(table,
//				 staticParamEnv,
//				 typeEnv,
//				 compilationUnit,
//				 subtypeChecker,
//				 labelExitTypes) {
//			 @Override public TypeCheckerResult forSpawn(Spawn that) {
//				 // Use TypeChecker's forSpawn method, but compose an error onto the result
//				 return TypeCheckerResult.compose(
//						 that,
//						 subtypeChecker,
//						 new TypeCheckerResult(that,
//								 TypeError.make(errorMsg,
//										 that)), that.accept(TypeChecker.this));
//			 }
//		 };
//		 TypeCheckerResult bodyResult = body.accept(newChecker);
//		 return TypeCheckerResult.compose(that, bodyResult.type(), subtypeChecker, bodyResult);
//	 }

	 @Override
	 public TypeCheckerResult forAtomicExpr(AtomicExpr that) {
		 return forAtomic(
				 that.getExpr(),
				 errorMsg("A 'spawn' expression must not occur inside an 'atomic' expression."));
	 }

	 @Override
	 public TypeCheckerResult forBigFixity(BigFixity that) {
		 // No checks needed to be performed on a BigFixity.
		 return new TypeCheckerResult(that);
	 }

	 @Override
	 public TypeCheckerResult forBlockOnly(Block that, Option<TypeCheckerResult> exprType_result, List<TypeCheckerResult> exprs_result) {
		 // Type is type of last expression or void if none.
		 if (exprs_result.isEmpty()) {
			 return TypeCheckerResult.compose(that, Types.VOID, subtypeChecker, exprs_result);
		 } else {
			 List<TypeCheckerResult> all_results = allVoidButLast(exprs_result,that.getExprs());
			 return TypeCheckerResult.compose(that, exprs_result.get(exprs_result.size()-1).type(), subtypeChecker, all_results);
		 }
	 }

	 @Override
	 public TypeCheckerResult forCaseExpr(CaseExpr that) {
		 Option<TypeCheckerResult> param_result = this.recurOnOptionOfExpr(that.getParam());
		 Option<TypeCheckerResult> compare_result = this.recurOnOptionOfOpRef(that.getCompare());
		 TypeCheckerResult equalsOp_result = that.getEqualsOp().accept(this);
		 TypeCheckerResult inOp_result = that.getInOp().accept(this);
		 NodeDepthFirstVisitor<Pair<TypeCheckerResult,TypeCheckerResult>> temp = new NodeDepthFirstVisitor<Pair<TypeCheckerResult,TypeCheckerResult>>() {
			 @Override
			 public Pair<TypeCheckerResult, TypeCheckerResult> forCaseClause(
					 CaseClause that) {
				 return Pair.make(that.getMatch().accept(TypeChecker.this), that.getBody().accept(TypeChecker.this));
			 }
		 };
		 List<Pair<TypeCheckerResult,TypeCheckerResult>> clauses_result = temp.recurOnListOfCaseClause(that.getClauses());
		 Option<TypeCheckerResult> elseClause_result = this.recurOnOptionOfBlock(that.getElseClause());

		 List<TypeCheckerResult> all_results=new ArrayList<TypeCheckerResult>();
		 boolean failed=false;

		 // Checker that clauses all typechecked properly
		 for(Pair<TypeCheckerResult,TypeCheckerResult> clause_result: clauses_result){
			 if(clause_result.first().type().isNone() || clause_result.second().type().isNone()){
				 failed=true;
			 }
			 all_results.add(TypeCheckerResult.compose(that, subtypeChecker,
					 TypeCheckerResult.compose(that, subtypeChecker, CollectUtil.makeList(IterUtil.pairFirsts(clauses_result))),
					 TypeCheckerResult.compose(that, subtypeChecker, CollectUtil.makeList(IterUtil.pairSeconds(clauses_result)))));
		 }


		 // Check that compare typechecked, if it exists
		 if( compare_result.isSome()) {
			 if(compare_result.unwrap().type().isNone()){
				 failed=true;
			 }
			 all_results.add(compare_result.unwrap());
		 }
		 // Check elseClause
		 if(elseClause_result.isSome()){
			 if(elseClause_result.unwrap().type().isNone()){
				 failed=true;
			 }
			 all_results.add(elseClause_result.unwrap());
		 }
		 // Check if we are dealing with a normal case (i.e. not an extremum)
		 if (that.getParam().isSome()) {
			 all_results.add(param_result.unwrap());
			 if(param_result.unwrap().type().isNone()){
				 failed=true;
			 }

			 if(equalsOp_result.type().isNone() || inOp_result.type().isNone()){
				 return bug("Equals or In does not have a type");
			 }

			 if(failed){
				 return TypeCheckerResult.compose(that,this.subtypeChecker,all_results);
			 }

			 return forCaseExprNormalOnly(that, all_results, param_result.unwrap(), compare_result, equalsOp_result, inOp_result,clauses_result,elseClause_result);
		 } else {
			 if(failed){
				 return TypeCheckerResult.compose(that,this.subtypeChecker,all_results);
			 }
			 return forExtremumOnly(that, all_results, compare_result.unwrap() ,clauses_result);
		 }
	 }

	 // Handle regular case expressions
	 private TypeCheckerResult forCaseExprNormalOnly(CaseExpr that,
			 List<TypeCheckerResult> all_results, // known to be mutable
			 TypeCheckerResult param_result,
			 Option<TypeCheckerResult> compare_result,
			 TypeCheckerResult equalsOp_result, TypeCheckerResult inOp_result,
			 List<Pair<TypeCheckerResult,TypeCheckerResult>> clauses_result,
			 Option<TypeCheckerResult> elseClause_result) {
		 Type param_type = param_result.type().unwrap();

		 List<Type> clause_types = new ArrayList<Type>(that.getClauses().size()+1);
		 for(Pair<TypeCheckerResult,TypeCheckerResult> clause_result: clauses_result){
			 Type clause_type = clause_result.first().type().unwrap();

			 clause_types.add(clause_result.second().type().unwrap());


			 Option<Pair<Type,ConstraintFormula>> application;
			 ArgList args = new ArgList(param_type, clause_type);
			 String err;

			 if( that.getCompare().isSome() ) {
				 application = TypesUtil.applicationType(subtypeChecker,
						 compare_result.unwrap().type().unwrap(), args);

				 err = "Could not find overloading of " + that.getCompare().unwrap().getOriginalName() +
				 " applicable for arguments of type " + param_type + " and " + clause_type + ".";
			 }
			 else {
				 // Check both = and IN operators
				 // we first want to do <: generator test.
				 // If both are sat, we use =, if only gen_subtype_c is sat, we use IN
				 ConstraintFormula gen_subtype_c =
					 subtypeChecker.subtype(clause_type, Types.makeGeneratorType(NodeFactory.make_InferenceVarType(clause_result.first().ast().getSpan())));
				 ConstraintFormula gen_subtype_p =
					 subtypeChecker.subtype(param_type, Types.makeGeneratorType(NodeFactory.make_InferenceVarType(clause_result.first().ast().getSpan())));

				 if( gen_subtype_c.isSatisfiable() && !gen_subtype_p.isSatisfiable() ) {
					 // Implicit IN
					 application = TypesUtil.applicationType(subtypeChecker, inOp_result.type().unwrap(), args);
					 err = "Could not find overloading of " + that.getInOp().getOriginalName() +
					 " applicable for arguments of type " + param_type + " and " + clause_type + ".";
				 }
				 else {
					 // Implicit =
					 application = TypesUtil.applicationType(subtypeChecker, equalsOp_result.type().unwrap(), args);
					 err = "Could not find overloading of " + that.getEqualsOp().getOriginalName() +
					 " applicable for arguments of type " + param_type + " and " + clause_type + ".";
				 }
			 }

			 Node lhs_node = clause_result.first().ast();
			 if( application.isNone() || application.unwrap().second().isFalse() ) {
				 // NOTHIN
				 TypeCheckerResult e_result = new TypeCheckerResult(that, TypeError.make(err, lhs_node));
				 all_results.add(e_result);
			 }
			 else {
				 // Result of application must be a boolean
				 Type app_type = application.unwrap().first();
				 TypeCheckerResult bool_result = this.checkSubtype(app_type, Types.BOOLEAN, lhs_node,
				 "Result of applying = op to param and clause must have Boolean type.");

				 TypeCheckerResult result = new TypeCheckerResult(that, application.unwrap().second());

				 all_results.add(bool_result);
				 all_results.add(result);
			 }
		 }

		 if( that.getElseClause().isSome() ) {
			 clause_types.add(elseClause_result.unwrap().type().unwrap());
		 }
		 // The join of all clause rhs types is the type of the CaseExpr
		 Type result_type = this.subtypeChecker.join(clause_types);
		 return TypeCheckerResult.compose(that, result_type, subtypeChecker, all_results);
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
		 return TypeCheckerResult.compose(that, subtypeChecker.join(clause_types), 
				                          subtypeChecker, clause_results).addNodeTypeEnvEntry(that, typeEnv);
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
	 public TypeCheckerResult forChainExpr(ChainExpr that) {
		 List<TypeCheckerResult> all_results = new ArrayList<TypeCheckerResult>();
		 Expr prev = that.getFirst();
		 for(Link link : that.getLinks()){
			 OpRef op = link.getOp();
			 Expr next = link.getExpr();
			 OpExpr tempOpExpr = new OpExpr(new Span(prev.getSpan(),next.getSpan()), false , op, Useful.list(prev, next));
			 TypeCheckerResult result = tempOpExpr.accept(this);
			 all_results.add(result);
			 if(result.type().isSome()){
				 all_results.add(this.checkSubtype(result.type().unwrap(), Types.BOOLEAN, tempOpExpr));
			 }
			 prev=next;
		 }
		 return TypeCheckerResult.compose(that, Types.BOOLEAN,this.subtypeChecker, all_results);
	 }
	 
//	 /** 
//	  * If the given type is a trait, or a type variable that extends some trait,
//	  * or some conjunction/disjunction of things that could have methods called on it,
//	  * return all the TraitIndex-es, otherwise nothing.  
//	  */
//	 private List<TraitIndex> getTypeIndecesFromType(Type rcvr_type) {
//		 if( rcvr_type instanceof NamedType ) {
//			 NamedType named_type = (NamedType)rcvr_type;
//			 // Since it's not in the trait table, let's try the type environment.
//			 Id name = named_type.getName();
//			 
//			 Option<TypeConsIndex> ind = table.typeCons(name);
//			 if( ind.isSome() ) {
//				 if( ind.unwrap() instanceof TraitIndex ) {
//					 return Collections.singletonList((TraitIndex)ind.unwrap());
//				 }
//				 else {
//					 return bug("Shouldn't this be a trait index?");
//				 }
//			 }
//			 // At this point, we don't have a Trait, but we could still have a
//			 // VarType or an intersection: something that is allowed to have method
//			 // calls.
//			 Option<StaticParam> param_ = this.typeEnv.staticParam(name);
//			 
//			 if( param_.isNone() ) return Collections.emptyList();
//			 
//			 StaticParam param = param_.unwrap();
//			 return param.accept(new NodeAbstractVisitor<List<TraitIndex>>(){
//
//				@Override
//				public List<TraitIndex> forTypeParam(TypeParam that) {
//					List<TraitIndex> result = new ArrayList<TraitIndex>();
//					for( BaseType type : that.getExtendsClause() ) {
//						result.addAll(TypeChecker.this.getTypeIndecesFromType(type));
//					}
//					return result;
//				}
//			 });
//		 }
//		 else if( rcvr_type instanceof BoundType ) {
//			 return NI.nyi("You should be able to call methods on this type, but this is nyi." + rcvr_type);
//		 }
//		 else {
//			 return Collections.emptyList();
//		 }
//	 }
	 
	 @Override
	 public TypeCheckerResult forCharLiteralExpr(CharLiteralExpr that) {
		 return new TypeCheckerResult(that, Types.CHAR);
	 }
	 
	 // STUBS -----------------------------
	 @Override
	 public TypeCheckerResult forComponentOnly(Component that,
	         TypeCheckerResult name_result,
	         List<TypeCheckerResult> imports_result,
	         List<TypeCheckerResult> exports_result,
	         List<TypeCheckerResult> decls_result) {

	     Component new_comp = 
	         new Component(that.getSpan(),
	                 that.is_native(),
	                 (APIName)name_result.ast(),
	                 (List<Import>)TypeCheckerResult.astFromResults(imports_result),
	                 (List<Export>)TypeCheckerResult.astFromResults(exports_result),
	                 (List<Decl>)TypeCheckerResult.astFromResults(decls_result));

	     return TypeCheckerResult.compose(new_comp,
	             subtypeChecker,
	             name_result,
	             TypeCheckerResult.compose(new_comp, subtypeChecker, imports_result),
	             TypeCheckerResult.compose(new_comp, subtypeChecker, exports_result), 
	             TypeCheckerResult.compose(new_comp, subtypeChecker, decls_result));
	 }
	 @Override
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
	 
	 public TypeCheckerResult forDoFront(DoFront that) {
	     TypeCheckerResult bodyResult =
	         that.isAtomic() ? forAtomic(
	                 that.getExpr(),
	                 errorMsg("A 'spawn' expression must not occur inside",
	                 "an 'atomic' do block."))
	                 : that.getExpr().accept(this);
	     
	     Option<TypeCheckerResult> loc_result_ = this.recurOnOptionOfExpr(that.getLoc());
	     TypeCheckerResult loc_result = loc_result_.isNone() ? new TypeCheckerResult(that) : 
	                                                           loc_result_.unwrap();
	         
	     TypeCheckerResult region_result = new TypeCheckerResult(that);
	     if (loc_result_.isSome() && loc_result_.unwrap().type().isSome()) {
	         Type locType = loc_result_.unwrap().type().unwrap();
	         region_result = checkSubtype(locType,
	                                      Types.REGION,
	                                      that.getLoc().unwrap(),
	                                      errorMsg("Location of 'do' block must ",
	                                               "have type Region: ", locType));

	     }

	     DoFront new_node = new DoFront(that.getSpan(),
	                                    (Option<Expr>)TypeCheckerResult.astFromResult(loc_result_),
	                                    that.isAtomic(),
	                                    (Block)bodyResult.ast());
	     return TypeCheckerResult.compose(new_node,
	                                      bodyResult.type(),
	                                      subtypeChecker,
	                                      bodyResult,
	                                      loc_result,
	                                      region_result);
	 }
	 
	 @Override
	 public TypeCheckerResult forDoOnly(Do that, Option<TypeCheckerResult> exprType_result, List<TypeCheckerResult> fronts_result) {
		 // Get union of all clauses' types
		 List<Type> frontTypes = new ArrayList<Type>();
		 for (TypeCheckerResult frontResult : fronts_result) {
			 if (frontResult.type().isSome()) {
				 frontTypes.add(frontResult.type().unwrap());
			 }
		 }
		 return TypeCheckerResult.compose(that, subtypeChecker.join(frontTypes), subtypeChecker, fronts_result);
	 }


	 @Override
	 public TypeCheckerResult forEnclosing(Enclosing that) {
		 Option<APIName> api = that.getApi();
		 TypeEnv env = api.isSome() ? returnTypeEnvForApi(api.unwrap()) : this.typeEnv;

		 Option<BindingLookup> binding = env.binding(that);
		 if( binding.isSome() ) {
			 return new TypeCheckerResult(that, binding.unwrap().getType());
		 } else {
			 return new TypeCheckerResult(that,
					 TypeError.make(errorMsg("Enclosing operator not found: ",
							 that),
							 that));
		 }
	 }

	 @Override
	 public TypeCheckerResult forEnclosingFixity(EnclosingFixity that) {
		 // No checks needed to be performed on a EnclosingFixity.
		 return new TypeCheckerResult(that);
	 }
	 
	 @Override
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

	 @Override
	 public TypeCheckerResult forExportOnly(Export that, List<TypeCheckerResult> apis_result) {
		 return new TypeCheckerResult(that);
	 }

	 // Works for extremum expressions
	 // case most < of ... end
	 private TypeCheckerResult forExtremumOnly(CaseExpr that,
			 List<TypeCheckerResult> all_results, // known to be mutable
			 TypeCheckerResult compare_result,
			 List<Pair<TypeCheckerResult,TypeCheckerResult>> clauses_result){

		 Iterable<Type> candidate_types =
			 IterUtil.map(IterUtil.pairFirsts(clauses_result), new Lambda<TypeCheckerResult,Type>(){
				 public Type value(TypeCheckerResult arg0) { return arg0.type().unwrap(); }});

		 Iterable<Type> rhs_types =
			 IterUtil.map(IterUtil.pairSeconds(clauses_result), new Lambda<TypeCheckerResult,Type>(){
				 public Type value(TypeCheckerResult arg0) { return arg0.type().unwrap(); }});

		 Type union_of_candidate_types = this.subtypeChecker.join(candidate_types);
		 Type total_op_order = Types.makeTotalOperatorOrder(union_of_candidate_types, that.getCompare().unwrap().getOriginalName());

		 TypeCheckerResult total_result = this.checkSubtype(union_of_candidate_types, total_op_order, that,
				 "In an extremum expression, the union of all candidate types must be a subtype of " +
				 "TotalOperatorOrder[union,<,<=,>=,>,"+that.getCompare().unwrap().getOriginalName()+"] but it is not. " +
				 "The union is " + union_of_candidate_types);

		 all_results.add(total_result);

		 Type result_type = this.subtypeChecker.join(rhs_types);
		 return TypeCheckerResult.compose(that, result_type,subtypeChecker, total_result);
	 }


	 @Override
	 public TypeCheckerResult forFieldRefOnly(FieldRef that, Option<TypeCheckerResult> exprType_result,
			 TypeCheckerResult obj_result, TypeCheckerResult field_result) {

		 // We need the type of the receiver
		 if( obj_result.type().isNone() ) {
			 return TypeCheckerResult.compose(that, subtypeChecker, obj_result);
		 }
		 //check whether receiver can have fields
		 Type recvr_type=obj_result.type().unwrap();
		 TypeCheckerResult result;
		 Option<Type> result_type;
		 
		 List<TraitType> trait_types = traitTypesCallable(recvr_type);
		 
		 if( !trait_types.isEmpty() ) {
			 Option<Type> f_type = this.findFieldInTraitHierarchy(trait_types, that);
			 if( f_type.isSome() ) {
				 result = new TypeCheckerResult(that);
				 result_type = f_type;
			 }
			 else {
				 String no_field = "Field " + that.getField() + " could not be found in type" +
				 recvr_type + ".";
				 result = new TypeCheckerResult(that, TypeError.make(no_field, that));
				 result_type = Option.none();
			 }
		 }
		 else {
			 String not_trait = "Receiver of field expression must be a trait or object, but "
				 + recvr_type + " is neither.";
			 result = new TypeCheckerResult(that, TypeError.make(not_trait, that));
			 result_type = Option.none();
		 }
		 return TypeCheckerResult.compose(that, result_type, this.subtypeChecker, obj_result, result);
	 }

	 // TRIVIAL NODES ---------------------
	 @Override
	 public TypeCheckerResult forFloatLiteralExpr(FloatLiteralExpr that) {
		 return new TypeCheckerResult(that, Types.FLOAT_LITERAL);
	 }

	 @Override
	 public TypeCheckerResult forFnDef(FnDef that) {
		 TypeChecker newChecker = this.extend(that.getStaticParams(), that.getParams(), that.getWhere());

		 TypeCheckerResult contractResult = that.getContract().accept(newChecker);
		 TypeCheckerResult bodyResult = that.getBody().accept(newChecker);
		 TypeCheckerResult result = new TypeCheckerResult(that);

		 if( !contractResult.isSuccessful() || !bodyResult.isSuccessful() ) {
			 return TypeCheckerResult.compose(that, subtypeChecker, result, bodyResult, contractResult);
		 }



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

		 FnDef new_node = new FnDef(that.getSpan(),
				 that.getMods(),
				 that.getName(),
				 that.getStaticParams(),
				 that.getParams(),
				 returnType,
				 that.getThrowsClause(),
				 that.getWhere(),
				 (Contract)contractResult.ast(),
				 that.getSelfName(),
				 (Expr)bodyResult.ast());
		 return TypeCheckerResult.compose(new_node, subtypeChecker, contractResult,
				 bodyResult, result).addNodeTypeEnvEntry(new_node, typeEnv);
	 }

	 @Override
	 public TypeCheckerResult forFnExpr(FnExpr that) {
		 // Fn expressions have arrow type. They cannot have static arguments.
		 // They cannot have where clauses.

		 Option<TypeCheckerResult> returnType_result = recurOnOptionOfType(that.getReturnType());

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
		 //get domain type
		 List<Type> dlist = new ArrayList<Type>();
		 Boolean varargs=false;
		 for(Param p: that.getParams()){
			 if(p instanceof NormalParam){
				 NormalParam n = (NormalParam)p;
				 if(n.getType().isSome()){
					 dlist.add(n.getType().unwrap());
				 }
				 else{
					 NI.nyi();
				 }
			 }
			 if(p instanceof VarargsParam){
				 VarargsParam v = (VarargsParam) p;
				 dlist.add(v.getType());
				 varargs=true;
			 }
		 }
		 AbstractTupleType domain;
		 if(varargs){
			 Type var = dlist.remove(dlist.size()-1);
			 domain = new VarargTupleType(dlist,var);
		 }
		 else{
			 domain = NodeFactory.makeTupleType(dlist);
		 }

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
				 all_results.add(this.checkSubtype(exn, Types.CHECKED_EXCEPTION, that,
						 "Types in throws clause must be subtypes of CheckedException, but "+
						 exn + " is not."));
			 }
		 }

		 ArrowType arr = NodeFactory.makeArrowType(new Span(), domain, return_type);
		 FnExpr new_node = new FnExpr(that.getSpan(),
				                      that.isParenthesized(),
				                      that.getName(),
				                      that.getStaticParams(),
				                      that.getParams(),
				                      (Option<Type>)TypeCheckerResult.astFromResult(returnType_result),
				                      that.getWhere(),
				                      (Option<List<BaseType>>)TypeCheckerResult.astFromResults(throwsClause_result),
				                      (Expr)body_result.ast() );
		 
		 return TypeCheckerResult.compose(new_node, arr, 
				 this.subtypeChecker, all_results).addNodeTypeEnvEntry(new_node, typeEnv);
	 }

	 @Override
	 public TypeCheckerResult forFnRefOnly(FnRef that, Option<TypeCheckerResult> exprType_result, 
			 TypeCheckerResult originalName_result,
			 List<TypeCheckerResult> fns_result,
			 List<TypeCheckerResult> staticArgs_result) {

//		 Get intersection of overloaded function types.
		 LinkedList<Type> overloadedTypes = new LinkedList<Type>();
		 for (TypeCheckerResult fn_result : fns_result) {
			 if (fn_result.type().isSome()) {
				 // This is the ONLY location where we have access to the static arguments that were
				 // explicitly passed, if there are any, so we should instantiate those arguments, and
				 // remove any that cannot possibly match.
				 Option<Type> instantiated_type =
					 TypesUtil.applyStaticArgsIfPossible(fn_result.type().unwrap(),that.getStaticArgs());

				 if( instantiated_type.isSome() )
					 overloadedTypes.add(instantiated_type.unwrap());
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
		 Option<TypeCheckerResult> type_result = recurOnOptionOfType(that.getExprType());
		 TypeChecker extend = this.extend(pair.second());
		 TypeCheckerResult body_result = that.getBody().accept(extend);
		 return forForOnly(that, type_result, pair.first(), body_result);
	 }

	 @Override
	 public TypeCheckerResult forForOnly(For that, Option<TypeCheckerResult> exprType_result, List<TypeCheckerResult> gens_result, TypeCheckerResult body_result) {

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

		 return TypeCheckerResult.compose(that,Types.VOID, subtypeChecker,
				                          all_results).addNodeTypeEnvEntry(that, typeEnv);
	 }





	 @Override
	 public TypeCheckerResult forGeneratedExpr(GeneratedExpr that) {
		 Pair<List<TypeCheckerResult>,List<LValueBind>> pair = recurOnListsOfGeneratorClauseBindings(that.getGens());
		 TypeChecker extend = this.extend(pair.second());
		 TypeCheckerResult body_result = that.getExpr().accept(extend);
		 //make sure body has type void?
		List<TypeCheckerResult> res = pair.first();
		res.add(body_result);
		if( !body_result.type().unwrap().equals(Types.VOID) ) {
			res.add(new TypeCheckerResult(that,TypeError.make("Body of generated expression must have type (), but had type " +
					body_result.type().unwrap(), that.getExpr())));
		}
		return TypeCheckerResult.compose(that,Types.VOID,
				subtypeChecker, res).addNodeTypeEnvEntry(that, typeEnv);
	 }


	 private Pair<TypeCheckerResult, List<LValueBind>> forGeneratorClauseGetBindings(GeneratorClause that,
			 boolean mustBeCondition) {
		 // We just don't visit the Ids at all, and let a different pass handle shadowing
		 TypeCheckerResult init_result = that.getInit().accept(this);
		 return forGeneratorClauseOnlyGetBindings(that, init_result, mustBeCondition);
	 }

	 // In the end, this whole method may be pointless. We probably want a method that will
	 // return the bindings so that other methods can actually use this.
	 @Override
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
				 // Because generator_type is almost certainly a _InferenceVar, we have to declare a new tuple
				 // that is the size of the binding and declare one to be a sub-type of the other.
				 List<Type> inference_vars = new ArrayList<Type>(bindings_count);
				 for( int i = 0; i<bindings_count;i++ ) {
					 inference_vars.add(NodeFactory.make_InferenceVarType(that.getBind().get(i).getSpan()));
				 }

				 String tup_err_msg = "If more than one variable is bound in a generator, generator must have tuple type "+
				 "but " + that.getInit() + " does not or has different number of arguments.";

				 TypeCheckerResult tuple_result =
					 this.checkSubtype(generator_type, Types.makeTuple(inference_vars), that.getInit(), tup_err_msg);
				 TypeCheckerResult tuple_result2 =
					 this.checkSubtype(Types.makeTuple(inference_vars), generator_type, that.getInit(), tup_err_msg);

				 this_result = TypeCheckerResult.compose(that, subtypeChecker, tuple_result, tuple_result2, this_result);
				 // Now just create the lvalues with the newly created inference variable type
				 Iterator<Id> id_iter = that.getBind().iterator();
				 result_bindings = new ArrayList<LValueBind>(bindings_count);
				 for( Type inference_var : inference_vars ) {
					 result_bindings.add(NodeFactory.makeLValue(id_iter.next(), inference_var));
				 }
			 }
			 return Pair.make(TypeCheckerResult.compose(that, subtypeChecker, this_result, init_result), result_bindings);
	 }

	 @Override
	 public TypeCheckerResult forId(Id name) {
		 Option<APIName> apiName = name.getApi();
		 if (apiName.isSome()) {
			 APIName api = apiName.unwrap();
			 TypeEnv apiTypeEnv = returnTypeEnvForApi(api);

			 Option<Type> type = apiTypeEnv.type(name);
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

	 @Override
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
		return TypeCheckerResult.compose(that, body_result.type(), 
				subtypeChecker, test_result, body_result, result).addNodeTypeEnvEntry(that, typeEnv);
	 }

	 @Override
    public TypeCheckerResult forIfOnly(If that, Option<TypeCheckerResult> exprType_result,
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
	 public TypeCheckerResult forImportStar(ImportStar that) {
		 // No checks needed since all imports are handled by the trait table.
		 return new TypeCheckerResult(that);
	 }

	 @Override
	 public TypeCheckerResult forInFixity(InFixity that) {
		 // No checks needed to be performed on a InFixity.
		 return new TypeCheckerResult(that);
	 }

	 @Override
	 public TypeCheckerResult forIntArgOnly(IntArg that,
			 TypeCheckerResult val_result) {
		 return new TypeCheckerResult(that);
	 }

	 @Override
	 public TypeCheckerResult forIntLiteralExpr(IntLiteralExpr that) {
		 return new TypeCheckerResult(that, Types.INT_LITERAL);
	 }

	 @Override
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
		 return TypeCheckerResult.compose(that, labelType,
				                          subtypeChecker, bodyResult).addNodeTypeEnvEntry(that, typeEnv);
	 }

	 @Override
	 public TypeCheckerResult forLetFn(LetFn that) {
		 TypeCheckerResult result = new TypeCheckerResult(that);
		 Relation<IdOrOpOrAnonymousName, FnDef> fnDefs = new IndexedRelation<IdOrOpOrAnonymousName, FnDef>(false);
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
						 subtypeChecker, allVoidButLast(newChecker.recurOnListOfExpr(that.getBody()),that.getBody())));
		 
		 return result.addNodeTypeEnvEntry(that, typeEnv);
	 }

	 @Override
	 public TypeCheckerResult forLocalVarDecl(LocalVarDecl that) {
		 TypeCheckerResult result = new TypeCheckerResult(that);

		 // Create type for LHS
		 Type lhs_type = 
			 Types.MAKE_TUPLE.value(IterUtil.map(that.getLhs(), new Lambda<LValue,Type>(){
				 public Type value(LValue arg0) { return getTypeOfLValue(arg0); }}));

		 Option<TypeCheckerResult> _rhs_result = this.recurOnOptionOfExpr(that.getRhs());
		 if ( _rhs_result.isSome() ) {
			 TypeCheckerResult rhs_result = _rhs_result.unwrap();
			 result = TypeCheckerResult.compose(that, subtypeChecker, result, rhs_result);

			 if( rhs_result.type().isSome() ) {
				 result = TypeCheckerResult.compose(that, subtypeChecker, result,
						 checkSubtype(rhs_result.type().unwrap(), lhs_type, that, "RHS must be a subtype of LHS."));
			 }
		 }

		 TypeChecker newChecker = this.extend(that);
		 // A LocalVarDecl is like a let. It has a body, and it's type is the type of the body
		 List<TypeCheckerResult> body_results = newChecker.recurOnListOfExpr(that.getBody());
		 Option<Type> body_type = that.getBody().size() == 0 ?
				 Option.<Type>some(Types.VOID) :
					 body_results.get(body_results.size()-1).type();
				 
		LocalVarDecl new_node = new LocalVarDecl(that.getSpan(),
		                                         that.isParenthesized(),
		                                         (List<Expr>)TypeCheckerResult.astFromResults(body_results),
		                                         that.getLhs(),
		                                         (Option<Expr>)TypeCheckerResult.astFromResult(_rhs_result));
		return TypeCheckerResult.compose(new_node,
				body_type, subtypeChecker,
				result, TypeCheckerResult.compose(new_node,
						subtypeChecker, allVoidButLast(body_results,that.getBody()))).addNodeTypeEnvEntry(new_node, typeEnv);
	 }

	 @Override
	 public TypeCheckerResult forLooseJuxtOnly(LooseJuxt that, Option<TypeCheckerResult> exprType_result, 
			 TypeCheckerResult multiJuxt_result,
			 TypeCheckerResult infixJuxt_result,
			 List<TypeCheckerResult> exprs_result) {
		 // The implementation of this method is very similar to tight juxt except
		 // the ordering of association is different.
		 // Notice also that tightJuxt has to be recursive, but loose juxt is not, due to specification.

		 // Did any subexpressions fail to typecheck?
		 for( TypeCheckerResult r : exprs_result ) {
			 if( r.type().isNone())
				 return TypeCheckerResult.compose(that, subtypeChecker, exprs_result);
		 }

		 if( that.getExprs().size() != exprs_result.size() ) {
			 bug("Number of types don't match number of sub-expressions");
		 }
		 // Specification describes chunks, which are elements group together. Chunking process goes first.
		 List<Pair<TypeCheckerResult,Expr>> checked_chunks = new LinkedList<Pair<TypeCheckerResult,Expr>>();
		 {
			 List<Pair<TypeCheckerResult,Expr>> cur_chunk = new LinkedList<Pair<TypeCheckerResult,Expr>>();
			 Iterator<Expr> expr_iter = that.getExprs().iterator();
			 boolean seen_non_fn = false;
			 // First the loose juxtaposition is broken into nonempty chunks; wherever there is a non-function element followed
			 // by a function element, the latter begins a new chunk. Thus a chunk consists of some number (possibly zero) of
			 // functions followed by some number (possibly zero) of non-functions.
			 for( TypeCheckerResult r : exprs_result ) {
				 boolean is_arrow = TypesUtil.isArrows(r.type().unwrap());
				 if( is_arrow && seen_non_fn ) {
					 // finished last chunk
					 Pair<TypeCheckerResult,Expr> checked_chunk = this.checkChunk(cur_chunk, that.getInfixJuxt());
					 checked_chunks.add(checked_chunk);
					 cur_chunk.clear();
					 seen_non_fn = false;
				 }
				 if( is_arrow ){
					 cur_chunk.add(Pair.make(r, expr_iter.next()));
				 }
				 else {
					 seen_non_fn = true;
					 cur_chunk.add(Pair.make(r, expr_iter.next()));
				 }
			 }
			 // Last chunk needs to be checked, if there is one
			 if( !cur_chunk.isEmpty() ) {
				 checked_chunks.add(checkChunk(cur_chunk, that.getInfixJuxt()));
			 }
		 }
		 // After chunking
		 List<Expr> new_juxt_exprs = CollectUtil.makeList(IterUtil.pairSeconds(checked_chunks));
		 List<TypeCheckerResult> new_juxt_results = CollectUtil.makeList(IterUtil.pairFirsts(checked_chunks));

		 if( checked_chunks.size() == 1 ) {
			 Expr expr = IterUtil.first(new_juxt_exprs);
			 TypeCheckerResult expr_result = expr.accept(this); // Is it bad to re-typecheck all args?
			 return TypeCheckerResult.compose(expr, expr_result.type(), subtypeChecker, expr_result,
					 TypeCheckerResult.compose(expr, subtypeChecker, new_juxt_results));
		 }
		 // (1) If any element that remains has type String, then it is a static error if any two adjacent elements are not of type String.
		 // TODO: Separate pass?
		 // (2) Treat the sequence that remains as a multifix application of the juxtaposition operator. The rules for multifix operators then apply:
		 OpExpr multi_op_expr = new OpExpr(that.getSpan(), that.getMultiJuxt(), new_juxt_exprs);
		 TypeCheckerResult multi_op_result = multi_op_expr.accept(this);
		 if( multi_op_result.type().isSome() ) {
			 return TypeCheckerResult.compose(multi_op_expr, multi_op_result.type(), subtypeChecker,
					 TypeCheckerResult.compose(multi_op_expr, subtypeChecker, new_juxt_results));
		 }
		 // if an applicable method cannot be found for the entire expression, then it is left-associated.
		 Iterator<Expr> expr_iter = new_juxt_exprs.iterator();
		 Expr expr_1 = expr_iter.next(); // the fact that >= two items are here is guaranteed from above.
		 Expr expr_2 = expr_iter.next();
		 OpExpr cur_op_expr = new OpExpr(new Span(expr_1.getSpan(),expr_2.getSpan()), that.getInfixJuxt(), Useful.list(expr_1,expr_2));
		 while( expr_iter.hasNext() ) {
			 Expr next_expr = expr_iter.next();
			 cur_op_expr = new OpExpr(new Span(cur_op_expr.getSpan(),next_expr.getSpan()), that.getInfixJuxt(), Useful.list(cur_op_expr, next_expr));
		 }
		 // typecheck this result instead
		 TypeCheckerResult op_expr_result = cur_op_expr.accept(this); // Is it bad to re-typecheck all args?
		 return TypeCheckerResult.compose(cur_op_expr, op_expr_result.type(), subtypeChecker, op_expr_result,
				 TypeCheckerResult.compose(cur_op_expr, subtypeChecker, new_juxt_results));
	 }

	 // Math primary, which is the more general case, is going to be called for both TightJuxt and MathPrimary
	 @Override
	 public TypeCheckerResult forMathPrimary(MathPrimary that) {

		 // Base case of recursion: If there is no 'rest', return the Expr
		 Expr front = that.getFront();
		 if( that.getRest().isEmpty() ) {
			 return front.accept(this);
		 }
		 // See if simple static errors exist
		 Option<TypeCheckerResult> static_result = exponentiationStaticCheck(that, that.getRest());
		 if( static_result.isSome() ) {
			 return static_result.unwrap();
		 }

		 // HANDLE THE FRONT ITEM
		 {
			 // Create a new list of MathItems that is a copy of the old one
			 List<MathItem> new_items = new ArrayList<MathItem>(that.getRest());
			 ListIterator<MathItem> item_iter = new_items.listIterator();

			 TypeCheckerResult front_result = front.accept(this);
			 MathItem first_of_rest = item_iter.next();
			 if( front_result.type().isNone() )
				 return front_result;
			 // If front is a fn followed by an expr, we reassociate
			 if( TypesUtil.isArrows(front_result.type().unwrap()) && isExprMI(first_of_rest) ) {
				 MathItem arg = first_of_rest;
				 // It is a static error if either the argument is not parenthesized,
				 Option<TypeCheckerResult> is_error = expectParenedExprItem(arg);
				 if( is_error.isSome() ) {
					 return is_error.unwrap();
				 }
				 item_iter.remove(); // remove arg from item list
				 // static error if the argument is immediately followed by a non-expression element.
				 if( item_iter.hasNext() ) {
					 Option<TypeCheckerResult> is_expr_error = expectExprMI(item_iter.next());
					 if( is_expr_error.isSome() ) {
						 return is_expr_error.unwrap();
					 }
				 }
				 // Otherwise, make a new MathPrimary that is one element shorter, and recur
				 _RewriteFnApp fn = new _RewriteFnApp(new Span(front.getSpan(),arg.getSpan()), front, ((ExprMI)arg).getExpr());
				 MathPrimary new_primary = new MathPrimary(that.getSpan(),that.isParenthesized(),that.getMultiJuxt(),that.getInfixJuxt(),fn,new_items);
				 return new_primary.accept(this);
			 }
		 }
		 // THE FRONT ITEM WAS NOT A FN FOLLOWED BY AN EXPR, REASSOCIATE REST
		 {

			 Expr last_expr_ = front;
			 boolean last_expr_is_front = true;
			 // Create a new list of MathItems that is a copy of the old one
			 List<MathItem> new_items = new ArrayList<MathItem>(that.getRest());
			 ListIterator<MathItem> item_iter = new_items.listIterator();

			 while( item_iter.hasNext() ) {
				 MathItem cur_item = item_iter.next();
				 final Expr last_expr = last_expr_;
				 // For each expression element determine whether it is a function immediately followed by an expr
				 if( isExprMI(cur_item) ) {
					 TypeCheckerResult expr_result = ((ExprMI)cur_item).getExpr().accept(this);
					 if( expr_result.type().isNone() )
						 return expr_result;

					 Option<MathItem> next_item = item_iter.hasNext() ? Option.<MathItem>some(item_iter.next()) : Option.<MathItem>none();
					 if( TypesUtil.isArrows(expr_result.type().unwrap()) && next_item.isSome() && isExprMI(next_item.unwrap()) ) {
						 // Yes. We have a function followed by an expr
						 MathItem arg = next_item.unwrap();
						 // It is a static error if either the argument is not parenthesized,
						 Option<TypeCheckerResult> is_error = expectParenedExprItem(arg);
						 if( is_error.isSome() ) {
							 return is_error.unwrap();
						 }
						 item_iter.remove(); // remove arg from item list
						 item_iter.previous();
						 item_iter.remove(); // remove fn from item list
						 // replace both with fn appication
						 _RewriteFnApp fn = new _RewriteFnApp(new Span(front.getSpan(),arg.getSpan()), front, ((ExprMI)arg).getExpr());
						 item_iter.add(new NonParenthesisDelimitedMI(fn.getSpan(),fn));
						 // static error if the argument is immediately followed by a non-expression element.
						 if( item_iter.hasNext() ) {
							 Option<TypeCheckerResult> is_expr_error = expectExprMI(item_iter.next());
							 if( is_expr_error.isSome() ) {
								 return is_expr_error.unwrap();
							 }
						 }
						 // Otherwise, make a new MathPrimary that is one element shorter, and recur
						 MathPrimary new_primary = new MathPrimary(that.getSpan(),that.isParenthesized(),that.getMultiJuxt(),that.getInfixJuxt(),that.getFront(),new_items);
						 return new_primary.accept(this);
					 } else {
						 // Continues to next expression...
						 last_expr_ = ((ExprMI)cur_item).getExpr();
						 last_expr_is_front = false;
					 }
				 }
				 else {
					 // If there is any non-expression element then replace the first such element and the
					 // element immediately preceding it (which must be an expression) with a single element that does the appropriate
					 // operator application.
					 NodeDepthFirstVisitor<Expr> op_visitor = new NodeDepthFirstVisitor<Expr>() {
						 @Override
						 public Expr forExponentiationMI(ExponentiationMI that) {
							 // how could an expression not exist in an exponentiation?
							 return ExprFactory.makeOpExpr(that.getOp(), last_expr, that.getExpr().unwrap());
						 }
						 @Override
						 public Expr forSubscriptingMI(SubscriptingMI that) {
							 Span span = new Span(last_expr.getSpan(),that.getSpan());
							 return ExprFactory.makeSubscriptExpr(span, last_expr, that.getExprs(), Option.some(that.getOp()), that.getStaticArgs());
						 }
					 };
					 Expr new_expr = cur_item.accept(op_visitor);
					 item_iter.remove(); // remove NonExprMI
					 if( last_expr_is_front ) {
						 // in our new MathPrimary, place this expression at the front
						 MathPrimary new_primary = new MathPrimary(that.getSpan(),that.isParenthesized(),that.getMultiJuxt(),that.getInfixJuxt(),new_expr,new_items);
						 return new_primary.accept(this);
					 }
					 else {
						 // in our new MathPrimary, place this expression where it is
						 item_iter.previous();
						 item_iter.remove(); // remove the previous expression in the list.
						 MathItem new_item = new ParenthesisDelimitedMI(new_expr.getSpan(),new_expr);
						 item_iter.add(new_item); // replace both item with new item
						 MathPrimary new_primary = new MathPrimary(that.getSpan(),that.isParenthesized(),that.getMultiJuxt(),that.getInfixJuxt(),that.getFront(),new_items);
						 return new_primary.accept(this);
					 }
				 }
			 }
		 }

		 // If you've made is this far, there are only expressions left in the math primary.
		 // helper method handles the last two rules
		 return juxtaposeMathPrimary(that);
	 }

     // YOU ARE HERE REWRITING AST, MOVING UP. NEB
	 
	 @Override
	 public TypeCheckerResult forMethodInvocationOnly(MethodInvocation that, Option<TypeCheckerResult> exprType_result, 
			 TypeCheckerResult obj_result, TypeCheckerResult DONTUSE,
			 List<TypeCheckerResult> staticArgs_result,
			 TypeCheckerResult arg_result) {
	     
	     // This method will probably be moved to a later pass...

		 // Did the arguments typecheck?
		 if( arg_result.type().isNone() ) {
			 return TypeCheckerResult.compose(that, subtypeChecker, obj_result, arg_result, 
			         TypeCheckerResult.compose(that, subtypeChecker, staticArgs_result));
		 }
		 // We need the type of the receiver
		 if( obj_result.type().isNone() ) {
		     return TypeCheckerResult.compose(that, subtypeChecker, obj_result, arg_result, 
                     TypeCheckerResult.compose(that, subtypeChecker, staticArgs_result));
		 }
		 // Check whether receiver can have methods
		 Type recvr_type=obj_result.type().unwrap();
		 List<TraitType> traits = traitTypesCallable(recvr_type);
		 if( traits.isEmpty() ) {
			 //error receiver not a trait
			 String trait_err = "Target of a method invocation must have trait type, while this receiver has type " + recvr_type + ".";
			 TypeCheckerResult trait_result = new TypeCheckerResult(that.getObj(), TypeError.make(trait_err, that.getObj()));
			 return TypeCheckerResult.compose(that, subtypeChecker, obj_result, arg_result, trait_result,
                     TypeCheckerResult.compose(that, subtypeChecker, staticArgs_result));
		 }
		 else {
			 Pair<List<Method>,List<TypeCheckerResult>> candidate_pair =
				 findMethodsInTraitHierarchy(that.getMethod(), traits, arg_result.type().unwrap(),that.getStaticArgs(),that);
			 
			 // Now we join together the results, or return an error if there are no candidates.
			 List<Method> candidates = candidate_pair.first();
			 if(candidates.isEmpty()) {
				 String err = "No candidate methods found for '" + that.getMethod() + "' with argument types (" + arg_result.type().unwrap() + ").";
				 TypeCheckerResult no_methods = new TypeCheckerResult(that,TypeError.make(err,that));
				 
				 return TypeCheckerResult.compose(that, subtypeChecker, obj_result, arg_result, no_methods,
				         TypeCheckerResult.compose(that, subtypeChecker, staticArgs_result));
			 }

			 List<Type> ranges = CollectUtil.makeList(IterUtil.map(candidates, new Lambda<Method,Type>(){
				 public Type value(Method arg0) {
					 return arg0.getReturnType();
				 }}));

			 Type range = subtypeChecker.join(ranges);
			 MethodInvocation new_node = new MethodInvocation(that.getSpan(),
			                                                  that.isParenthesized(),
			                                                  Option.<Type>some(range),
			                                                  (Expr)obj_result.ast(),
			                                                  that.getMethod(),
			                                                  (List<StaticArg>)TypeCheckerResult.astFromResults(staticArgs_result),
			                                                  (Expr)arg_result.ast());
			 
			 return TypeCheckerResult.compose(new_node, range, subtypeChecker, obj_result, arg_result,
			         TypeCheckerResult.compose(that, subtypeChecker, staticArgs_result),
			         TypeCheckerResult.compose(new_node, subtypeChecker, candidate_pair.second()));
		 }
	 }
	 
	 @Override
	 public TypeCheckerResult forMultiFixity(MultiFixity that) {
		 // No checks needed to be performed on a MultiFixity.
		 return new TypeCheckerResult(that);
	 }

	 @Override
	 public TypeCheckerResult forNoFixity(NoFixity that) {
		 // No checks needed to be performed on a NoFixity.
		 return new TypeCheckerResult(that);
	 }

	 @Override
	 public TypeCheckerResult forNormalParam(NormalParam that) {
		 // No checks needed to be performed on a NormalParam.
		 return new TypeCheckerResult(that);
	 }

	 @Override
	 public TypeCheckerResult forNumberConstraintOnly(NumberConstraint that,
			 TypeCheckerResult val_result) {
		 return new TypeCheckerResult(that);
	 }

	 @Override
	 public TypeCheckerResult forObjectDecl(final ObjectDecl that) {
		 List<TypeCheckerResult> modsResult = recurOnListOfModifier(that.getMods());
		 TypeCheckerResult nameResult = that.getName().accept(this);
		 List<TypeCheckerResult> extendsClauseResult = recurOnListOfTraitTypeWhere(that.getExtendsClause());
		 TypeCheckerResult whereResult = that.getWhere().accept(this);
		 Option<List<TypeCheckerResult>> paramsResult = recurOnOptionOfListOfParam(that.getParams());
		 Option<List<TypeCheckerResult>> throwsClauseResult = recurOnOptionOfListOfBaseType(that.getThrowsClause());

		 TypeChecker method_checker = this.extend(that.getStaticParams(), that.getParams(), that.getWhere());
		 TypeChecker field_checker = method_checker;
		 TypeCheckerResult contractResult = that.getContract().accept(method_checker);

		 // Verify that no extends clauses try to extend an object.
		 List<TypeCheckerResult> extends_no_obj_result =
			 CollectUtil.makeList(
							 IterUtil.map(that.getExtendsClause(), new Lambda<TraitTypeWhere,TypeCheckerResult>(){
								 public TypeCheckerResult value(TraitTypeWhere arg0) {
									 return assertTrait(arg0.getType(), that, "Objects can only extend traits.", arg0);
								 }}));

		 // Extend method checker with fields
		 for (Decl decl: that.getDecls()) {
			 if (decl instanceof VarDecl) {
				 VarDecl _decl = (VarDecl)decl;
				 method_checker = method_checker.extend(_decl.getLhs());
			 }
		 }

		 // Check method declarations.
		 Option<TypeConsIndex> ind = table.typeCons(that.getName());
		 if(ind.isNone()){
			 bug(that.getName()+"is not in table");
		 }
		 // Extend type checker with methods and functions that will now be in scope as regular functions
		 TraitIndex thatIndex = (TraitIndex)ind.unwrap();
		 Relation<IdOrOpOrAnonymousName,Method> methods = thatIndex.dottedMethods();
		 methods = new UnionRelation<IdOrOpOrAnonymousName, Method>(inheritedMethods(that.getExtendsClause()), methods);
		 method_checker = method_checker.extendWithMethods(methods);
		 method_checker = method_checker.extendWithFunctions(thatIndex.functionalMethods());

		 // Extend checker with self, (could we ever be nested more deeply than Component/Object, so that api of cu would be wrong?) 
		 APIName api = this.compilationUnit.ast().getName();
		 method_checker = TypeChecker.addSelf(api, that.getName(),method_checker,thatIndex.staticParameters());

		 // Check declarations, storing them in the same order
		 List<TypeCheckerResult> decls_result = new ArrayList<TypeCheckerResult>();
		 for (Decl decl: that.getDecls()) {
			 if (decl instanceof FnDecl) {
				 // Methods get some extra vars in their declarations
			     decls_result.add(decl.accept(method_checker));
			 }
			 else if( decl instanceof VarDecl ) {
			     // Fields get to see earlier fields
			     VarDecl _decl = (VarDecl)decl;
			     decls_result.add(decl.accept(field_checker));
			     field_checker = field_checker.extend(_decl.getLhs());
			 }
			 else {
			     decls_result.add(decl.accept(this));
			 }
		 }

		 ObjectDecl new_node = new ObjectDecl(that.getSpan(), 
				                             (List<Modifier>)TypeCheckerResult.astFromResults(modsResult),
				                             that.getName(),
				                             that.getStaticParams(),
				                             (List<TraitTypeWhere>)TypeCheckerResult.astFromResults(extendsClauseResult),
				                             (WhereClause)whereResult.ast(),
				                             (Option<List<Param>>)TypeCheckerResult.astFromResults(paramsResult),
				                             (Option<List<BaseType>>)TypeCheckerResult.astFromResults(throwsClauseResult),
				                             (Contract)contractResult.ast(),
				                             (List<Decl>)TypeCheckerResult.astFromResults(decls_result));
		 
		 return TypeCheckerResult.compose(new_node, subtypeChecker, 
		         TypeCheckerResult.compose(new_node, subtypeChecker, extends_no_obj_result),
		         TypeCheckerResult.compose(new_node, subtypeChecker, modsResult), 
		         nameResult,
		         TypeCheckerResult.compose(new_node, subtypeChecker, extendsClauseResult),
		         whereResult,
		         TypeCheckerResult.compose(new_node, subtypeChecker, paramsResult),
		         TypeCheckerResult.compose(new_node, subtypeChecker, throwsClauseResult), 
		         contractResult, 
		         TypeCheckerResult.compose(new_node, subtypeChecker, decls_result)).addNodeTypeEnvEntry(new_node, typeEnv);
	 }

	 @Override
	 public TypeCheckerResult forObjectExpr(final ObjectExpr that) {
		 List<TypeCheckerResult> extendsClause_result = recurOnListOfTraitTypeWhere(that.getExtendsClause());

		 // Verify that no extends clauses try to extend an object.
		 List<TypeCheckerResult> extends_traits_result = CollectUtil.makeList(
		         IterUtil.map(that.getExtendsClause(), new Lambda<TraitTypeWhere,TypeCheckerResult>() {
		             public TypeCheckerResult value(TraitTypeWhere arg0) {
		                 return assertTrait(arg0.getType(), that, "Objects can only extend traits.", arg0);
		             }}));
		 
		 // Extend the type checker with all of the field decls
		 TypeChecker method_checker = this;
		 TypeChecker field_checker = this;
		 for( Decl decl : that.getDecls() ) {
			 if( decl instanceof VarDecl ) {
				 VarDecl _decl = (VarDecl)decl;
				 method_checker = method_checker.extend(_decl.getLhs());
			 }
		 }

		 // Extend type checker with methods and functions that will now be in scope as regular functions
		 ObjectTraitIndex obj_index = IndexBuilder.buildObjectExprIndex(that);
		 Relation<IdOrOpOrAnonymousName,Method> methods = obj_index.dottedMethods();
		 methods = new UnionRelation<IdOrOpOrAnonymousName, Method>(inheritedMethods(that.getExtendsClause()), methods);
		 method_checker = method_checker.extendWithMethods(methods);
		 method_checker = method_checker.extendWithFunctions(obj_index.functionalMethods());

		 // Extend checker with self
		 Type obj_type = TypesUtil.getObjectExprType(that);
		 method_checker = method_checker.extend(Collections.singletonList(NodeFactory.makeLValue("self",
				 obj_type)));

		 // Typecheck each declaration
		 List<TypeCheckerResult> decls_result = new ArrayList<TypeCheckerResult>(that.getDecls().size());
		 for (Decl decl: that.getDecls()) {
			 if (decl instanceof FnDecl) {
				 // Methods get a few more things in scope than everything else
			     decls_result.add(decl.accept(method_checker));
			 }
			 else if( decl instanceof VarDecl ) {
			     // fields get to see earlier fields
			     VarDecl _decl = (VarDecl)decl;
			     decls_result.add(decl.accept(field_checker));
			     field_checker = field_checker.extend(_decl.getLhs());
			 }
			 else {
			     decls_result.add(decl.accept(this));
			 }
		 }

		 ObjectExpr new_node = new ObjectExpr(that.getSpan(),
		                                      that.isParenthesized(),
		                                      (List<TraitTypeWhere>)TypeCheckerResult.astFromResults(extendsClause_result),
		                                      (List<Decl>)TypeCheckerResult.astFromResults(decls_result)
		                                      );
		 return TypeCheckerResult.compose(new_node, obj_type, subtypeChecker, 
				                          TypeCheckerResult.compose(new_node, subtypeChecker, extendsClause_result),
				                          TypeCheckerResult.compose(new_node, subtypeChecker, decls_result),
				                          TypeCheckerResult.compose(new_node, subtypeChecker, extends_traits_result)).addNodeTypeEnvEntry(new_node, typeEnv);
	 }

	 @Override
	 public TypeCheckerResult forOp(Op that) {
	     Option<APIName> api = that.getApi();

	     Option<BindingLookup> binding;

	     if( api.isSome() ) {
	         TypeEnv type_env = returnTypeEnvForApi(api.unwrap());
	         binding = type_env.binding(that);
	     }
	     else {
	         binding = typeEnv.binding(that);
	     }

	     if (binding.isNone()) {
	         return new TypeCheckerResult(that,
	                 TypeError.make(errorMsg("Operator not found: ",
	                         OprUtil.decorateOperator(that)),
	                         that));
	     }
	     else {
	         // Happy case. No rewriting necessary, b/c nothing interesting below Op
	         return new TypeCheckerResult(that, binding.unwrap().getType());
	     }
	 }
	 
	 @Override
	 public TypeCheckerResult forOpExprOnly(OpExpr that, Option<TypeCheckerResult> exprType_result, 
			 TypeCheckerResult op_result,
			 List<TypeCheckerResult> args_result) {
	     
	     if( op_result.type().isNone() ) {
	         return TypeCheckerResult.compose(that, subtypeChecker, op_result,
	                TypeCheckerResult.compose(that, subtypeChecker, args_result));
	     }
		 
	     Type arrowType = op_result.type().unwrap();
	     
	     ArgList argTypes = new ArgList();
	     for (TypeCheckerResult r : args_result) {
	         if (r.type().isNone()) {
	             return TypeCheckerResult.compose(that, subtypeChecker, op_result,
	                     TypeCheckerResult.compose(that, subtypeChecker, args_result));
	         }
	         argTypes.add(r.type().unwrap());
	     }

	     Option<Pair<Type,ConstraintFormula>> app_result = TypesUtil.applicationType(subtypeChecker, arrowType, argTypes);
	     if (app_result.isNone()) {
	         // Guaranteed at least one operator because all the overloaded operators
	         // are created by disambiguation, not by the user.
	         OpName opName = IterUtil.first(that.getOp().getOps());
	         return TypeCheckerResult.compose(that, subtypeChecker,
	                 op_result,
	                 TypeCheckerResult.compose(that, subtypeChecker, args_result),
	                 new TypeCheckerResult(that, TypeError.make(errorMsg("Call to operator ",
	                         opName, " has invalid arguments, " + argTypes),
	                         that)));
	     }

	     Type applicationType = app_result.unwrap().first();

	     OpExpr new_node = new OpExpr(that.getSpan(),
	                                  that.isParenthesized(),
	                                  Option.<Type>some(applicationType),
	                                  (OpRef)op_result.ast(),
	                                  (List<Expr>)TypeCheckerResult.astFromResults(args_result));
	     
         // If we have a type, constraints must be propagated up.	     
	     TypeCheckerResult result = new TypeCheckerResult(new_node, app_result.unwrap().second());

	     
	     
		 return TypeCheckerResult.compose(new_node, applicationType,
				 subtypeChecker, op_result, result,
				 TypeCheckerResult.compose(new_node, subtypeChecker, args_result));
	 }
	 
	 @Override
	 public TypeCheckerResult forOpRefOnly(OpRef that, Option<TypeCheckerResult> exprType_result, 
			 TypeCheckerResult originalName_result,
			 List<TypeCheckerResult> ops_result,
			 List<TypeCheckerResult> staticArgs_result) {

		 // Get intersection of overloaded operator types.
		 List<Type> overloadedTypes = new ArrayList<Type>(ops_result.size());
		 for (TypeCheckerResult op_result : ops_result) {
			 if (op_result.type().isSome()) {
				 // Apply static arguments. Like FnRef, there is no other location in the
			     // AST where we are able to apply.
				 Option<Type> instantiated_type = TypesUtil.applyStaticArgsIfPossible(op_result.type().unwrap(), that.getStaticArgs());
				 
				 if( instantiated_type.isSome() )
				     overloadedTypes.add(instantiated_type.unwrap());
			 }
		 }
		 Option<Type> type = (overloadedTypes.isEmpty()) ?
				 Option.<Type>none() :
					 Option.<Type>some(new IntersectionType(overloadedTypes));
				 
		OpRef new_node = new OpRef(that.getSpan(),
		                           that.isParenthesized(),
		                           type,
		                           that.getLexicalDepth(),
		                           that.getOriginalName(),
		                           (List<OpName>)TypeCheckerResult.astFromResults(ops_result),
		                           (List<StaticArg>)TypeCheckerResult.astFromResults(staticArgs_result));		 
				 
		return TypeCheckerResult.compose(new_node, type, subtypeChecker,
		        TypeCheckerResult.compose(new_node, subtypeChecker, ops_result),
                TypeCheckerResult.compose(new_node, subtypeChecker, staticArgs_result));
	 }

	 @Override
	 public TypeCheckerResult forPostFixity(PostFixity that) {
		 // No checks needed to be performed on a PostFixity.
		 return new TypeCheckerResult(that);
	 }

	 @Override
	 public TypeCheckerResult forPreFixity(PreFixity that) {
		 // No checks needed to be performed on a PreFixity.
		 return new TypeCheckerResult(that);
	 }

	 @Override
	 public TypeCheckerResult forSpawn(Spawn that) {
	     // Create a new type checker that conceals any labels
	     TypeChecker newChecker = this.extendWithout(that, labelExitTypes.keySet());
	     TypeCheckerResult bodyResult = that.getBody().accept(newChecker);
	     
	     if (bodyResult.type().isNone()) 
	         return TypeCheckerResult.compose(that, subtypeChecker, bodyResult);

	     Type expr_type = Types.makeThreadType(bodyResult.type().unwrap());
	     Spawn new_node = new Spawn(that.getSpan(),
	                                that.isParenthesized(),
	                                Option.<Type>some(expr_type),
	                                (Expr)bodyResult.ast());

	     return TypeCheckerResult.compose(new_node,
	             expr_type, subtypeChecker, 
	             bodyResult).addNodeTypeEnvEntry(new_node, typeEnv);
	 }

	 @Override
	 public TypeCheckerResult forStringLiteralExpr(StringLiteralExpr that) {
	     StringLiteralExpr new_node = new StringLiteralExpr(that.getSpan(),
	                                                        that.isParenthesized(),
	                                                        Option.<Type>some(Types.STRING),
	                                                        that.getText());
		 return new TypeCheckerResult(new_node, Types.STRING);
	 }

	 @Override
	 public TypeCheckerResult forSubscriptExprOnly(SubscriptExpr that, Option<TypeCheckerResult> exprType_result, 
			 TypeCheckerResult obj_result, List<TypeCheckerResult> subs_result,
			 Option<TypeCheckerResult> op_result,
			 List<TypeCheckerResult> staticArgs_result) {

		 TypeCheckerResult all_result = TypeCheckerResult.compose(that, subtypeChecker, obj_result,
				 TypeCheckerResult.compose(that, subtypeChecker, subs_result),
				 TypeCheckerResult.compose(that, subtypeChecker, staticArgs_result));

		 // ignore the op_result. A subscript op behaves like a dotted method.
		 // make sure all sub-exprs are well-typed

		 if( obj_result.type().isNone() ) return all_result;
		 for( TypeCheckerResult r : subs_result ) {
			 if( r.type().isNone() ) return all_result;
		 }

		 Type obj_type = obj_result.type().unwrap();
		 List<Type> subs_types = CollectUtil.makeList(IterUtil.map(subs_result, new Lambda<TypeCheckerResult, Type>(){
			 public Type value(TypeCheckerResult arg0) { return arg0.type().unwrap(); }}));
		 TypeCheckerResult r = this.subscriptHelper(that, that.getOp(), obj_type, subs_types, that.getStaticArgs());
		 return TypeCheckerResult.compose(that, r.type(), subtypeChecker, all_result, r);
	 }

	 @Override
	 public TypeCheckerResult forThrowOnly(Throw that, Option<TypeCheckerResult> exprType_result, 
			 TypeCheckerResult expr_result) {

		 if( expr_result.type().isNone() ) {
			 // Failure in subexpr
			 return TypeCheckerResult.compose(that, Types.BOTTOM, subtypeChecker, expr_result);
		 }

         // A throw expression has type bottom, pretty much regardless
         // but expr must have type Exception		 
		 TypeCheckerResult expr_is_exn = this.checkSubtype(expr_result.type().unwrap(),
		         Types.EXCEPTION, that.getExpr(), "'throw' can only throw objects of Exception type. " +
		         "This expression is of type " + expr_result.type().unwrap());

		 Throw new_node = new Throw(that.getSpan(),
		                            that.isParenthesized(),
		                            Option.<Type>some(Types.BOTTOM),
		                            (Expr)expr_result.ast());

		 return TypeCheckerResult.compose(new_node, Types.BOTTOM, subtypeChecker, expr_is_exn, expr_result);

	 }

	 @Override
	 public TypeCheckerResult forTightJuxt(TightJuxt that) {
		 // Just create a MathPrimary
		 Expr front = IterUtil.first(that.getExprs());
		 Iterable<Expr> rest = IterUtil.skipFirst(that.getExprs());

		 List<MathItem> items = CollectUtil.makeList(IterUtil.map(rest, new Lambda<Expr,MathItem>(){
			 public MathItem value(Expr arg0) {
				 if( arg0.isParenthesized() || arg0 instanceof TupleExpr || arg0 instanceof VoidLiteralExpr)
					 return new ParenthesisDelimitedMI(arg0.getSpan(),arg0);
				 else
					 return new NonParenthesisDelimitedMI(arg0.getSpan(),arg0);
			 }}));
		 MathPrimary new_primary = new MathPrimary(that.getSpan(),that.isParenthesized(),that.getMultiJuxt(),that.getInfixJuxt(),front,items);
		 return new_primary.accept(this);
	 }

	 @Override
	 public TypeCheckerResult forTraitDecl(final TraitDecl that) {
		 List<TypeCheckerResult> modsResult = recurOnListOfModifier(that.getMods());
		 List<TypeCheckerResult> extendsClauseResult = recurOnListOfTraitTypeWhere(that.getExtendsClause());
		 TypeCheckerResult whereResult = that.getWhere().accept(this);
		 List<TypeCheckerResult> excludesResult = recurOnListOfBaseType(that.getExcludes());

		 // Verify that this trait only extends other traits
		 List<TypeCheckerResult> extends_trait_result = CollectUtil.makeList(
		         IterUtil.map(that.getExtendsClause(), new Lambda<TraitTypeWhere,TypeCheckerResult>(){
		             public TypeCheckerResult value(TraitTypeWhere arg0) {
		                 return assertTrait(arg0.getType(), that, "Traits can only extend traits.", arg0);
		             }}));

		 Option<List<TypeCheckerResult>> comprisesResult =  recurOnOptionOfListOfBaseType(that.getComprises());	 

		 TypeChecker method_checker = this.extend(that.getStaticParams(), that.getWhere());
		 TypeChecker field_checker = method_checker;

		 // Add field declarations (getters/setters?) to method_checker 
		 for (Decl decl: that.getDecls()) {
			 if (decl instanceof VarDecl) {
				 VarDecl _decl = (VarDecl)decl;
				 method_checker = method_checker.extend(_decl.getLhs());
			 }
		 }

		 Option<TypeConsIndex> ind = table.typeCons(that.getName());
		 if(ind.isNone()){
			 bug(that.getName()+"is not in table");
		 }

		 // Extend method checker with methods and functions that will now be in scope
		 TraitIndex thatIndex = (TraitIndex)ind.unwrap();
		 Relation<IdOrOpOrAnonymousName,Method> methods = thatIndex.dottedMethods();
		 methods = new UnionRelation<IdOrOpOrAnonymousName, Method>(inheritedMethods(that.getExtendsClause()), methods);
		 method_checker = method_checker.extendWithMethods(methods);
		 method_checker = method_checker.extendWithFunctions(thatIndex.functionalMethods());

		 // Extend method checker with self, (could we ever be nested more deeply than Component/Object, so that api of cu would be wrong?) 
		 APIName api = this.compilationUnit.ast().getName();
		 method_checker = TypeChecker.addSelf(api, that.getName(),method_checker,thatIndex.staticParameters());

		 // Check declarations
		 List<TypeCheckerResult> decls_result = new ArrayList<TypeCheckerResult>(that.getDecls().size());
		 for (Decl decl: that.getDecls()) {
			 if (decl instanceof FnDecl) {
				 // methods see extra variables in scope
				 decls_result.add(decl.accept(method_checker));
			 }
			 else if( decl instanceof VarDecl ) {
			     // fields see other fields
			     VarDecl _decl = (VarDecl)decl;
			     decls_result.add(decl.accept(field_checker));
			     field_checker = field_checker.extend(_decl.getLhs());
			 }
			 else {
			     decls_result.add(decl.accept(this));
			 }
		 }
		 
		 TraitDecl new_node = 
		     new TraitDecl(that.getSpan(),
		                   (List<Modifier>)TypeCheckerResult.astFromResults(modsResult),
		                   that.getName(),
		                   that.getStaticParams(),
		                   (List<TraitTypeWhere>)TypeCheckerResult.astFromResults(extendsClauseResult),
		                   (WhereClause)whereResult.ast(),
		                   (List<BaseType>)TypeCheckerResult.astFromResults(excludesResult),
		                   (Option<List<BaseType>>)TypeCheckerResult.astFromResults(comprisesResult),
		                   (List<Decl>)TypeCheckerResult.astFromResults(decls_result));
		 
		 return TypeCheckerResult.compose(new_node, subtypeChecker, 
		         TypeCheckerResult.compose(new_node, subtypeChecker, modsResult),
		         TypeCheckerResult.compose(new_node, subtypeChecker, extendsClauseResult), 
				 whereResult, 
				 TypeCheckerResult.compose(new_node, subtypeChecker, extends_trait_result), 
				 TypeCheckerResult.compose(new_node, subtypeChecker, excludesResult),
				 TypeCheckerResult.compose(new_node, subtypeChecker, comprisesResult), 
				 TypeCheckerResult.compose(new_node, subtypeChecker, decls_result)
		         ).addNodeTypeEnvEntry(new_node, typeEnv);
	 }



	 @Override
	 public TypeCheckerResult forTraitType(TraitType that) {
		 return new TypeCheckerResult(that);
	 }

	 public TypeCheckerResult forTraitTypeWhereOnly(TraitTypeWhere that,
			 TypeCheckerResult type_result,
			 TypeCheckerResult where_result) {
		 return TypeCheckerResult.compose(that, subtypeChecker, type_result, where_result);
	 }

	 @Override
	 public TypeCheckerResult forTryAtomicExpr(TryAtomicExpr that) {
		 return forAtomic(
				 that.getExpr(),
				 errorMsg("A 'spawn' expression must not occur inside a 'try atomic' expression."));
	 }

	 @Override
	 public TypeCheckerResult forTryOnly(Try that, Option<TypeCheckerResult> exprType_result, 
			 TypeCheckerResult body_result, Option<TypeCheckerResult> catch_result,
			 List<TypeCheckerResult> forbid_result, Option<TypeCheckerResult> finally_result) {
		 // gather all sub-results
	     TypeCheckerResult catch_result_ = catch_result.isSome() ? catch_result.unwrap() : new TypeCheckerResult(that);
	     TypeCheckerResult finally_result_ = finally_result.isSome() ? finally_result.unwrap() : new TypeCheckerResult(that);

		 // Check that all forbids are subtypes of exception
		 List<TypeCheckerResult> forbids_exn = new ArrayList<TypeCheckerResult>(that.getForbid().size());
	     for( Type t : that.getForbid() ) {
			 TypeCheckerResult r =
				 this.checkSubtype(t, Types.EXCEPTION, t,"All types in 'forbids' clause must be subtypes of " +
						 "Exception, but "+ t + " is not.");
			 forbids_exn.add(r);
		 }
		 // the resulting type is the join of try, catches, and finally
		 List<Type> all_types = new LinkedList<Type>();
		 if( body_result.type().isSome() )
			 all_types.add(body_result.type().unwrap());
		 if( catch_result.isSome() && catch_result.unwrap().type().isSome() )
			 all_types.add(catch_result.unwrap().type().unwrap());
		 if( finally_result.isSome() && finally_result.unwrap().type().isSome() )
			 all_types.add(finally_result.unwrap().type().unwrap());

		 Type expr_type = this.subtypeChecker.join(all_types);
		 Try new_node = new Try(that.getSpan(),
		                        that.isParenthesized(),
		                        Option.<Type>some(expr_type),
		                        (Block)body_result.ast(),
		                        (Option<Catch>)TypeCheckerResult.astFromResult(catch_result),
		                        (List<BaseType>)TypeCheckerResult.astFromResults(forbid_result),
		                        (Option<Block>)TypeCheckerResult.astFromResult(finally_result));
		 
		 return TypeCheckerResult.compose(new_node, expr_type, subtypeChecker, 
		                                  catch_result_, finally_result_,
		                                  TypeCheckerResult.compose(new_node, subtypeChecker, forbid_result),
		                                  TypeCheckerResult.compose(new_node, subtypeChecker, forbids_exn),
		                                  body_result);
	 }

	 @Override
	 public TypeCheckerResult forTupleExprOnly(TupleExpr that, Option<TypeCheckerResult> exprType_result,
			 List<TypeCheckerResult> exprs_result) {
		 List<Type> types = new ArrayList<Type>(exprs_result.size());
		 for (TypeCheckerResult r : exprs_result) {
			 if (r.type().isNone()) {
				 return TypeCheckerResult.compose(that, subtypeChecker, exprs_result);
			 }
			 types.add(r.type().unwrap());
		 }
		 
		 TupleType tuple_type = NodeFactory.makeTupleType(types);
		 
		 TupleExpr new_node = new TupleExpr(that.getSpan(),
		                                    that.isParenthesized(),
		                                    Option.<Type>some(tuple_type),
		                                    (List<Expr>)TypeCheckerResult.astFromResults(exprs_result));
		 
		 
        return TypeCheckerResult.compose(new_node, tuple_type, 
                                         subtypeChecker, exprs_result);
	 }

	 private TypeCheckerResult forTypeAnnotatedExprOnly(TypeAnnotatedExpr that,
			 final TypeCheckerResult expr_result,
			 String errorMsg) {
		 
	     if( expr_result.type().isNone() ) {
	         return new TypeCheckerResult(that);
	     }
	     
	     final Type annotatedType = that.getType();
	     Type exprType = expr_result.type().unwrap();
	     
	     // Check that expression type <: annotated type.
	     TypeCheckerResult subtype_check = checkSubtype(exprType,
                 annotatedType,
                 expr_result.ast(),
                 errorMsg); 
	     
	     Node new_node =
	         that.accept(new NodeAbstractVisitor<Node>(){

	             @Override
	             public Node forAsExpr(AsExpr that) {
	                 return new AsExpr(that.getSpan(), that.isParenthesized(), some(annotatedType), 
	                         (Expr)expr_result.ast(), annotatedType);
	             }

	             @Override
	             public Node forAsIfExpr(AsIfExpr that) {
	                 return new AsIfExpr(that.getSpan(), that.isParenthesized(), some(annotatedType),
	                         (Expr)expr_result.ast(), annotatedType);
	             }
	         });
	     
		 return TypeCheckerResult.compose(new_node, annotatedType, subtypeChecker, expr_result, subtype_check);
	 }

	 @Override
	 public TypeCheckerResult forTypeArg(TypeArg that) {
		 return new TypeCheckerResult(that);
	 }

	 @Override
	 public TypeCheckerResult forTypecase(Typecase that) {
		 // typecase is a somewhat interesting case
		 List<TypeCheckerResult> clauses_result;
		 List<TypeCheckerResult> bind_results = new LinkedList<TypeCheckerResult>();
		 List<Id> bindings = that.getBindIds();

		 Option<TypeCheckerResult> bind_expr_result_ = this.recurOnOptionOfExpr(that.getBindExpr());
		 if( bind_expr_result_.isSome()) {
			 if(bind_expr_result_.unwrap().type().isNone()){
				 return TypeCheckerResult.compose(that, subtypeChecker, bind_expr_result_.unwrap());
			 }
			 Type original_type = bind_expr_result_.unwrap().type().unwrap();

			 if( bindings.size() > 1 ) {
				 // typecase (a,b,c) = someX of ...
				 if(!(original_type instanceof TupleType)){
					 String err="Right hand side of binding not a tuple";
					 return TypeCheckerResult.compose(that,
							 subtypeChecker,
							 new TypeCheckerResult(that,TypeError.make(err, that.getBindExpr().unwrap())), bind_expr_result_.unwrap());
				 }
				 TupleType tuple=(TupleType)original_type;
				 if(tuple.getElements().size()!=bindings.size()){
					 String err="Number of bindings does not match type of expression";
					 return TypeCheckerResult.compose(that,
							 subtypeChecker,
							 new TypeCheckerResult(that,TypeError.make(err, that.getBindExpr().unwrap())), bind_expr_result_.unwrap());
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
				 Option<BindingLookup> lookup = typeEnv.binding(id);
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

		 Option<TypeCheckerResult> elseClause_result_ = recurOnOptionOfBlock(that.getElseClause());
		 TypeCheckerResult elseClause_result = elseClause_result_.isSome() ?
		         elseClause_result_.unwrap() :
		         new TypeCheckerResult(that);
		 
		 TypeCheckerResult bind_expr_result = bind_expr_result_.isSome() ?
		         bind_expr_result_.unwrap() :
		         new TypeCheckerResult(that);
		         
		 List<Type> all_types = new ArrayList<Type>(clauses_result.size()+1);
		 for( TypeCheckerResult result : clauses_result ) {
			 if( result.type().isSome() ) {
				 all_types.add(result.type().unwrap());
			 }
		 }
		 
		 Type result_type = subtypeChecker.join(all_types);
		 Typecase new_node = new Typecase(that.getSpan(),
		                                  that.isParenthesized(),
		                                  Option.some(result_type),
		                                  that.getBindIds(),
		                                  (Option<Expr>)TypeCheckerResult.astFromResult(bind_expr_result_),
		                                  (List<TypecaseClause>)TypeCheckerResult.astFromResults(clauses_result),
		                                  (Option<Block>)TypeCheckerResult.astFromResult(elseClause_result_));
		 
        return TypeCheckerResult.compose(new_node,
                                         result_type, 
				                         subtypeChecker,
				                         bind_expr_result,
				                         elseClause_result,
				                         TypeCheckerResult.compose(new_node, subtypeChecker, clauses_result)
				                         ).addNodeTypeEnvEntry(new_node, typeEnv);
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

	 @Override
	 public TypeCheckerResult forTypeParam(TypeParam that) {
		 return new TypeCheckerResult(that);
	 }

	 @Override
	 public TypeCheckerResult forVarDecl(VarDecl that) {
		 List<LValueBind> lhs = that.getLhs();
		 Expr init = that.getInit();

		 TypeCheckerResult initResult = init.accept(this);
		 
		 TypeCheckerResult subtype_result;
		 if (lhs.size() == 1) { // We have a single variable binding, not a tuple binding
			 LValueBind var = lhs.get(0);
			 Option<Type> varType = var.getType();
			 if (varType.isSome()) {
				 if (initResult.type().isNone()) {
					 // The right hand side could not be typed, which must have resulted in a
					 // signaled error. No need to signal another error.
					 return TypeCheckerResult.compose(that, subtypeChecker, initResult);
				 }
				 else {
				     subtype_result =
				         checkSubtype(initResult.type().unwrap(),
				                 varType.unwrap(),
				                 that,
				                 errorMsg("Attempt to define variable ", var, " ",
				                         "with an expression of type ", initResult.type().unwrap()));

				 }
			 } else { // Eventually, this case will involve type inference
				 return bug("All inferrred types should at least be inference variables by typechecking: " + that);
			 }
		 } else { // lhs.size() >= 2
			 Type varType = typeFromLValueBinds(lhs);
			 if (initResult.type().isNone()) {
				 // The right hand side could not be typed, which must have resulted in a
				 // signaled error. No need to signal another error.
				 return TypeCheckerResult.compose(that, subtypeChecker, initResult);
			 }
			 else {
			     subtype_result=
			         checkSubtype(initResult.type().unwrap(),
			                 varType,
			                 that,
			                 errorMsg("Attempt to define variables ", lhs, " ",
			                         "with an expression of type ", initResult.type().unwrap()));
			 }
		 }
		 
		 VarDecl new_node = new VarDecl(that.getSpan(), that.getLhs(), (Expr)initResult.ast());
		 return TypeCheckerResult.compose(new_node, subtypeChecker, subtype_result, initResult);
	 }

	 @Override
	 public TypeCheckerResult forVarRefOnly(VarRef that, Option<TypeCheckerResult> exprType_result, TypeCheckerResult var_result) {
		 Option<Type> varType = var_result.type();
		 
		 VarRef new_node = new VarRef(that.getSpan(), 
		                              varType,
		                              (Id)var_result.ast());
		 
		 return TypeCheckerResult.compose(new_node, varType, subtypeChecker, var_result);
	 }

	 @Override
	 public TypeCheckerResult forVoidLiteralExpr(VoidLiteralExpr that) {
	     VoidLiteralExpr new_node = new VoidLiteralExpr(that.getSpan(),
	                                                    that.isParenthesized(),
	                                                    Option.<Type>some(Types.VOID),
	                                                    that.getText());
	     return new TypeCheckerResult(new_node, Types.VOID);
	 }


	 @Override
	 public TypeCheckerResult forWhereClause(WhereClause that) {
		 if (that.getBindings().isEmpty() && that.getConstraints().isEmpty()) {
			 return new TypeCheckerResult(that);
		 } else {
			 return defaultCase(that);
		 }
	 }

	 @Override
	 public TypeCheckerResult forWhile(While that) {
		 Pair<TypeCheckerResult,List<LValueBind>> res = this.forGeneratorClauseGetBindings(that.getTest(), true);
		 TypeChecker extended = this.extend(res.second());
		 TypeCheckerResult body_result = that.getBody().accept(extended);
		 
		// did sub expressions typecheck?
         if(body_result.type().isNone()) {
             return TypeCheckerResult.compose(that,Types.VOID, subtypeChecker, res.first(), body_result);
         }

         String void_err = "Body of while loop must have type (), but had type " +
            body_result.type().unwrap();
         TypeCheckerResult void_result = this.checkSubtype(body_result.type().unwrap(), Types.VOID, that.getBody(), void_err);
         
         While new_node = new While(that.getSpan(),
                                    that.isParenthesized(),
                                    Option.<Type>some(Types.VOID),
                                    (GeneratorClause)res.first().ast(),
                                    (Do)body_result.ast());
         
         return TypeCheckerResult.compose(new_node,Types.VOID, subtypeChecker,
                 res.first(), body_result, void_result).addNodeTypeEnvEntry(new_node, typeEnv);		 
	 }

	 /**
	  * Returns a type checker result and the a Type that is the type of the
	  * condition. The given type is checked to be a sub-type of
	  * Condition[\T\] where T is an inference variable, and the inferred type
	  * T is returned.
	  */
	 private Pair<TypeCheckerResult, Type> getConditionType(Type sub, Node ast, String error) {
		 Type infer_type = NodeFactory.make_InferenceVarType(ast.getSpan());
		 Type generator_type = Types.makeConditionType(infer_type);
		 TypeCheckerResult result = this.checkSubtype(sub, generator_type, ast, error);

		 return Pair.make( result, infer_type );
	 }

	 /**
	  * Returns a type checker result and the a Type that is the type of the
	  * generator. The given type is checked to be a sub-type of
	  * Generator[\T\] where T is an inference variable, and the inferred type
	  * T is returned.
	  */
	 private Pair<TypeCheckerResult, Type> getGeneratorType(Type sub, Node ast, String error) {
		 Type infer_type = NodeFactory.make_InferenceVarType(ast.getSpan());
		 Type generator_type = Types.makeGeneratorType(infer_type);
		 TypeCheckerResult result = this.checkSubtype(sub, generator_type, ast, error);

		 return Pair.make( result, infer_type );
	 }

	 private Relation<IdOrOpOrAnonymousName, Method> inheritedMethods(List<TraitTypeWhere> extended_traits) {
		 return inheritedMethodsHelper(new HierarchyHistory(), extended_traits);
	 }

	 // Return all of the methods from super-traits
	 private Relation<IdOrOpOrAnonymousName, Method> inheritedMethodsHelper(HierarchyHistory h,
			 List<TraitTypeWhere> extended_traits) {
		 Relation<IdOrOpOrAnonymousName, Method> methods = new IndexedRelation<IdOrOpOrAnonymousName, Method>(false);
		 for( TraitTypeWhere trait_ : extended_traits ) {

			 BaseType type_ = trait_.getType();

			 if( h.hasExplored(type_) )
				 continue;
			 h = h.explore(type_);

			 // Trait types or VarTypes can represent traits at this phase of compilation.
			 Id trait_name;
			 if( type_ instanceof TraitType ) {
				 trait_name = ((TraitType)type_).getName();
			 }
			 else if( type_ instanceof VarType ) {
				 trait_name = ((VarType)type_).getName();
			 }
			 else {
				 // Probably ANY
				 return EmptyRelation.make();
			 }

			 Option<TypeConsIndex> tci = this.table.typeCons(trait_name);
			 if( tci.isSome() && tci.unwrap() instanceof TraitIndex ) {
				 TraitIndex ti = (TraitIndex)tci.unwrap();
				 // Add dotted methods
				 methods.addAll(ti.dottedMethods());
				 // Add getters
				 methods.addAll(Useful.<IdOrOpOrAnonymousName,Method>relation(ti.getters()));
				 // Add setters
				 methods.addAll(Useful.<IdOrOpOrAnonymousName,Method>relation(ti.setters()));
				 // For now we won't add functional methods. They are not received through inheritance.

				 // Now recursively add methods from trait's extends clause
				 methods.addAll(inheritedMethodsHelper(h, ti.extendsTypes()));
			 }
			 else {
				 // Probably ANY
				 return EmptyRelation.make();
			 }


		 }
		 return methods;
	 }

	 private TypeCheckerResult juxtaposeMathPrimary(MathPrimary that) {
		 // 2.) If you have reached this point, The overall juxtaposition entirely of expressions
		 ConsList<Expr> exprs_ = CollectUtil.makeConsList(IterUtil.map(that.getRest(), new Lambda<MathItem,Expr>(){
			 public Expr value(MathItem arg0) {
				 if( !isExprMI(arg0) )
					 return bug("Must be an expr.");
				 else
					 return ((ExprMI)arg0).getExpr();
			 }
		 }));
		 List<Expr> exprs = CollectUtil.makeList(ConsList.cons(that.getFront(), exprs_));


		 // (1) If any element that remains has type String, then it is a static error if any two adjacent elements are not of type String.
		 // Moved this to seperate pass
		 // (2) Treat the sequence that remains as a multifix application of the juxtaposition operator. The rules for multifix operators then apply:
		 OpExpr multi_op_expr = new OpExpr(that.getSpan(),that.getMultiJuxt(),exprs);
		 TypeCheckerResult multi_op_result = multi_op_expr.accept(this);
		 if( multi_op_result.type().isSome() ) {
			 return TypeCheckerResult.compose(multi_op_expr, multi_op_result.type(), subtypeChecker, multi_op_result);
		 }

		 // if an applicable method cannot be found for the entire expression, then it is left-associated.
		 Iterator<Expr> expr_iter = exprs.iterator();
		 Expr expr_1 = expr_iter.next(); // the fact that >= two items are here is guaranteed from above.
		 Expr expr_2 = expr_iter.next();
		 OpExpr cur_op_expr = new OpExpr(new Span(expr_1.getSpan(),expr_2.getSpan()), that.getInfixJuxt(), Useful.list(expr_1,expr_2));
		 while( expr_iter.hasNext() ) {
			 Expr next_expr = expr_iter.next();
			 cur_op_expr = new OpExpr(new Span(cur_op_expr.getSpan(),next_expr.getSpan()), that.getInfixJuxt(), Useful.list(cur_op_expr, next_expr));
		 }
		 // typecheck this result instead
		 TypeCheckerResult op_expr_result = cur_op_expr.accept(this); // Is it bad to re-typecheck all args?
		 return op_expr_result;
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

	 // For each generator clause, check its body, then put its variables in scope for the next generator clause.
	 // Finally, return all of the bindings so that they can be put in scope in some larger expression, like the
	 // body of a for loop, for example.
	 private Pair<List<TypeCheckerResult>, List<LValueBind>> recurOnListsOfGeneratorClauseBindings(List<GeneratorClause> gens) {
		 if( gens.isEmpty() )
			 return Pair.make(Collections.<TypeCheckerResult>emptyList(), Collections.<LValueBind>emptyList());
		 else if( gens.size() == 1 ) {
			 Pair<TypeCheckerResult,List<LValueBind>> pair = forGeneratorClauseGetBindings(IterUtil.first(gens), false);
			 return Pair.make(Collections.singletonList(pair.first()), pair.second());
		 }
		 else {
			 Pair<TypeCheckerResult,List<LValueBind>> pair = forGeneratorClauseGetBindings(IterUtil.first(gens), false);
			 TypeChecker new_checker = this.extend(pair.second());
			 // recur
			 Pair<List<TypeCheckerResult>, List<LValueBind>> recur_result = 
				 new_checker.recurOnListsOfGeneratorClauseBindings(CollectUtil.makeList(IterUtil.skipFirst(gens)));
			 return Pair.make( Useful.cons(pair.first(), recur_result.first()), 
					           Useful.concat(pair.second(), recur_result.second()));
		 }
	 }

	 private TypeEnv returnTypeEnvForApi(APIName api) {
		 if( compilationUnit.ast().getName().equals(api) )
			 return typeEnv;
		 else
			 return TypeEnv.make(table.compilationUnit(api));
	 }

	 private TypeCheckerResult subscriptHelper(Node that, Option<Enclosing> op,
			 Type obj_type, List<Type> subs_types, List<StaticArg> static_args) {
		 List<TraitType> traits = traitTypesCallable(obj_type);
		 // we need to have a trait otherwise we can't see its methods.
		 if( traits.isEmpty() ) {
			 String err = "Only traits can have subscripting methods and " + obj_type + " is not one.";
			 TypeCheckerResult err_result = new TypeCheckerResult(that, TypeError.make(err, that));
			 return TypeCheckerResult.compose(that, subtypeChecker, err_result);
		 }

		 // Make a tuple type out of given arguments
		 Type arg_type = Types.MAKE_TUPLE.value(subs_types);

		 Pair<List<Method>,List<TypeCheckerResult>> candidate_pair =
			 findMethodsInTraitHierarchy(op.unwrap(), traits, arg_type, static_args,that);
		 TypeCheckerResult result = TypeCheckerResult.compose(that, subtypeChecker, candidate_pair.second());
		 List<Method> candidates = candidate_pair.first();

		 // Now we join together the results, or return an error if there are no candidates.
		 if(candidates.isEmpty()){
			 String err = "No candidate methods found for '" + op + "'  on type " + obj_type + " with argument types (" + arg_type + ").";
			 TypeCheckerResult err_result = new TypeCheckerResult(that,TypeError.make(err,that));
			 return TypeCheckerResult.compose(that, subtypeChecker, result, err_result);
		 }

		 List<Type> ranges = CollectUtil.makeList(IterUtil.map(candidates, new Lambda<Method,Type>(){
			 public Type value(Method arg0) { return arg0.getReturnType(); }}));

		 Type range = this.subtypeChecker.join(ranges);
		 return TypeCheckerResult.compose(that, range, subtypeChecker, result);
	 }

	 /**
	  * Given a type, which could be a VarType, Intersection or Union, return the TraitTypes
	  * that this type could be used as for the purposes of calling methods and fields.
	  */
	 private List<TraitType> traitTypesCallable(Type type) {
		 return type.accept(new NodeAbstractVisitor<List<TraitType>>(){

			@Override
			public List<TraitType> forIntersectionType(IntersectionType that) {
				return NI.nyi("You should be able to call methods on this type, but this is nyi." + that);
			}

			@Override
			public List<TraitType> forTraitType(TraitType that) {
				return Collections.singletonList(that);
			}

			@Override
			public List<TraitType> forType(Type that) {
				return Collections.emptyList();
			}

			@Override
			public List<TraitType> forUnionType(UnionType that) {
				return NI.nyi("You should be able to call methods on this type, but this is nyi." + that);
			}

			@Override
			public List<TraitType> forVarType(VarType that) {
				 Option<StaticParam> param_ = typeEnv.staticParam(that.getName());
				 
				 if( param_.isNone() ) return Collections.emptyList();
				 
				 StaticParam param = param_.unwrap();
				 return param.accept(new NodeAbstractVisitor<List<TraitType>>(){

					@Override
					public List<TraitType> forTypeParam(TypeParam that) {
						List<TraitType> result = new ArrayList<TraitType>();
						for( BaseType type : that.getExtendsClause() ) {
							result.addAll(TypeChecker.this.traitTypesCallable(type));
						}
						return result;
					}
				 });
			}
		 });
	 }

}
