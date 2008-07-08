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

import static com.sun.fortress.compiler.Types.ANY;
import static com.sun.fortress.compiler.Types.BOTTOM;
import static com.sun.fortress.exceptions.InterpreterBug.bug;
import static edu.rice.cs.plt.debug.DebugUtil.debug;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sun.fortress.compiler.Types;
import com.sun.fortress.compiler.typechecker.TypeAnalyzer.SubtypeHistory;
import com.sun.fortress.nodes.AnyType;
import com.sun.fortress.nodes.ArrayType;
import com.sun.fortress.nodes.ArrowType;
import com.sun.fortress.nodes.BaseDim;
import com.sun.fortress.nodes.Block;
import com.sun.fortress.nodes.BottomType;
import com.sun.fortress.nodes.DimExpr;
import com.sun.fortress.nodes.DimRef;
import com.sun.fortress.nodes.Domain;
import com.sun.fortress.nodes.Effect;
import com.sun.fortress.nodes.ExponentDim;
import com.sun.fortress.nodes.Expr;
import com.sun.fortress.nodes.ExtentRange;
import com.sun.fortress.nodes.FixedPointType;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.Indices;
import com.sun.fortress.nodes.IntExpr;
import com.sun.fortress.nodes.IntersectionType;
import com.sun.fortress.nodes.Label;
import com.sun.fortress.nodes.MatrixType;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.NodeAbstractVisitor;
import com.sun.fortress.nodes.NodeDepthFirstVisitor;
import com.sun.fortress.nodes.NodeDepthFirstVisitor_void;
import com.sun.fortress.nodes.NodeUpdateVisitor;
import com.sun.fortress.nodes.Op;
import com.sun.fortress.nodes.OpDim;
import com.sun.fortress.nodes.ProductDim;
import com.sun.fortress.nodes.QuotientDim;
import com.sun.fortress.nodes.StaticArg;
import com.sun.fortress.nodes.StaticParam;
import com.sun.fortress.nodes.TaggedDimType;
import com.sun.fortress.nodes.TaggedUnitType;
import com.sun.fortress.nodes.TraitType;
import com.sun.fortress.nodes.TupleType;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes.UnionType;
import com.sun.fortress.nodes.VarType;
import com.sun.fortress.nodes.VarargTupleType;
import com.sun.fortress.nodes.VoidType;
import com.sun.fortress.nodes.WhereClause;
import com.sun.fortress.nodes._InferenceVarType;
import com.sun.fortress.nodes._RewriteGenericArrowType;
import com.sun.fortress.nodes._RewriteGenericSingletonType;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.useful.MultiMap;
import com.sun.fortress.useful.NI;

import edu.rice.cs.plt.collect.CollectUtil;
import edu.rice.cs.plt.collect.ConsList;
import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.lambda.Lambda;
import edu.rice.cs.plt.tuple.Option;

/**
 * An immutable representation of constraints on a set of inference variables.  Certain typing
 * assertions (such as subtyping) can be made under the assumption that a certain ConstraintFormula
 * is satisfied.  Combining a number of these formulas, it is possible to determine whether a
 * certain portion of a program is well-formed (and, if so, how the inference variables that appear
 * within the program should be instantiated).
 */
public abstract class ConstraintFormula {

    /** Merge this and another formula by asserting that they must both be true. */
    public abstract ConstraintFormula and(ConstraintFormula c, SubtypeHistory history);

    /** Merge this and another formula by asserting that one of the two must be true. */
    public abstract ConstraintFormula or(ConstraintFormula c, SubtypeHistory history);


    /**
     * Apply a type substitution to the contents of a formula.  Callers assume responsibility
     * for guaranteeing that the substitution will not change the satisfiability (or truth) of the
     * formula.  Substitutions of bounded inference variables must map to other (or the same)
     * inference variables, not arbitrary types.
     */
    public abstract ConstraintFormula applySubstitution(Lambda<Type, Type> sigma);

    /** Determine whether the formula is true for all inference variable instantiations. */
    public abstract boolean isTrue();

