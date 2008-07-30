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

import static edu.rice.cs.plt.tuple.Option.wrap;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.sun.fortress.compiler.index.TraitIndex;
import com.sun.fortress.compiler.index.TypeConsIndex;
import com.sun.fortress.exceptions.StaticError;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.AbsDecl;
import com.sun.fortress.nodes.AbsFnDecl;
import com.sun.fortress.nodes.AbsObjectDecl;
import com.sun.fortress.nodes.AbsTraitDecl;
import com.sun.fortress.nodes.AbsVarDecl;
import com.sun.fortress.nodes.Accumulator;
import com.sun.fortress.nodes.BaseType;
import com.sun.fortress.nodes.Block;
import com.sun.fortress.nodes.BoolParam;
import com.sun.fortress.nodes.Catch;
import com.sun.fortress.nodes.Contract;
import com.sun.fortress.nodes.Decl;
import com.sun.fortress.nodes.DimDecl;
import com.sun.fortress.nodes.Do;
import com.sun.fortress.nodes.DoFront;
import com.sun.fortress.nodes.Exit;
import com.sun.fortress.nodes.Expr;
import com.sun.fortress.nodes.FnDef;
import com.sun.fortress.nodes.FnExpr;
import com.sun.fortress.nodes.FnRef;
import com.sun.fortress.nodes.For;
import com.sun.fortress.nodes.GeneratedExpr;
import com.sun.fortress.nodes.GeneratorClause;
import com.sun.fortress.nodes.GrammarDef;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.IdOrOpOrAnonymousName;
import com.sun.fortress.nodes.IdStaticParam;
import com.sun.fortress.nodes.IfClause;
import com.sun.fortress.nodes.IntParam;
import com.sun.fortress.nodes.LValue;
import com.sun.fortress.nodes.LValueBind;
import com.sun.fortress.nodes.Label;
import com.sun.fortress.nodes.LetFn;
import com.sun.fortress.nodes.LocalVarDecl;
import com.sun.fortress.nodes.Modifier;
import com.sun.fortress.nodes.ModifierGetter;
import com.sun.fortress.nodes.ModifierSetter;
import com.sun.fortress.nodes.NatParam;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.NodeDepthFirstVisitor;
import com.sun.fortress.nodes.NodeUpdateVisitor;
import com.sun.fortress.nodes.ObjectDecl;
import com.sun.fortress.nodes.ObjectExpr;
import com.sun.fortress.nodes.OpExpr;
import com.sun.fortress.nodes.OpName;
import com.sun.fortress.nodes.OpRef;
import com.sun.fortress.nodes.Param;
import com.sun.fortress.nodes.StaticParam;
import com.sun.fortress.nodes.TaggedDimType;
import com.sun.fortress.nodes.TaggedUnitType;
import com.sun.fortress.nodes.TraitDecl;
import com.sun.fortress.nodes.TraitType;
import com.sun.fortress.nodes.TraitTypeWhere;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes.Typecase;
import com.sun.fortress.nodes.UnitDecl;
import com.sun.fortress.nodes.UnitParam;
import com.sun.fortress.nodes.UnpastingBind;
import com.sun.fortress.nodes.UnpastingSplit;
import com.sun.fortress.nodes.VarDecl;
import com.sun.fortress.nodes.VarRef;
import com.sun.fortress.nodes.VarType;
import com.sun.fortress.nodes.VoidLiteralExpr;
import com.sun.fortress.nodes.WhereClause;
import com.sun.fortress.nodes.While;
import com.sun.fortress.nodes._RewriteObjectRef;
import com.sun.fortress.nodes_util.ExprFactory;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.nodes_util.Span;
import com.sun.fortress.parser_util.FortressUtil;
import com.sun.fortress.useful.HasAt;
import com.sun.fortress.useful.Useful;