    /** Determine whether the formula is false for all inference variable instantiations. */
    public abstract boolean isFalse();

    /** Determine whether there exists some choice for inference variables that makes the formula true. */
    public boolean isSatisfiable() { return !isFalse(); }

    /** Get the map of inference variable types to upper bounds **/
    public Map<_InferenceVarType, Type> getMap(){
    	return Collections.emptyMap();
    }
    

    public static final ConstraintFormula TRUE = new ConstraintFormula() {
        public ConstraintFormula and(ConstraintFormula f, SubtypeHistory history) { return f; }
        public ConstraintFormula or(ConstraintFormula f, SubtypeHistory history) { return this; }
        public ConstraintFormula applySubstitution(Lambda<Type, Type> sigma) { return this; }
        public boolean isTrue() { return true; }
        public boolean isFalse() { return false; }
        public String toString() { return "(true)"; }
    };

    public static final ConstraintFormula FALSE = new ConstraintFormula() {
        public ConstraintFormula and(ConstraintFormula f, SubtypeHistory history) { return this; }
        public ConstraintFormula or(ConstraintFormula f, SubtypeHistory history) { return f; }
        public ConstraintFormula applySubstitution(Lambda<Type, Type> sigma) { return this; }
        public boolean isTrue() { return false; }
        public boolean isFalse() { return true; }
        public String toString() { return "(false)"; }
    };

    /**
     * This is a prototype class in an attempt to more closely implement the algorithm
     * described in 
     */
    public static class SimpleFormula extends ConstraintFormula {
    	
    	final private MultiMap<_InferenceVarType,Type> ivarUpperBounds;
    	final private MultiMap<_InferenceVarType,Type> ivarLowerBounds;
    	
    	final private SubtypeHistory history;
    	
    	public SimpleFormula(SubtypeHistory h) {
    		ivarUpperBounds = new MultiMap<_InferenceVarType,Type>();
    		ivarLowerBounds = new MultiMap<_InferenceVarType,Type>();
    		history = h;
    	}
    	
    	private SimpleFormula(MultiMap<_InferenceVarType,Type> ivarUpperBounds,
    			MultiMap<_InferenceVarType,Type> ivarLowerBounds, SubtypeHistory h) {

    		MultiMap<_InferenceVarType,Type> newuppers = new MultiMap<_InferenceVarType,Type>(ivarUpperBounds);
    		MultiMap<_InferenceVarType,Type> newlowers = new MultiMap<_InferenceVarType,Type>(ivarLowerBounds);
    		for(Map.Entry<_InferenceVarType,Set<Type>> entry: ivarUpperBounds.entrySet()){
    			_InferenceVarType ivar = entry.getKey();
    			Set<Type> uppers = entry.getValue();
    			for(Type t: uppers){
    				if(t instanceof _InferenceVarType){
    					newlowers.putItem((_InferenceVarType)t, ivar);
    				}
    			}
    		}
    		for(Map.Entry<_InferenceVarType,Set<Type>> entry: ivarLowerBounds.entrySet()){
    			_InferenceVarType ivar = entry.getKey();
    			Set<Type> lowers = entry.getValue();
    			for(Type t: lowers){
    				if(t instanceof _InferenceVarType){
    					newuppers.putItem((_InferenceVarType)t, ivar);
    				}
    			}
    		}	

    		this.ivarLowerBounds = newlowers;
    		this.ivarUpperBounds = newuppers;
    		history = h;
    	}
    	
    	// Never returns null
    	private Set<Type> getUpperBounds(_InferenceVarType ivar) {
    		return this.ivarUpperBounds.containsKey(ivar) ? 
    				this.ivarUpperBounds.get(ivar) :
    				Collections.<Type>emptySet();
    	}
    	
    	// Never returns null
    	private Set<Type> getLowerBounds(_InferenceVarType ivar) {
    		return this.ivarLowerBounds.containsKey(ivar) ? 
    				this.ivarLowerBounds.get(ivar) :
    				Collections.<Type>emptySet();
    	}
    	    	
		@Override
		public boolean isSatisfiable() {
			return this.solve().isSatisfiable();
		}

		@Override
		public ConstraintFormula and(ConstraintFormula c, SubtypeHistory history) {
            if (c.isTrue()) { return this; }
            else if (c.isFalse()) { return c; }
            else if (c instanceof SimpleFormula) { return merge((SimpleFormula) c, history); }
            else { throw new RuntimeException("unexpected case"); }
		}

		// Combine the upper and lower bounds for each inference variable
		private ConstraintFormula merge(SimpleFormula c, SubtypeHistory hist) {
			MultiMap<_InferenceVarType,Type> new_upper_bounds = new MultiMap<_InferenceVarType,Type>();
			MultiMap<_InferenceVarType,Type> new_lower_bounds = new MultiMap<_InferenceVarType,Type>();
			
			Set<_InferenceVarType> all_ivars = CollectUtil.union(ivarUpperBounds.keySet(), 
                                                                 c.ivarUpperBounds.keySet());
			all_ivars = CollectUtil.union(ivarLowerBounds.keySet(), all_ivars);
			all_ivars = CollectUtil.union(c.ivarLowerBounds.keySet(), all_ivars);
			
			for( _InferenceVarType ivar : all_ivars ) {
				new_upper_bounds.putItems(ivar, CollectUtil.union(getUpperBounds(ivar),
						                                          c.getUpperBounds(ivar)));
				new_lower_bounds.putItems(ivar, CollectUtil.union(getLowerBounds(ivar),
                                                                  c.getLowerBounds(ivar)));
			}
			return new SimpleFormula(new_upper_bounds, new_lower_bounds, hist);
		}
		
		@Override
		public ConstraintFormula applySubstitution(final Lambda<Type, Type> sigma) {
			MultiMap<_InferenceVarType,Type> new_uppers = new MultiMap<_InferenceVarType,Type>();
			MultiMap<_InferenceVarType,Type> new_lowers = new MultiMap<_InferenceVarType,Type>();
			for( Map.Entry<_InferenceVarType, Set<Type>> entry : this.ivarUpperBounds.entrySet() ) {
				_InferenceVarType ivar = entry.getKey();
				Set<Type> u_bounds = entry.getValue();
				new_uppers.putItems((_InferenceVarType)sigma.value(ivar), CollectUtil.asSet(IterUtil.map(u_bounds, sigma)));
			}
			for( Map.Entry<_InferenceVarType, Set<Type>> entry : this.ivarLowerBounds.entrySet() ) {
				_InferenceVarType ivar = entry.getKey();
				Set<Type> l_bounds = entry.getValue();
				new_lowers.putItems((_InferenceVarType)sigma.value(ivar), CollectUtil.asSet(IterUtil.map(l_bounds, sigma)));
			}
			return new SimpleFormula(new_uppers, new_lowers, this.history);
		}

		@Override
		public boolean isFalse() {
			// We could actually do some simplification if we wanted.
			return false;
		}

		@Override
		public boolean isTrue() {
			// We could actually do some simplification if we wanted.
			return false;
		}
		
		/** Find a cycle in naked inference variables starting at the given one */
		private Option<ConsList<_InferenceVarType>> findCycle(_InferenceVarType cur_start, final ConsList<_InferenceVarType> cur_path) {
			assert(cur_path.size() > 0);
			// First element of path is the thing we are looking for
			// cur_start is where we should start from
			final _InferenceVarType var_to_find = IterUtil.first(cur_path);
			// we will always move in the upwards direction, towards Object..
			Set<Type> next_types = new HashSet<Type>();
			next_types.addAll(this.getUpperBounds(cur_start));
			// Now visit each next_type in turn. If it's a naked inference var, check to see if we have
			// found a cycle. If not, recur. If it's not a InfVar, keep going.
			for( Type next_type : next_types ) {
				Option<ConsList<_InferenceVarType>> r = next_type.accept(new NodeDepthFirstVisitor<Option<ConsList<_InferenceVarType>>>(){
					@Override public Option<ConsList<_InferenceVarType>> defaultCase(Node that) {
						return Option.none();
					}
					@Override
					public Option<ConsList<_InferenceVarType>> for_InferenceVarType(_InferenceVarType that) {
						// See if this var forms a cycle, and if not recur
						if( that.equals(var_to_find) ) return Option.some(cur_path);
						else return findCycle(that, 
									CollectUtil.makeConsList(ConsList.append(cur_path, ConsList.singleton(that))));
					}
					
				});
				// If we found a cycle, we are done.
				if(r.isSome()) {
					return r;
				}
			}
			return Option.none();
		}
		