import edu.rice.cs.plt.collect.CollectUtil;
import edu.rice.cs.plt.collect.ConsList;
import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.lambda.Lambda2;
import edu.rice.cs.plt.tuple.Option;
import edu.rice.cs.plt.tuple.Pair;

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
	private List<StaticError> _errors;
	private Option<Id> _innerMostLabel;

	public ExprDisambiguator(NameEnv env, List<StaticError> errors) {
		_env = env;
		_errors = errors;
		_innerMostLabel = Option.<Id>none();
	}

	private ExprDisambiguator(NameEnv env, List<StaticError> errors, Option<Id> innerMostLabel) {
		this(env, errors);
		_innerMostLabel = innerMostLabel;
	}

	private ExprDisambiguator extendWithVars(Set<Id> vars) {
		NameEnv newEnv = new LocalVarEnv(_env, vars);
		return new ExprDisambiguator(newEnv, _errors, this._innerMostLabel);
	}
	
	private ExprDisambiguator extendWithFns(Set<? extends IdOrOpOrAnonymousName> definedNames){
		NameEnv newEnv = new LocalFnEnv(_env, CollectUtil.makeSet(IterUtil.relax(definedNames)));
		return new ExprDisambiguator(newEnv, _errors, this._innerMostLabel);
	}

	private ExprDisambiguator extendWithSelf(Span span) {
		Set<Id> selfSet = Collections.singleton(new Id(span, "self"));
		return extendWithVars(selfSet);
	}

	private ExprDisambiguator extendsWithResult(Span span) {
		Set<Id> resultSet = Collections.singleton(new Id(span, "result"));
		return extendWithVars(resultSet);
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
		ExprDisambiguator v = new ExprDisambiguator(newEnv, _errors);
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

	private static boolean isSetterOrGetter(List<Modifier> mods) {
		NodeDepthFirstVisitor<Boolean> mod_visitor =
		new NodeDepthFirstVisitor<Boolean>() {
			@Override public Boolean defaultCase(Node n) { return false; }
			@Override public Boolean forModifierGetter(ModifierGetter that) { return true; }
			@Override public Boolean forModifierSetter(ModifierSetter that) { return true; }
		};
		
		return
		IterUtil.fold(mod_visitor.recurOnListOfModifier(mods), false, new Lambda2<Boolean,Boolean,Boolean>(){
			public Boolean value(Boolean arg0, Boolean arg1) { return arg0 | arg1; }});
	}
	
	private Pair<Set<Id>,Set<IdOrOpOrAnonymousName>> extractDeclNames(List<Decl> decls) {
		final Set<IdOrOpOrAnonymousName> accessors = new HashSet<IdOrOpOrAnonymousName>();
		
		NodeDepthFirstVisitor<Set<Id>> var_finder = new NodeDepthFirstVisitor<Set<Id>>(){
			@Override
			public Set<Id> forAbsVarDecl(AbsVarDecl that) {
				return extractDefinedVarNames(that.getLhs());
			}
			
			@Override
			public Set<Id> forVarDecl(VarDecl that) {
				return extractDefinedVarNames(that.getLhs());
			}

			@Override
			public Set<Id> forAbsFnDecl(AbsFnDecl that) {
				return Collections.emptySet();
			}

			@Override
			public Set<Id> forFnDef(FnDef that) {
				return Collections.emptySet();
			}
		
		};
		NodeDepthFirstVisitor<Set<IdOrOpOrAnonymousName>> fn_finder = new NodeDepthFirstVisitor<Set<IdOrOpOrAnonymousName>>(){
			@Override
			public Set<IdOrOpOrAnonymousName> forAbsVarDecl(AbsVarDecl that) {
				return Collections.emptySet();
			}
			
			@Override
			public Set<IdOrOpOrAnonymousName> forVarDecl(VarDecl that) {
				return Collections.emptySet();
			}

			@Override
			public Set<IdOrOpOrAnonymousName> forAbsFnDecl(AbsFnDecl that) {
				if( isSetterOrGetter(that.getMods()) )
					accessors.add(that.getName());
				
				if( FortressUtil.isFunctionalMethod(that.getParams()) ) {
					// don't add functional methods! they go at the top level...
					return Collections.emptySet();
				}
				else {
					return Collections.singleton(that.getName());
				}
			}

			@Override
			public Set<IdOrOpOrAnonymousName> forFnDef(FnDef that) {
				if( isSetterOrGetter(that.getMods()) )
					accessors.add(that.getName());
				if( FortressUtil.isFunctionalMethod(that.getParams()) ) {
					// don't add functional methods! they go at the top level...
					return Collections.emptySet();
				}
				else {
					return Collections.singleton(that.getName());
				}
			}
		};
		List<Set<Id>> vars_ = var_finder.recurOnListOfDecl(decls);
		List<Set<IdOrOpOrAnonymousName>> fns_ = fn_finder.recurOnListOfDecl(decls);
		
		Set<Id> vars = Useful.union(vars_);
		Set<IdOrOpOrAnonymousName> fns = new HashSet<IdOrOpOrAnonymousName>(Useful.union(fns_));
		
		// For every accessor, remove that fn from fns if there is also a variable.
		// See shadowing rules in section 7.3
		for( IdOrOpOrAnonymousName ioooan : accessors ) {
			if( vars.contains(ioooan) ) {
				fns.remove(ioooan);
			}
		}
		
		return Pair.make(vars, fns);
	}
	
	/**
	 * When recurring on an AbsTraitDecl, we first need to extend the
	 * environment with all the newly bound static parameters that can
	 * be used in an expression context.
	 * TODO: Handle variables bound in where clauses.
	 * TODO: Insert inherited method names into the environment.
	 */
	@Override public Node forAbsTraitDecl(final AbsTraitDecl that) {
		ExprDisambiguator v = this.extendWithVars(extractStaticExprVars(that.getStaticParams()));
		List<TraitTypeWhere> extends_clause = v.recurOnListOfTraitTypeWhere(that.getExtendsClause());
		
		// Include trait declarations and inherited methods
		Pair<Set<Id>,Set<IdOrOpOrAnonymousName>> decl_names = extractAbsDeclNames(that.getDecls());
		Set<Id> vars = decl_names.first();
		Set<IdOrOpOrAnonymousName> fns = decl_names.second();
		v = this.extendWithVars(extractStaticExprVars
				(that.getStaticParams())).
				extendWithFns(inheritedMethods(extends_clause)).
				extendWithSelf(that.getSpan()).extendWithVars(vars).extendWithFns(fns);
		
		return forAbsTraitDeclOnly(that,
				v.recurOnListOfModifier(that.getMods()),
				(Id) that.getName().accept(v),
				v.recurOnListOfStaticParam(that.getStaticParams()),
				extends_clause,
				(WhereClause) that.getWhere().accept(v),
				v.recurOnListOfBaseType(that.getExcludes()),
				v.recurOnOptionOfListOfBaseType(that.getComprises()),
				v.recurOnListOfAbsDecl(that.getDecls()));
	}
	
	
	/**
	 * When recurring on an ObjExpr, we first need to extend the
	 * environment with all the newly bound variables and methods
	 * TODO: Insert inherited method names into the environment.
	 */
	@Override
	public Node forObjectExpr(ObjectExpr that) {
		List<TraitTypeWhere> extends_clause = recurOnListOfTraitTypeWhere(that.getExtendsClause());
		
		// Include trait declarations and inherited methods
		Pair<Set<Id>,Set<IdOrOpOrAnonymousName>> decl_names = extractDeclNames(that.getDecls());
		Set<Id> vars = decl_names.first();
		Set<IdOrOpOrAnonymousName> fns = decl_names.second();
		ExprDisambiguator v = this.extendWithFns(inheritedMethods(extends_clause)).
				extendWithSelf(that.getSpan()).extendWithVars(vars).extendWithFns(fns);
		
		return forObjectExprOnly(that,
				extends_clause,
				v.recurOnListOfDecl(that.getDecls()));
	}

	// Make sure we don't infinitely explore supertraits that are acyclic
	public static class HierarchyHistory {
		final private Set<Type> explored;
		
		public HierarchyHistory() {	explored = Collections.emptySet();	}
		private HierarchyHistory(Set<Type> explored) { this.explored = explored; }
		
		public HierarchyHistory explore(Type t) {
			return new HierarchyHistory(CollectUtil.union(explored, t));
		}
		
		public boolean hasExplored(Type t) {
			return explored.contains(t);
		}
	}
	
	/**
	 * Given a list of TraitTypeWhere that some trait or object extends,
	 * this method returns a list of method ids that the trait receives
	 * through inheritance. The implementation of this method is somewhat
	 * involved, since at this stage of compilation, not all types are
	 * fully formed. (In particular, types in extends clauses of types that
	 * are found in the GlobalEnvironment.)
	 * @param extended_traits
	 * @return
	 */
	private Set<IdOrOpOrAnonymousName> inheritedMethods(List<TraitTypeWhere> extended_traits) {	
		return inheritedMethodsHelper(new HierarchyHistory(), extended_traits);
	}

	private Set<IdOrOpOrAnonymousName> inheritedMethodsHelper(HierarchyHistory h,
			List<TraitTypeWhere> extended_traits) {
		Set<IdOrOpOrAnonymousName> methods = new HashSet<IdOrOpOrAnonymousName>();
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
				return Collections.emptySet();
			}
			
			TypeConsIndex tci = this._env.typeConsIndex(trait_name);
			if( tci instanceof TraitIndex ) {
				TraitIndex ti = (TraitIndex)tci;
				// Add dotted methods
				methods.addAll(ti.dottedMethods().firstSet());
				// Add getters
				methods.addAll(ti.getters().keySet());
				// Add setters
				methods.addAll(ti.setters().keySet());
				// For now we won't add functional methods. They are not received through inheritance.

				// Now recursively add methods from trait's extends clause
				
				methods.addAll(inheritedMethodsHelper(h, ti.extendsTypes()));
			}
			else {
				// Probably ANY
				return Collections.emptySet();
			}
			
			
		}
		return methods;
	}
	
	/**
	 * When recurring on a TraitDecl, we first need to extend the
	 * environment with all the newly bound static parameters that
	 * can be used in an expression context.
	 * TODO: Handle variables bound in where clauses.
	 * TODO: Insert inherited method names into the environment.
	 */
	@Override public Node forTraitDecl(final TraitDecl that) {
		ExprDisambiguator v = this.extendWithVars(extractStaticExprVars(that.getStaticParams()));
		List<TraitTypeWhere> extends_clause = v.recurOnListOfTraitTypeWhere(that.getExtendsClause());
		
		// Include trait declarations and inherited methods
		Pair<Set<Id>,Set<IdOrOpOrAnonymousName>> decl_names = extractDeclNames(that.getDecls());
		Set<Id> vars = decl_names.first();
		Set<IdOrOpOrAnonymousName> fns = decl_names.second();
		v = this.extendWithVars(extractStaticExprVars
				(that.getStaticParams())).
				extendWithFns(inheritedMethods(extends_clause)).
				extendWithSelf(that.getSpan()).extendWithVars(vars).extendWithFns(fns);
		
		return forTraitDeclOnly(that,
				v.recurOnListOfModifier(that.getMods()),
				(Id) that.getName().accept(v),
				v.recurOnListOfStaticParam(that.getStaticParams()),
				extends_clause,
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
		ExprDisambiguator v = this.extendWithVars(extractStaticExprVars(that.getStaticParams()));
		List<TraitTypeWhere> extends_clause = v.recurOnListOfTraitTypeWhere(that.getExtendsClause());
		
		// Include trait declarations and inherited methods
		Pair<Set<Id>,Set<IdOrOpOrAnonymousName>> decl_names = extractAbsDeclNames(that.getDecls());
		Set<Id> vars = decl_names.first();
		Set<IdOrOpOrAnonymousName> fns = decl_names.second();
		v = this.extendWithVars(extractStaticExprVars
				(that.getStaticParams())).
				extendWithFns(inheritedMethods(extends_clause)).
				extendWithSelf(that.getSpan()).
				extendWithVars(extractParamNames(that.getParams())).
				extendWithVars(vars).extendWithFns(fns);
		
		return forAbsObjectDeclOnly(that,
				v.recurOnListOfModifier(that.getMods()),
				(Id) that.getName().accept(v),
				v.recurOnListOfStaticParam(that.getStaticParams()),
				extends_clause,
				(WhereClause) that.getWhere().accept(v),
				v.recurOnOptionOfListOfParam(that.getParams()),
				v.recurOnOptionOfListOfBaseType(that.getThrowsClause()),
				(Contract) that.getContract().accept(v),
				v.recurOnListOfAbsDecl(that.getDecls()));
	}

	private Pair<Set<Id>,Set<IdOrOpOrAnonymousName>> extractAbsDeclNames(List<AbsDecl> decls) {
		final Set<IdOrOpOrAnonymousName> accessors = new HashSet<IdOrOpOrAnonymousName>();
		
		NodeDepthFirstVisitor<Set<Id>> var_finder = new NodeDepthFirstVisitor<Set<Id>>(){

			@Override
			public Set<Id> forAbsVarDecl(AbsVarDecl that) {
				return extractDefinedVarNames(that.getLhs());
			}
			@Override
			public Set<Id> forAbsFnDecl(AbsFnDecl that) {
				return Collections.emptySet();
			}		
		};
		NodeDepthFirstVisitor<Set<IdOrOpOrAnonymousName>> fn_finder = new NodeDepthFirstVisitor<Set<IdOrOpOrAnonymousName>>(){

			@Override
			public Set<IdOrOpOrAnonymousName> forAbsVarDecl(AbsVarDecl that) {
				return Collections.emptySet();
			}
			@Override
			public Set<IdOrOpOrAnonymousName> forAbsFnDecl(AbsFnDecl that) {
				if( isSetterOrGetter(that.getMods()) )
					accessors.add(that.getName());
				
				return Collections.singleton(that.getName());
			}
		};
		List<Set<Id>> vars_ = var_finder.recurOnListOfAbsDecl(decls);
		List<Set<IdOrOpOrAnonymousName>> fns_ = fn_finder.recurOnListOfAbsDecl(decls);
		
		Set<Id> vars = Useful.union(vars_);
		Set<IdOrOpOrAnonymousName> fns = new HashSet<IdOrOpOrAnonymousName>(Useful.union(fns_));
		
		// For every accessor, remove that fn from fns if there is also a variable.
		// See shadowing rules in section 7.3
		for( IdOrOpOrAnonymousName ioooan : accessors ) {
			if( vars.contains(ioooan) ) {
				fns.remove(ioooan);
			}
		}
		
		return Pair.make(vars, fns);
	}

	/**
	 * When recurring on an ObjectDecl, we first need to extend the
	 * environment with all the newly bound static parameters that can
	 * be used in an expression context, along with all the object parameters.
	 * TODO: Handle variables bound in where clauses.
	 * TODO: Insert inherited method names into the environment.
	 */
	@Override public Node forObjectDecl(final ObjectDecl that) {
		ExprDisambiguator v = this.extendWithVars(extractStaticExprVars(that.getStaticParams()));
		List<TraitTypeWhere> extends_clause = v.recurOnListOfTraitTypeWhere(that.getExtendsClause());
		
		// Include trait declarations and inherited methods
		Pair<Set<Id>,Set<IdOrOpOrAnonymousName>> decl_names = extractDeclNames(that.getDecls());
		Set<Id> vars = decl_names.first();
		Set<IdOrOpOrAnonymousName> fns = decl_names.second();
		v = this.extendWithVars(extractStaticExprVars
				(that.getStaticParams())).
				extendWithFns(inheritedMethods(extends_clause)).
				extendWithSelf(that.getSpan()).
				extendWithVars(extractParamNames(that.getParams())).
				extendWithVars(vars).extendWithFns(fns);

		return forObjectDeclOnly(that,
				v.recurOnListOfModifier(that.getMods()),
				(Id) that.getName().accept(v),
				v.recurOnListOfStaticParam(that.getStaticParams()),
				extends_clause,
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
		ExprDisambiguator v = extendWithVars(staticExprVars).extendWithVars(params);

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
		ExprDisambiguator v = extendWithVars(staticExprVars).extendWithVars(params);

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
	 * Currently we don't descend into dimensions or units.
	 */
	@Override
	public Node forTaggedDimType(TaggedDimType that) {
		return forTaggedDimTypeOnly(that,
				                    (Type)that.getType().accept(this),
				                    that.getDim(),
				                    that.getUnit());
	}

	/**
	 * Currently we don't descend into dimensions or units.
	 */
	@Override
	public Node forTaggedUnitType(TaggedUnitType that) {
		return forTaggedUnitTypeOnly(that,
					                 (Type)that.getType().accept(this),
					                 that.getUnit());
	}

	
	/**
	 * Currently we don't do any disambiguation of dimension or unit declarations.
	 */
	@Override public Node forDimDecl(DimDecl that) { return that; }
	@Override public Node forUnitDecl(UnitDecl that) { return that; }

	/**
	 * When recurring on a FnExpr, we first need to extend the
	 * environment with any newly bound static parameters that
	 * can be used in an expression context, along with all function
	 * parameters and 'self'.
	 */
	@Override public Node forFnExpr(FnExpr that) {
		Set<Id> staticExprVars = extractStaticExprVars(that.getStaticParams());
		Set<Id> params = extractParamNames(that.getParams());
		ExprDisambiguator v = extendWithVars(staticExprVars).extendWithVars(params);

		return forFnExprOnly(that,
				(IdOrOpOrAnonymousName) that.getName().accept(v),
				v.recurOnListOfStaticParam(that.getStaticParams()),
				v.recurOnListOfParam(that.getParams()),
				v.recurOnOptionOfType(that.getReturnType()),
				(WhereClause) that.getWhere().accept(v),
				v.recurOnOptionOfListOfBaseType(that.getThrowsClause()),
				(Expr) that.getBody().accept(v));
	}

	
	
	@Override
	public Node forCatch(Catch that) {
		ExprDisambiguator v = this.extendWithVars(Collections.singleton(that.getName()));
		return forCatchOnly(that,
							(Id)that.getName().accept(v),
				            v.recurOnListOfCatchClause(that.getClauses()));
	}

	/**
	 * Contracts are implicitly allowed to refer to a variable, "result."
	 */
	@Override
	public Node forContract(Contract that) {
		ExprDisambiguator v = extendsWithResult(that.getSpan());
		return forContractOnly(that,
				               v.recurOnOptionOfListOfExpr(that.getRequires()),
				               v.recurOnOptionOfListOfEnsuresClause(that.getEnsures()),
				               v.recurOnOptionOfListOfExpr(that.getInvariants()));
	}

	@Override
	public Node forGeneratedExpr(GeneratedExpr that) {
		Pair<List<GeneratorClause>,Set<Id>> pair = bindInListGenClauses(this, that.getGens());
		ExprDisambiguator extended_d = this.extendWithVars(pair.second());
		return forGeneratedExprOnly(that,
				                    (Expr)that.getExpr().accept(extended_d),
				                    pair.first());
	}

	@Override
	public Node forAccumulator(Accumulator that) {
		// Accumulator can bind variables
		Pair<List<GeneratorClause>, Set<Id>> pair = bindInListGenClauses(this, that.getGens());
		ExprDisambiguator extended_d = this.extendWithVars(pair.second());
		return forAccumulatorOnly(that,
				                  recurOnListOfStaticArg(that.getStaticArgs()),
				                  (OpName)that.getOpr().accept(this),
				                  pair.first(),
				                  (Expr)that.getBody().accept(extended_d)); 
	}

	/**
	 * An if clause has a generator that can potentially create a new binding.
	 * Here we must extend the context.
	 */
	@Override
	public Node forIfClause(IfClause that) {
		GeneratorClause gen = that.getTest();
		ExprDisambiguator e_d = this.extendWithVars(Useful.set(gen.getBind()));

		return forIfClauseOnly(that,
				(GeneratorClause)that.getTest().accept(this),
				(Block)that.getBody().accept(e_d));
	}

	/**
	 * While loops have generator clauses that can bind variables in the body.
	 */
	@Override
	public Node forWhile(While that) {
		GeneratorClause gen = that.getTest();
		ExprDisambiguator e_d = this.extendWithVars(Useful.set(gen.getBind()));

		return forWhileOnly(that,
				(GeneratorClause)that.getTest().accept(this),
				(Do)that.getBody().accept(e_d));
	}


	/**
	 * Typecase can bind new variables in the clauses.
	 */
	@Override
	public Node forTypecase(Typecase that) {
		Set<Id> bound_ids = Useful.set(that.getBindIds());
		ExprDisambiguator e_d = this.extendWithVars(bound_ids);

		return forTypecaseOnly(that, 
				that.getBindIds(),
				that.getBindExpr().isSome() ? Option.<Expr>some((Expr)that.getBindExpr().unwrap().accept(this)) : Option.<Expr>none(),
						e_d.recurOnListOfTypecaseClause(that.getClauses()),
						that.getElseClause().isSome() ? Option.<Block>some((Block)that.getElseClause().unwrap().accept(e_d)) : Option.<Block>none());
	}

	private static Pair<List<GeneratorClause>, Set<Id>> bindInListGenClauses(final ExprDisambiguator cur_disam, 
			                                                                 List<GeneratorClause> gens) {
		
		return IterUtil.fold(gens, Pair.<List<GeneratorClause>,Set<Id>>make(new LinkedList<GeneratorClause>(),new HashSet<Id>()), 
				new Lambda2<Pair<List<GeneratorClause>,Set<Id>>,GeneratorClause,Pair<List<GeneratorClause>,Set<Id>>>(){
			public Pair<List<GeneratorClause>, Set<Id>> value(
					Pair<List<GeneratorClause>, Set<Id>> arg0,
					GeneratorClause arg1) {
				// given the bindings thus far, rebuild the current GeneratorClause with the new bindings
				// pass along that generator's bindings.
				Set<Id> previous_bindings = arg0.second();
				ExprDisambiguator extended_d = cur_disam.extendWithVars(previous_bindings);
				GeneratorClause new_gen = (GeneratorClause)arg1.accept(extended_d);
				Set<Id> new_bindings = Useful.union(previous_bindings,Useful.set(arg1.getBind()));

				arg0.first().add(new_gen);
				return Pair.make(arg0.first(), new_bindings);
			}});
	}


	/**
	 * for loops have generator clauses that bind in later generator clauses.
	 */
	@Override
	public Node forFor(For that) {
		Pair<List<GeneratorClause>, Set<Id>> pair = bindInListGenClauses(this, that.getGens());	

		ExprDisambiguator new_disambiguator = this.extendWithVars(pair.second());
		return forForOnly(that,
				pair.first(),
				(DoFront)that.getBody().accept(new_disambiguator));
	}

	/** LetFns introduce local functions in scope within the body. */
	@Override public Node forLetFn(LetFn that) {
		Set<IdOrOpOrAnonymousName> definedNames = extractDefinedFnNames(that.getFns());
		ExprDisambiguator v = extendWithFns(definedNames);
		List<FnDef> fnsResult = v.recurOnListOfFnDef(that.getFns());
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
			Set<Id> objs = _env.explicitTypeConsNames(name);
			/* if (vars.isEmpty() && fns.isEmpty()) {
				vars = _env.onDemandVariableNames(name);
				fns = _env.onDemandFunctionNames(name);
			} */

			if (vars.size() == 1 && fns.isEmpty()) {
				Id newName = IterUtil.first(vars);

				if (newName.getApi().isNone() && newName == name && fields.isEmpty()) {
					// no change -- no need to recreate the VarRef
					return that;
				}
				else { result = new VarRef(that.getSpan(), newName); }
			}
			else if (vars.isEmpty() && !fns.isEmpty() ) {
				result = ExprFactory.makeFnRef(name,CollectUtil.makeList(fns));
				// TODO: insert correct number of to-infer arguments?
			}
			else if( vars.isEmpty() && fns.isEmpty() && objs.size() == 1 ) {
				result = ExprFactory.make_RewriteObjectRef(that.isParenthesized(), IterUtil.first(objs));
			}
			else if (!vars.isEmpty() || !fns.isEmpty() || !objs.isEmpty()) {
				// To be replaced by a 'shadowing' pass
				//Set<Id> varsFnsAndObjs = CollectUtil.union(CollectUtil.union(vars, fns), objs);
				//error("Name may refer to: " + NodeUtil.namesString(varsFnsAndObjs), name);
				return that;
			}
			else {
				// Turn off error message on this branch until we can ensure
				// that the VarRef doesn't resolve to an inherited method.
				// For now, assume it does refer to an inherited method.
				if (fields.isEmpty()) {
					// no change -- no need to recreate the VarRef
					error("Variable " + name + " could not be disambiguated.", name);
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
	public Node for_RewriteObjectRef(_RewriteObjectRef that) {
		Id obj_name = that.getObj();

		Set<Id> objs = _env.explicitTypeConsNames(obj_name);
		if( objs.isEmpty() ) {
			objs = _env.onDemandTypeConsNames(obj_name);
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
		}

		if( fns.isEmpty() ) {
			// Could be a singleton object with static arguments.
			Set<Id> types = _env.explicitTypeConsNames(fn_name);
			if( types.isEmpty() ) {
				types = _env.onDemandTypeConsNames(fn_name);
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

	/**
	 * Disambiguates an OpRef, but instead of reporting an error if it cannot be
	 * disambiguated, it returns NONE, which other methods can then used to decide
	 * if they want to report an error.
	 */
	private Option<OpRef> opRefHelper(OpRef that) {
		OpName op_name = IterUtil.first(that.getOps());
		Set<OpName> ops = _env.explicitFunctionNames(op_name);
		if (ops.isEmpty()) {
			ops = _env.onDemandFunctionNames(op_name);
		}

		if (ops.isEmpty()) {
			return Option.none();
		}

		OpRef result = new OpRef(that.getSpan(),that.isParenthesized(),op_name,CollectUtil.makeList(ops),that.getStaticArgs());
		return Option.<OpRef>some(result);
	}
	
	@Override
	public Node forOpExpr(OpExpr that) {
		// OpExpr checks to make sure its OpRef can be disambiguated, since
		// forOpRef will not automatically report an error.
		OpRef op_result;
		Option<OpRef> _op_result = opRefHelper(that.getOp()); 
		if( _op_result.isSome() ) {
			op_result = (OpRef)_op_result.unwrap();
		}
		else {
			String op_name = IterUtil.first(that.getOp().getOps()).stringName();
			error("Operator " + op_name + " cannot be disambiguated.", that.getOp());
			op_result = (OpRef)recur(that.getOp());
		}
		
        List<Expr> args_result = recurOnListOfExpr(that.getArgs());
        return forOpExprOnly(that, op_result, args_result);
	}

	@Override public Node forOpRef(OpRef that) {
		Option<OpRef> result_ = opRefHelper(that);

		if ( result_.isNone() ) {
			// Make sure to populate the 'originalName' field.
			return new OpRef(that.getSpan(),that.isParenthesized(),IterUtil.first(that.getOps()), that.getOps(),that.getStaticArgs());
		}
		else {
			return result_.unwrap();
		}
	}

	@Override public Node forLabel(Label that) {
		Id newName = (Id) that.getName().accept(this);
		ExprDisambiguator dis = new ExprDisambiguator(_env, _errors,
				Option.wrap(that.getName()));
		Block newBody = (Block) that.getBody().accept(dis);
		return super.forLabelOnly(that, newName, newBody);
	}

	@Override public Node forExitOnly(Exit that, Option<Id> target_result, Option<Expr> returnExpr_result) {
		Option<Id> target = target_result.isSome() ? target_result : _innerMostLabel;
		Option<Expr> with = returnExpr_result.isSome() ? 
				returnExpr_result :
				wrap((Expr)new VoidLiteralExpr(that.getSpan()));
		
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

	@Override
	public Node forGrammarDef(GrammarDef that) {
		return that;
	}	
}