		@Override
		public String toString() {
			StringBuffer result = new StringBuffer();
			Set<_InferenceVarType> all_ivars = CollectUtil.union(ivarUpperBounds.keySet(), 
                                                                 ivarLowerBounds.keySet());
			for( _InferenceVarType ivar : all_ivars ) {
				String lbound = ivarLowerBounds.containsKey(ivar) ? ivarLowerBounds.get(ivar).toString() : Collections.emptySet().toString();
				String ubound = ivarUpperBounds.containsKey(ivar) ? ivarUpperBounds.get(ivar).toString() : Collections.emptySet().toString();
				result.append(lbound + " <: " + ivar + " <: " + ubound + ", ");
			}
			return result.toString();
		}
		
		private ConstraintFormula replaceAndRemove(final _InferenceVarType new_ivar, final List<_InferenceVarType> to_remove) {
			final Lambda<_InferenceVarType,_InferenceVarType> sigma = new Lambda<_InferenceVarType,_InferenceVarType>(){
				public _InferenceVarType value(_InferenceVarType arg0) {
					if( to_remove.contains(arg0) )
						return new_ivar;
					else
						return arg0;
				}};
			
			final NodeUpdateVisitor replacer = new NodeUpdateVisitor() {
				@Override
				public Node for_InferenceVarType(_InferenceVarType that) {
					return sigma.value(that);
				}
			};
			
			Lambda<Type,Type> lambda = new Lambda<Type,Type>(){
				public Type value(Type arg0) { return (Type)arg0.accept(replacer); }};
			
			MultiMap<_InferenceVarType,Type> new_uppers = new MultiMap<_InferenceVarType,Type>();
			MultiMap<_InferenceVarType,Type> new_lowers = new MultiMap<_InferenceVarType,Type>();
			for( Map.Entry<_InferenceVarType, Set<Type>> entry : this.ivarUpperBounds.entrySet() ) {
				_InferenceVarType ivar = entry.getKey();
				Set<Type> u_bounds = entry.getValue();
				new_uppers.putItems((_InferenceVarType)sigma.value(ivar),
						CollectUtil.asSet(IterUtil.map(u_bounds, lambda)));
			}
			for( Map.Entry<_InferenceVarType, Set<Type>> entry : this.ivarLowerBounds.entrySet() ) {
				_InferenceVarType ivar = entry.getKey();
				Set<Type> l_bounds = entry.getValue();
				new_lowers.putItems((_InferenceVarType)sigma.value(ivar), CollectUtil.asSet(IterUtil.map(l_bounds, lambda)));
			}
			return new SimpleFormula(new_uppers, new_lowers, this.history);
		}
		
		// Returns a solved constraint formula, solved by doing the steps in 20.2 of spec1 beta
		private ConstraintFormula solve() {
			// 1.) for every cycle of constraints of naked prime static variables...
			Set<_InferenceVarType> ivars = CollectUtil.union(ivarLowerBounds.keySet(), ivarUpperBounds.keySet());
			for( _InferenceVarType ivar : ivars ) {
				Option<ConsList<_InferenceVarType>> cycle =
					findCycle(ivar, ConsList.singleton(ivar));
				if( cycle.isSome() ) {
					// if we have a cycle, replace all ivars in the cycle with any one
					final _InferenceVarType chosen_ivar = ConsList.first(cycle.unwrap());
					final List<_InferenceVarType> rest = CollectUtil.makeList(ConsList.rest(cycle.unwrap()));
					return this.replaceAndRemove(chosen_ivar, rest).solve();
				}
			}

			
			// 3.) If some primed static variable ...
			Map<_InferenceVarType,Type> lubs = solveHelper(ivars, ivarUpperBounds, 
					new Lambda<Set<Type>,Type>(){
						public Type value(Set<Type> arg0) {
							return arg0.size() == 1 ? IterUtil.first(arg0) : NodeFactory.makeIntersectionType(arg0);
						}});
			
			// 4.) for every naked static prime variable ...
			// TODO: check to see if invariant is being violated by not using the new bounds from
			// above here where we use ivarLowerbounds.
			Map<_InferenceVarType,Type> glbs = solveHelper(ivars, ivarLowerBounds,
					new Lambda<Set<Type>,Type>(){
						public Type value(Set<Type> arg0) {
							return arg0.size() == 1 ? IterUtil.first(arg0) : NodeFactory.makeUnionType(arg0);
						}});
			
			//5.) The inferred type is the intersection of its expanded lower and upper bounds
			Map<_InferenceVarType,Type> inferred_types = new HashMap<_InferenceVarType,Type>();
			for( _InferenceVarType ivar : ivars ) {
				if( lubs.containsKey(ivar) && glbs.containsKey(ivar) ) {
					Type lub = lubs.get(ivar);
					Type glb = glbs.get(ivar);
					if( this.history.subtypeNormal(glb, lub).equals(FALSE) ) {
						return FALSE;
					}
					
					inferred_types.put(ivar, NodeFactory.makeIntersectionType(lubs.get(ivar), glbs.get(ivar)));
				}
				else if( lubs.containsKey(ivar) ) {
					inferred_types.put(ivar, lubs.get(ivar));
				}
				else if( glbs.containsKey(ivar) ) {
					inferred_types.put(ivar, glbs.get(ivar));
				}
				else {
					return bug("Is this a bug?");
				}
			}
			
			return new SolvedSimpleFormula(inferred_types);
		}
		
		private Map<_InferenceVarType,Type> solveHelper(Set<_InferenceVarType> ivars, 
				                                        MultiMap<_InferenceVarType,Type> bounds,
				                                        Lambda<Set<Type>,Type> type_maker) {
			// 2.) for every naked static prime variable ...
			final Map<_InferenceVarType,Type> lubs_or_glbs = new HashMap<_InferenceVarType,Type>();
			for(_InferenceVarType ivar: ivars){
				if( bounds.containsKey(ivar) ) {
					Type lub_or_glb = type_maker.value(bounds.get(ivar)); 
					lubs_or_glbs.put(ivar, lub_or_glb);
				}
			}
			
			MultiMap<_InferenceVarType,Type> new_bounds = new MultiMap<_InferenceVarType,Type>();
			
			for(final _InferenceVarType t: ivars){
				Set<Type> uis = bounds.get(t);
				if( uis != null ) {
					for(Type  ui : uis){
						NodeUpdateVisitor v=new NodeUpdateVisitor(){

							@Override
							public Node for_InferenceVarType(_InferenceVarType that) {
								if(that.equals(t) || !lubs_or_glbs.containsKey(that)){
									return that;
								}
								else {
									return lubs_or_glbs.get(that);
								}
							}	
						};
						Type lub_or_glb=(Type)ui.accept(v);
						new_bounds.putItem(t, lub_or_glb);
					}
				}
			}
			
			if(!new_bounds.equals(bounds)){
				//return new SimpleFormula2(new_bounds,ivarLowerBounds).solve();
				return solveHelper(ivars, new_bounds, type_maker);
			}
			
			for(final _InferenceVarType t: ivars){
				Type lub = lubs_or_glbs.get(t);
				if( lub != null ) {
					final boolean[] flags = {false,false};
					NodeDepthFirstVisitor_void v= new NodeDepthFirstVisitor_void(){
						@Override
						public void for_InferenceVarType(
								_InferenceVarType that) {
							if(that.equals(t))
								flags[1]=true;
							else
								flags[0]=true;
						}	
					};
					lub.accept(v);
					if(flags[0])
						lubs_or_glbs.put(t, Types.ANY);
					else if(flags[1]){
						Id x = new Id("x");
						Type new_lub = (Type)lubs_or_glbs.get(t).accept(new InferenceVarReplacer(Collections.<_InferenceVarType,Type>singletonMap(t, new VarType(x))));
						lubs_or_glbs.put(t, NodeFactory.makeFixedPointType(x, new_lub));
					}
				}
			}
			
			return lubs_or_glbs;
		}
		
		// Created as the result of calling 'solve()'
		private static class SolvedSimpleFormula extends ConstraintFormula {
			private final Map<_InferenceVarType, Type> inferredTypes;			
			
			public SolvedSimpleFormula(Map<_InferenceVarType, Type> inferred_types) {
				this.inferredTypes = inferred_types;
			}

			// For this implementation, getMap() actually does hard work! It
			// implements section 20.3 of the specification.
			@Override
			public Map<_InferenceVarType, Type> getMap() {
				return Collections.unmodifiableMap(inferredTypes);
			}

			@Override
			public ConstraintFormula and(ConstraintFormula c,
					SubtypeHistory history) { 
				return bug("Once constraint has been solved, this should not be called"); 
			}

			@Override
			public ConstraintFormula applySubstitution(Lambda<Type, Type> sigma) {
				return bug("Once constraint has been solved, this should not be called");
			}

			@Override public boolean isFalse() { return false; }
			@Override public boolean isTrue() { return true; }

			@Override
			public ConstraintFormula or(ConstraintFormula c,
					SubtypeHistory history) {
				return bug("Once constraint has been solved, this should not be called");
			}
		}
		
		@Override
		public Map<_InferenceVarType, Type> getMap() {
			return this.solve().getMap();
		}

		@Override
		public ConstraintFormula or(ConstraintFormula c, SubtypeHistory history) {
			if( c.equals(TRUE) ) {
				return TRUE;
			}
			else if( c.equals(FALSE) ) {
				return this;
			}
			else {
				// For the time-being we are going to do what SimpleFormula did and return this,
				// but this is only used because there is an 'or' in traitSubTrait of TypeAnalyzer.
				// Another way of implemting traitSubTrait may be possible.
				//return NI.nyi();
				return this;
			}
		}
    }
    
    /** A conjunction of a number of binding constraints on inference variables.
     *  Clients are responsible for insuring that all constructed formulas are
     *  neither unsatisfiable (due to conflicting bounds on a variable) nor
     *  true (due to trivial bounds).
     */
//    public static class SimpleFormula extends ConstraintFormula {
//        private Map<_InferenceVarType, Type> _upperBounds;
//        private Map<_InferenceVarType, Type> _lowerBounds;
//
//        private SimpleFormula(Map<_InferenceVarType, Type> upperBounds,
//                              Map<_InferenceVarType, Type> lowerBounds) {
//            _upperBounds = upperBounds;
//            _lowerBounds = lowerBounds;
//        }
//
//
//
//        @Override
//		public Map<_InferenceVarType, Type> getMap() {
//			return Collections.unmodifiableMap(_upperBounds);
//		}
//
//
//
//		public ConstraintFormula and(ConstraintFormula f, SubtypeHistory history) {
//            if (f.isTrue()) { return this; }
//            else if (f.isFalse()) { return f; }
//            else if (f instanceof SimpleFormula) { return merge((SimpleFormula) f, history); }
//            else { throw new RuntimeException("unexpected case"); }
//        }
//
//        public ConstraintFormula or(ConstraintFormula f, SubtypeHistory history) {
//            if (f.isTrue()) { return f; }
//            else if (f.isFalse()) { return this; }
//            else {
//                // simplification for now -- arbitrarily pick one
//                return this;
//            }
//        }
//
//        public ConstraintFormula applySubstitution(Lambda<Type, Type> sigma) {
//            Map<_InferenceVarType, Type> newUppers =
//                new HashMap<_InferenceVarType, Type>(_upperBounds.size());
//            Map<_InferenceVarType, Type> newLowers =
//                new HashMap<_InferenceVarType, Type>(_lowerBounds.size());
//            for (Map.Entry<_InferenceVarType, Type> e : _upperBounds.entrySet()) {
//                newUppers.put((_InferenceVarType) sigma.value(e.getKey()), sigma.value(e.getValue()));
//            }
//            for (Map.Entry<_InferenceVarType, Type> e : _lowerBounds.entrySet()) {
//                newLowers.put((_InferenceVarType) sigma.value(e.getKey()), sigma.value(e.getValue()));
//            }
//            return new SimpleFormula(newUppers, newLowers);
//        }
//
//        public boolean isTrue() { return false; }
//        public boolean isFalse() { return false; }
//
//        public String toString() {
//            StringBuilder result = new StringBuilder();
//            boolean first = true;
//            for (_InferenceVarType t :
//                     CollectUtil.union(_upperBounds.keySet(), _lowerBounds.keySet())) {
//                if (first) { result.append("("); first = false; }
//                else { result.append(", "); }
//                if (_upperBounds.containsKey(t)) {
//                    if (_lowerBounds.containsKey(t)) {
//                        result.append(_lowerBounds.get(t));
//                        result.append(" <: ");
//                    }
//                    result.append(t);
//                    result.append(" <: ");
//                    result.append(_upperBounds.get(t));
//                }
//                else {
//                    result.append(t);
//                    result.append(" :> ");
//                    result.append(_lowerBounds.get(t));
//                }
//            }
//            result.append(")");
//            return result.toString();
//        }
//
//        
//        private boolean unionHelper(Type visited, final Type member){
//    		NodeAbstractVisitor<Boolean> v = new NodeAbstractVisitor<Boolean>(){
//    			@Override
//				public Boolean forUnionType(UnionType that) {
//    				return that.getElements().contains(member);
//				}
//				@Override
//				public Boolean forType(Type that) {
//					return that.equals(member);
//				}
//    		};
//    		return visited.accept(v);
//        }
//        
//        private boolean intersectionHelper(Type visited, final Type member){
//    		NodeAbstractVisitor<Boolean> v = new NodeAbstractVisitor<Boolean>(){
//    			@Override
//				public Boolean forIntersectionType(IntersectionType that) {
//    				return that.getElements().contains(member);
//				}
//				@Override
//				public Boolean forType(Type that) {
//					return that.equals(member);
//				}
//    		};
//    		return visited.accept(v);
//        }
//        
//        /*
//         *  Conservatively checks whether the constraint f is implied by
//         *  this. Used to terminate merge.
//         * 
//         */
//        private boolean implies(SimpleFormula f){
//        	boolean result = true;
//        	for(_InferenceVarType inf: f._lowerBounds.keySet()){
//        		Type newlower = f._lowerBounds.get(inf);
//        		Type oldlower = this._lowerBounds.get(inf);
//        		if(oldlower==null){
//        			return false;
//        		}
//        		result&= (unionHelper(oldlower,newlower) || intersectionHelper(newlower,oldlower));
//        	}
//        	for(_InferenceVarType inf: f._upperBounds.keySet()){
//        		final Type newupper = f._upperBounds.get(inf);
//        		final Type oldupper = this._upperBounds.get(inf);
//        		if(oldupper==null){
//        			return false;
//        		}
//        		result&= (intersectionHelper(oldupper, newupper) || intersectionHelper(newupper,oldupper)); 
//        	}
//        	return result;
//        }
//        
//        private ConstraintFormula merge(SimpleFormula f, SubtypeHistory h) {
//            debug.logStart(new String[]{"this", "f"}, this, f);
//            // Check whether this implies f
//            // if so don't merge them just return this
//            if(implies(f)){
//            	return this;
//            }
//            Map<_InferenceVarType, Type> uppers = new HashMap<_InferenceVarType, Type>();
//            Map<_InferenceVarType, Type> lowers = new HashMap<_InferenceVarType, Type>();
//            ConstraintFormula conditions = TRUE;
//            Set<_InferenceVarType> upperVars = CollectUtil.union(_upperBounds.keySet(),
//                                                                f._upperBounds.keySet());
//            Set<_InferenceVarType> lowerVars = CollectUtil.union(_lowerBounds.keySet(),
//                                                                f._lowerBounds.keySet());
//            // Optimization may be possible here -- Sukyoung
//            // Go through all inference variables from both constraints
//            for (_InferenceVarType t : CollectUtil.union(upperVars, lowerVars)) {
//                Type upper = null;
//                Type lower = null;
//                if (_upperBounds.containsKey(t) && f._upperBounds.containsKey(t)) {
//                    upper = h.meetNormal(_upperBounds.get(t), f._upperBounds.get(t));
//                }
//                else if (_upperBounds.containsKey(t)) { upper = _upperBounds.get(t); }
//                else if (f._upperBounds.containsKey(t)) { upper = f._upperBounds.get(t); }
//                if (_lowerBounds.containsKey(t) && f._lowerBounds.containsKey(t)) {
//                    lower = h.joinNormal(_lowerBounds.get(t), f._lowerBounds.get(t));
//                }
//                else if (_lowerBounds.containsKey(t)) { lower = _lowerBounds.get(t); }
//                else if (f._lowerBounds.containsKey(t)) { lower = f._lowerBounds.get(t); }
//
//                // determine conditions necessary for enforcing lower <: upper
//                if (_upperBounds.containsKey(t) && f._lowerBounds.containsKey(t) ||
//                    _lowerBounds.containsKey(t) && f._upperBounds.containsKey(t)) {
//                    // TODO: there may be a circular dependency here
//                    conditions = conditions.and(h.subtypeNormal(lower, upper), h);
//                }
//                if (upper != null) { uppers.put(t, upper); }
//                if (lower != null) { lowers.put(t, lower); }
//                if (conditions.isFalse()) { break; }
//            }
//            ConstraintFormula result = new SimpleFormula(uppers, lowers).and(conditions, h);
//            debug.logEnd("result", result);
//            return result;
//        }
//    }


    public static ConstraintFormula upperBound(_InferenceVarType var, Type bound, SubtypeHistory history) {
        debug.logStart(new String[]{"var","upperBound"}, var, bound);
        if (history.subtypeNormal(ANY, bound).isTrue()) {
            debug.logEnd("result", TRUE);
            return TRUE;
        }
        else{
        	MultiMap<_InferenceVarType,Type> uppers = new MultiMap<_InferenceVarType,Type>();
        	uppers.putItem(var, bound);
	        ConstraintFormula result =
	        	new SimpleFormula(uppers, new MultiMap<_InferenceVarType,Type>(), history);
	        debug.logEnd("result", result);
	        return result;
        }
    }

    private ConstraintFormula solve() {
		return this;
	}

	public static ConstraintFormula lowerBound(_InferenceVarType var, Type bound, SubtypeHistory history) {
        debug.logStart(new String[]{"var","lowerBound"}, var, bound);
        if (history.subtypeNormal(bound, BOTTOM).isTrue()) {
            debug.logEnd("result", TRUE);
            return TRUE;
        }
        else {
        	MultiMap<_InferenceVarType,Type> lowers = new MultiMap<_InferenceVarType,Type>();
        	lowers.putItem(var, bound);
            ConstraintFormula result =
            	new SimpleFormula(new MultiMap<_InferenceVarType,Type>(), lowers, history);
            debug.logEnd("result", result);
            return result;
        }
    }

    public static ConstraintFormula fromBoolean(boolean b) {
        return b ? TRUE : FALSE;
    }

    /**
     * AND together all of the given constraints.
     */
    public static ConstraintFormula bigAnd(Iterable<? extends ConstraintFormula> constraints,
    		                               SubtypeHistory hist) {
    	ConstraintFormula result = TRUE;
    	for( ConstraintFormula constraint : constraints ) {
    		result = result.and(constraint, hist);
    	}
    	return result;
    }
    


}
