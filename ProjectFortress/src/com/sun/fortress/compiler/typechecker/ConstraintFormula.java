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
import com.sun.fortress.exceptions.InterpreterBug;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.NodeDepthFirstVisitor;
import com.sun.fortress.nodes.NodeDepthFirstVisitor_void;
import com.sun.fortress.nodes.NodeUpdateVisitor;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes.TypeDepthFirstVisitor;
import com.sun.fortress.nodes.VarType;
import com.sun.fortress.nodes._InferenceVarType;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.SourceLoc;
import com.sun.fortress.useful.IMultiMap;
import com.sun.fortress.useful.MultiMap;
import com.sun.fortress.useful.NI;
import com.sun.fortress.useful.Useful;
import com.sun.tools.apt.mirror.type.TypeMaker;

import edu.rice.cs.plt.collect.CollectUtil;
import edu.rice.cs.plt.collect.ConsList;
import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.lambda.Lambda;
import edu.rice.cs.plt.lambda.Runnable1;
import edu.rice.cs.plt.tuple.Option;

/**
 * An immutable representation of constraints on a set of inference variables.  Certain typing
 * assertions (such as subtyping) can be made under the assumption that a certain ConstraintFormula
 * is satisfied.  Combining a number of these formulas, it is possible to determine whether a
 * certain portion of a program is well-formed (and, if so, how the inference variables that appear
 * within the program should be instantiated).
 * Constraint formulas are always in Disjunctive normal form
 */
public abstract class ConstraintFormula {

	// Created as the result of calling 'solve()'
    
    
	private static class SolvedFormula extends ConstraintFormula {
		private final Map<_InferenceVarType, Type> inferredTypes;			
		final private SubtypeHistory history;
		
		public SolvedFormula(Map<_InferenceVarType, Type> inferred_types, SubtypeHistory history) {
			this.inferredTypes = inferred_types;
			this.history = history;
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

        // For this implementation, getMap() actually does hard work! It
        // implements section 20.3 of the specification.
        @Override
        public Map<_InferenceVarType, Type> getMap() {
            Map<_InferenceVarType, Type> result = new HashMap<_InferenceVarType, Type>();
            // for each inferred type
            for( Map.Entry<_InferenceVarType, Type> entry : inferredTypes.entrySet() ) {
                result.put(entry.getKey(), closestExpressibleType(entry.getValue()));
            }

            return result;
        }

        private Type closestExpressibleType(Type value) {
            // TODO: As of 8/12/08 discussion w/ EA, we won't actually do
            // closest expressible types. Just normalize, then simplify.
            return history.normalize(value);
        }

        @Override public boolean isFalse() { return false; }
		@Override public boolean isTrue() { return true; }

		@Override
		public ConstraintFormula or(ConstraintFormula c,
				SubtypeHistory history) {
			return bug("Once constraint has been solved, this should not be called");
		}

        @Override
        public ConstraintFormula removeTypesFromScope(List<VarType> types) {
            return bug("Once constraint has been solved, this should not be called");
        }
	}
	
    /**
     * A formula that was unsatisfiable, and yet was able to solve for some inference variables.
     * We hope this class will help us give more localized error messages.
     */
    private static class FailedSolvedFormula extends ConstraintFormula {
        private final Map<_InferenceVarType, Type> inferredTypes;           
        final private SubtypeHistory history;
        
        public FailedSolvedFormula(Map<_InferenceVarType, Type> inferred_types, SubtypeHistory history) {
            this.inferredTypes = inferred_types;
            this.history = history;
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

        // For this implementation, getMap() actually does hard work! It
        // implements section 20.3 of the specification.
        @Override
        public Map<_InferenceVarType, Type> getMap() {
            Map<_InferenceVarType, Type> result = new HashMap<_InferenceVarType, Type>();
            // for each inferred type
            for( Map.Entry<_InferenceVarType, Type> entry : inferredTypes.entrySet() ) {
                result.put(entry.getKey(), closestExpressibleType(entry.getValue()));
            }

            return result;
        }

        private Type closestExpressibleType(Type value) {
            // TODO: As of 8/12/08 discussion w/ EA, we won't actually do
            // closest expressible types. Just normalize, then simplify.
            return history.normalize(value);
        }

        @Override public boolean isFalse() { return true; }
        @Override public boolean isTrue() { return false; }

        @Override
        public ConstraintFormula or(ConstraintFormula c,
                SubtypeHistory history) {
            return bug("Once constraint has been solved, this should not be called");
        }

        @Override
        public ConstraintFormula removeTypesFromScope(List<VarType> types) {
            return bug("Once constraint has been solved, this should not be called");
        }
    }
    
    /**
     * A constraint that is open, and yet contains solved inference variables.
     * Trying to add additional constraints to these inference variables will result in
     * FALSE, but otherwise this formula functions as a {@code ConjunctiveFormula}.
     */
    public static class PartiallySolvedFormula extends ConstraintFormula {

        private final Map<_InferenceVarType,Type> results;
        private final ConstraintFormula unsolved_formula;
        private final SubtypeHistory history;
        
        public PartiallySolvedFormula(Map<_InferenceVarType,Type> results, ConstraintFormula unsolved_formula, SubtypeHistory history) {
            this.results = results;
            this.unsolved_formula = unsolved_formula;
            this.history = history;
        }
        
        @Override
        public ConstraintFormula and(ConstraintFormula c, SubtypeHistory history) {
            if(c.isTrue()) { return this; }
            if(c.isFalse()) { return c; }
            if(c instanceof ConjunctiveFormula) {  
                ConjunctiveFormula c_ = (ConjunctiveFormula)c;
                
                // TODO: This is really slow!!
                if( CollectUtil.containsAny(c_.getMap().keySet(), this.results.keySet()) )
                    return new FailedSolvedFormula(results, history);
                
                return new PartiallySolvedFormula(results, unsolved_formula.and(c_, history), history);
            }
            if(c instanceof DisjunctiveFormula) { return NI.nyi(); };
            if(c instanceof PartiallySolvedFormula) { 
                PartiallySolvedFormula c_ = (PartiallySolvedFormula)c;
                
                // For now, we should make sure our set of inference vars are disjoint.
                if( CollectUtil.containsAny(c_.results.keySet(), this.results.keySet()) )
                    return NI.nyi();
                
                return new PartiallySolvedFormula(CollectUtil.union(this.results, c_.results), 
                        this.unsolved_formula.and(c_, history), history);
            }
            return NI.nyi();
        }

        @Override
        public ConstraintFormula applySubstitution(Lambda<Type, Type> sigma) {
            return NI.nyi();
        }

        @Override
        public Map<_InferenceVarType, Type> getMap() {
            return CollectUtil.union(unsolved_formula.getMap(), results);
        }

        @Override public boolean isFalse() { return false; }
        @Override public boolean isSatisfiable() { return this.solve().isSatisfiable(); }
        @Override public boolean isTrue() { return false; }

        @Override
        public ConstraintFormula or(ConstraintFormula c, SubtypeHistory history) {
            if( c.isTrue() )
                return TRUE;
            else if( c.isFalse() )
                return this;
            else {
                // TODO: This is really slow!!
                if( CollectUtil.containsAny(c.getMap().keySet(), this.results.keySet()) )
                    return new FailedSolvedFormula(results, history);

                return new PartiallySolvedFormula(results, unsolved_formula.and(c, history), history);
            }
        }

        @Override
        public ConstraintFormula removeTypesFromScope(List<VarType> types) {
            return new PartiallySolvedFormula(results, unsolved_formula.removeTypesFromScope(types), history);
        }

        @Override
        protected ConstraintFormula solve() {
            ConstraintFormula solved_constraint = unsolved_formula.solve();
            if( solved_constraint.isTrue() )
                return new SolvedFormula(CollectUtil.union(solved_constraint.getMap(), results), history);
            else
                return new FailedSolvedFormula(CollectUtil.union(solved_constraint.getMap(), results), history);
        }
    }
    
    /**
     * A conjunction of a number of binding constraints on inference variables.
     * We maintain an invariant that if (i1,i2) is in uppers (12,i1) is in lowers.
     */
    public static class ConjunctiveFormula extends ConstraintFormula {
    	final private SubtypeHistory history;
    	
    	final private IMultiMap<_InferenceVarType,Type> ivarLowerBounds;
    	
    	final private IMultiMap<_InferenceVarType,Type> ivarUpperBounds;
    	
        private ConjunctiveFormula(IMultiMap<_InferenceVarType,Type> ivarUpperBounds,
                IMultiMap<_InferenceVarType,Type> ivarLowerBounds, SubtypeHistory h) {

            IMultiMap<_InferenceVarType,Type> newuppers = new MultiMap<_InferenceVarType,Type>(ivarUpperBounds);
            IMultiMap<_InferenceVarType,Type> newlowers = new MultiMap<_InferenceVarType,Type>(ivarLowerBounds);
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

            this.ivarLowerBounds = Useful.shrinkMultiMap(newlowers);
            this.ivarUpperBounds = Useful.shrinkMultiMap(newuppers);
            history = h;
        }
    	
    	@Override
		public ConstraintFormula and(ConstraintFormula c, SubtypeHistory history) {
            if(c.isTrue()) { return this; }
            if(c.isFalse()) { return c; }
            if(c instanceof ConjunctiveFormula) { return merge((ConjunctiveFormula) c, history); }
            if(c instanceof DisjunctiveFormula) {return c.and(this,history);};
            if(c instanceof PartiallySolvedFormula) { return c.and(this, history); }
            return InterpreterBug.bug("can't and a solved formula");
		}
    	    	
		@Override
		public ConjunctiveFormula applySubstitution(final Lambda<Type, Type> sigma) {
			IMultiMap<_InferenceVarType,Type> new_uppers = new MultiMap<_InferenceVarType,Type>();
			IMultiMap<_InferenceVarType,Type> new_lowers = new MultiMap<_InferenceVarType,Type>();
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
			return new ConjunctiveFormula(new_uppers, new_lowers, this.history);
		}

		/** Find a cycle in naked inference variables starting at the given one, the cycle must have length greater than
		 *  one, so T_1 <: T_1 does not qualify. */
		private Option<ConsList<_InferenceVarType>> findCycle(_InferenceVarType cur_start, final ConsList<_InferenceVarType> cur_path) {
			assert(cur_path.size() > 0);
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
						if( Useful.consListContains(that, cur_path) && cur_path.size() > 1 ) 
							return Option.some(cur_path);
						else 
							return findCycle(that, ConsList.cons(that, cur_path));
					}
					
				});
				// If we found a cycle, we are done.
				if(r.isSome()) {
					return r;
				}
			}
			return Option.none();
		}

		// Never returns null
    	private Set<Type> getLowerBounds(_InferenceVarType ivar) {
    		return this.ivarLowerBounds.containsKey(ivar) ? 
    				this.ivarLowerBounds.get(ivar) :
    				Collections.<Type>emptySet();
    	}
		
		@Override
		public Map<_InferenceVarType, Type> getMap() {
			return this.solve().getMap();
		}

		// Never returns null
    	private Set<Type> getUpperBounds(_InferenceVarType ivar) {
    		return this.ivarUpperBounds.containsKey(ivar) ? 
    				this.ivarUpperBounds.get(ivar) :
    				Collections.<Type>emptySet();
    	}

		@Override
		public boolean isFalse() {
			// We could actually do some simplification if we wanted.
			return false;
		}
		
		@Override
		public boolean isSatisfiable() {
			return this.solve().isSatisfiable();
		}
		
		@Override
		public boolean isTrue() {
			// We could actually do some simplification if we wanted.
			return false;
		}
		
		// Combine the upper and lower bounds for each inference variable
		private ConjunctiveFormula merge(ConjunctiveFormula c, SubtypeHistory hist) {
			IMultiMap<_InferenceVarType,Type> new_upper_bounds = new MultiMap<_InferenceVarType,Type>();
			IMultiMap<_InferenceVarType,Type> new_lower_bounds = new MultiMap<_InferenceVarType,Type>();
			
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
			return new ConjunctiveFormula(new_upper_bounds, new_lower_bounds, hist);
		}
		
		@Override
		public ConstraintFormula or(ConstraintFormula c, SubtypeHistory history) {
			if( c.equals(TRUE) ) {
				return TRUE;
			}
			else if( c.equals(FALSE) ) {
				return this;
			}
			else if( c instanceof ConjunctiveFormula){
				if(c.equals(this)){
					return this;
				}
				else{
					//return new DisjunctiveFormula(Useful.set(this,(ConjunctiveFormula)c));
					return this;
				}
			}
			else if(c instanceof DisjunctiveFormula){
				return c.or(this, history);
			}
			else {
				return InterpreterBug.bug("Unexpected type, " + c.getClass());
			}
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

            IMultiMap<_InferenceVarType,Type> new_uppers = new MultiMap<_InferenceVarType,Type>();
            IMultiMap<_InferenceVarType,Type> new_lowers = new MultiMap<_InferenceVarType,Type>();
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

            // "Remove redundant constraints"
            // For now we will just remove #1 <: #1
            new_uppers.removeItem(new_ivar, new_ivar);
            new_lowers.removeItem(new_ivar, new_ivar);

            // No remaining constraints means that the entire constraint was vaccuously true.
            return 
            (new_uppers.isEmpty() && new_lowers.isEmpty()) ?
                    TRUE :
                    new ReplacedConstraintFormula(new ConjunctiveFormula(new_uppers, new_lowers, this.history), new_ivar, to_remove);
        }
		
		// Returns a solved constraint formula, solved by doing the steps in 20.2 of spec1 beta
		@Override
		protected ConstraintFormula solve() {
			
			Set<_InferenceVarType> ivars = CollectUtil.union(ivarLowerBounds.keySet(), ivarUpperBounds.keySet());
			
			// 1.) for every cycle of constraints of naked prime static variables...
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

			
			//Preprocess: make sure every inference variable is bounded
			
			// temp will initially store all inference variables that have bounds.
			final Set<_InferenceVarType> temp = new HashSet<_InferenceVarType>();
			temp.addAll(ivars);
			final NodeDepthFirstVisitor_void v=new NodeDepthFirstVisitor_void(){
				@Override
				public void for_InferenceVarType(_InferenceVarType that) {
					temp.add(that);
				}
			};
			Runnable1<Type> accepter=new Runnable1<Type>(){
				public void run(Type arg0) {
					arg0.accept(v);
				}
			};
			
			// Add all additional inference variables found within the types' bounds. 
			for(_InferenceVarType ivar: ivars){
				if(ivarUpperBounds.containsKey(ivar)){
					Set<Type> potentials = ivarUpperBounds.get(ivar);
					IterUtil.run(potentials, accepter);
				}
				if(ivarLowerBounds.containsKey(ivar)){
					Set<Type> potentials = ivarLowerBounds.get(ivar);
					IterUtil.run(potentials, accepter);
				}
			}
			IMultiMap<_InferenceVarType,Type> bounded_uppers = new MultiMap<_InferenceVarType,Type>(ivarUpperBounds);
			IMultiMap<_InferenceVarType,Type> bounded_lowers = new MultiMap<_InferenceVarType,Type>(ivarLowerBounds);
			for(_InferenceVarType ivar: CollectUtil.complement(temp,ivarUpperBounds.keySet())){
				bounded_uppers.putItem(ivar,Types.ANY);
			}
			for(_InferenceVarType ivar: CollectUtil.complement(temp,ivarLowerBounds.keySet())){
				bounded_lowers.putItem(ivar,Types.BOTTOM);
			}
			
			ivars=temp;
			
			// 2
			Map<_InferenceVarType, Type> new_uppers = new HashMap<_InferenceVarType, Type>();
			for(_InferenceVarType ivar: ivars){
				new_uppers.put(ivar,Types.makeIntersection(bounded_uppers .get(ivar)));
			}
			//3
			TypeExpander expander = new TypeExpander(new_uppers);
			Map<_InferenceVarType,Type> lubs = new HashMap<_InferenceVarType,Type>();
			for(_InferenceVarType ivar: ivars){
				lubs.put(ivar,(Type) ivar.accept(expander));
			}
			
			
			// 4
			Map<_InferenceVarType, Type> new_lowers = new HashMap<_InferenceVarType, Type>();
			for(_InferenceVarType ivar: ivars){
				new_lowers.put(ivar,Types.makeUnion(bounded_lowers .get(ivar)));
			}
			//3'
			expander = new TypeExpander(new_lowers);
			Map<_InferenceVarType,Type> glbs = new HashMap<_InferenceVarType,Type>();
			for(_InferenceVarType ivar: ivars){
				glbs.put(ivar, (Type)ivar.accept(expander));
			}
			
			//5.) The inferred type is the intersection of its expanded lower and upper bounds
			Map<_InferenceVarType,Type> inferred_types = new HashMap<_InferenceVarType,Type>();
			boolean solvable = true;
			for( _InferenceVarType ivar : ivars ) {
				if( !lubs.containsKey(ivar) || !glbs.containsKey(ivar) ) {
					InterpreterBug.bug("An inference variable is unbounded");
				}
				Type lub = lubs.get(ivar);
				Type glb = glbs.get(ivar);
				if( this.history.subtypeNormal(glb, lub).isFalse() ){
					solvable = false;
					//return FALSE;
				}
				else{
					inferred_types.put(ivar, NodeFactory.makeIntersectionType(lub, glb));
				}
			}
			if(solvable){
			    return new SolvedFormula(inferred_types, history);
			}
			else{
			    return new FailedSolvedFormula(inferred_types, history);
			}
		}

		@Override
		public String toString() {
			StringBuffer result = new StringBuffer();
			Set<_InferenceVarType> all_ivars = CollectUtil.union(ivarUpperBounds.keySet(), 
                                                                 ivarLowerBounds.keySet());
			result.append("{");
			int i=0;
			for( _InferenceVarType ivar : all_ivars ) {
				String lbound = ivarLowerBounds.containsKey(ivar) ? ivarLowerBounds.get(ivar).toString() : Collections.emptySet().toString();
				String ubound = ivarUpperBounds.containsKey(ivar) ? ivarUpperBounds.get(ivar ).toString() : Collections.emptySet().toString();
				
				SourceLoc ivar_loc = ivar.getSpan().getBegin();
				
				result.append(lbound + " <: " + ivar +":("+ ivar_loc.getLine()+":"+ivar_loc.column()+")" + " <: " + ubound);
				if(i<all_ivars.size()-1){
					result.append(", ");
				}
				i++;
			}
			result.append("}");
 			return result.toString();

		}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime
				* result
				+ ((ivarLowerBounds == null) ? 0 : ivarLowerBounds
						.hashCode());
		result = prime
				* result
				+ ((ivarUpperBounds == null) ? 0 : ivarUpperBounds
						.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final ConjunctiveFormula other = (ConjunctiveFormula) obj;
		if (ivarLowerBounds == null) {
			if (other.ivarLowerBounds != null)
				return false;
		} else if (!ivarLowerBounds.equals(other.ivarLowerBounds))
			return false;
		if (ivarUpperBounds == null) {
			if (other.ivarUpperBounds != null)
				return false;
		} else if (!ivarUpperBounds.equals(other.ivarUpperBounds))
			return false;
		return true;
	}

    @Override
    public ConstraintFormula removeTypesFromScope(final List<VarType> types) {
        ConstraintFormula solved = this.solve();
        IMultiMap<_InferenceVarType,Type> new_uppers = new MultiMap<_InferenceVarType,Type>();
        IMultiMap<_InferenceVarType,Type> new_lowers = new MultiMap<_InferenceVarType,Type>();
        Map<_InferenceVarType,Type> solved_ivars = new HashMap<_InferenceVarType,Type>();
        
        for( Map.Entry<_InferenceVarType, Type> entry : solved.getMap().entrySet() ) {
            Boolean contains_removed_type = entry.getValue().accept(
                new TypeDepthFirstVisitor<Boolean>() {
                    @Override public Boolean defaultCase(Type that) { return Boolean.FALSE; }
                    @Override public Boolean forVarType(VarType that) { return types.contains(that); }
                });
            if( contains_removed_type ) {
                solved_ivars.put(entry.getKey(), entry.getValue());
            }
            else {
                if( this.ivarLowerBounds.containsKey(entry.getKey()) )
                    new_lowers.putItems(entry.getKey(), ivarLowerBounds.get(entry.getKey()));
                if( this.ivarUpperBounds.containsKey(entry.getKey()))
                    new_uppers.putItems(entry.getKey(), ivarUpperBounds.get(entry.getKey()));
                
            }
        }
        
        return new PartiallySolvedFormula(solved_ivars,
                new ConjunctiveFormula(new_uppers, new_lowers, history),
                history);
    }
 }
    
    public static class DisjunctiveFormula extends ConstraintFormula {
    	
    	private Set<ConjunctiveFormula> conjuncts;
    	
		DisjunctiveFormula(Set<ConjunctiveFormula> _conjuncts){
			if(_conjuncts.isEmpty())
				InterpreterBug.bug("Empty conjunct");
			conjuncts=Collections.unmodifiableSet(_conjuncts);
		}
    	
		/*
		 * Returns the first constraint formula that is solvable.
		 */
		@Override
		protected ConstraintFormula solve() {
			for(ConjunctiveFormula cf: conjuncts){ 
				ConstraintFormula sf= cf.solve();
				if(sf.isSatisfiable())
					return sf;
			}
			return FALSE;
		}

		@Override
		public ConstraintFormula and(ConstraintFormula c, final SubtypeHistory history) {
			if(c.isFalse()){
				return c;
			}
			if(c.isTrue()){
				return this;
			}
			if(c instanceof ConjunctiveFormula){
				final ConjunctiveFormula cf = (ConjunctiveFormula)c;
				
				Set<ConjunctiveFormula> temp = new HashSet<ConjunctiveFormula>();
				for( ConjunctiveFormula cur_cf : conjuncts ) {
					ConjunctiveFormula new_cf = cur_cf.merge(cf, history);
					
					// Memory optimization
					if( new_cf.isSatisfiable() ) {
						temp.add(new_cf);
					}
				}			
				if(temp.isEmpty())
					return FALSE;
				return new DisjunctiveFormula(temp);
			}
			if(c instanceof DisjunctiveFormula){
				final DisjunctiveFormula df = (DisjunctiveFormula) c;
				
				Set<ConjunctiveFormula> temp = new HashSet<ConjunctiveFormula>();
				for( ConjunctiveFormula cur_cf : conjuncts ) {
					ConstraintFormula new_cf = df.and(cur_cf, history);
					
					if( new_cf instanceof DisjunctiveFormula ) {
						temp.addAll(((DisjunctiveFormula)new_cf).conjuncts);
					}
				}
				if(temp.isEmpty())
					return FALSE;
				return new DisjunctiveFormula(temp);
			}
			return InterpreterBug.bug("Can't and with a Solved Constraint");
		}


		@Override
		public ConstraintFormula applySubstitution(final Lambda<Type, Type> sigma) {
			return new DisjunctiveFormula(CollectUtil.makeSet(IterUtil.map(conjuncts, new Lambda<ConjunctiveFormula,ConjunctiveFormula>(){
				public ConjunctiveFormula value(ConjunctiveFormula arg0) {
					return arg0.applySubstitution(sigma);
				}
				
			})));
		}

		@Override
		public boolean isFalse() {
			return false;
		}

		@Override
		public boolean isTrue() {
			return false;
		}

		@Override
		public ConstraintFormula or(ConstraintFormula c, SubtypeHistory history) {
			if(c.isFalse()){
				return this;
			}
			if(c.isTrue()){
				return c;
			}
			if(c instanceof ConjunctiveFormula){
				final ConjunctiveFormula cf = (ConjunctiveFormula)c;
				Set<ConjunctiveFormula> temp = new HashSet<ConjunctiveFormula>(conjuncts);
				temp.add(cf);
				return new DisjunctiveFormula(temp);
			}
			if(c instanceof DisjunctiveFormula){
				final DisjunctiveFormula df = (DisjunctiveFormula) c;
				Set<ConjunctiveFormula> temp = new HashSet<ConjunctiveFormula>(conjuncts);
				temp.addAll(df.conjuncts);
				return new DisjunctiveFormula(temp);
			}
			return InterpreterBug.bug("Can't and with a Solved Constraint");

		}

		@Override
		public boolean isSatisfiable() {
			return this.solve().isSatisfiable();
		}
		
		@Override
		public Map<_InferenceVarType, Type> getMap() {
			return this.solve().getMap();
		}
		
		@Override
		public String toString() {
			StringBuffer result = new StringBuffer();
			int i=0;
			for(ConjunctiveFormula form : this.conjuncts){
				result.append(form.toString());
				if(i<this.conjuncts.size()-1){
					result.append(" OR ");
				}
				i++;
			}
			return result.toString();
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((conjuncts == null) ? 0 : conjuncts.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			final DisjunctiveFormula other = (DisjunctiveFormula) obj;
			if (conjuncts == null) {
				if (other.conjuncts != null)
					return false;
			} else if (!conjuncts.equals(other.conjuncts))
				return false;
			return true;
		}

        @Override
        public ConstraintFormula removeTypesFromScope(List<VarType> types) {
            return NI.nyi();
        }

    	
    }
    
    public static final ConstraintFormula FALSE = new ConstraintFormula() {
        public ConstraintFormula and(ConstraintFormula f, SubtypeHistory history) { return this; }
        public ConstraintFormula applySubstitution(Lambda<Type, Type> sigma) { return this; }
        public boolean isFalse() { return true; }
        public boolean isTrue() { return false; }
        public ConstraintFormula or(ConstraintFormula f, SubtypeHistory history) { return f; }
        public String toString() { return "(false)"; }
        @Override public ConstraintFormula removeTypesFromScope(List<VarType> types) { return this; }
    };


    public static final ConstraintFormula TRUE = new ConstraintFormula() {
        public ConstraintFormula and(ConstraintFormula f, SubtypeHistory history) { return f; }
        public ConstraintFormula applySubstitution(Lambda<Type, Type> sigma) { return this; }
        public boolean isFalse() { return false; }
        public boolean isTrue() { return true; }
        public ConstraintFormula or(ConstraintFormula f, SubtypeHistory history) { return this; }
        public String toString() { return "(true)"; }
        @Override public ConstraintFormula removeTypesFromScope(List<VarType> types) { return this; }
    };

    /**
     * When inference variables have been removed because of cycles, we still need to preserve the
     * inference variables that were removed, so that later when getMap is called, we will still know
     * what those removed inference variables were discovered to be.
     */
    private static final class ReplacedConstraintFormula extends ConstraintFormula {
        private final ConstraintFormula delegate;
        private final _InferenceVarType newIVar;
        private  final List<_InferenceVarType> removedIVars;
        
        public ReplacedConstraintFormula(ConstraintFormula delegate, _InferenceVarType newIVar, List<_InferenceVarType> removedIVars) {
            this.delegate = delegate;
            this.newIVar = newIVar;
            this.removedIVars = removedIVars;
        }
        
        @Override
        public Map<_InferenceVarType, Type> getMap() {
            Map<_InferenceVarType, Type> old_map = delegate.getMap();
            if( !old_map.containsKey(newIVar) )
                return old_map;
            
            Map<_InferenceVarType, Type> new_map = new HashMap<_InferenceVarType, Type>();
            for( _InferenceVarType removedIVar : removedIVars ) {
                new_map.put(removedIVar, old_map.get(newIVar));
            }
            return CollectUtil.union(old_map, new_map);
        }
        
        @Override
        protected ConstraintFormula solve() {
            return new ReplacedConstraintFormula(delegate.solve(), newIVar, removedIVars);
        }
        
        @Override public ConstraintFormula and(ConstraintFormula c, SubtypeHistory history) { return delegate.and(c, history); }
        @Override public ConstraintFormula applySubstitution(Lambda<Type, Type> sigma) { return delegate.applySubstitution(sigma); }
        @Override public boolean isFalse() { return delegate.isFalse(); }
        @Override public boolean isTrue() { return delegate.isTrue(); }
        @Override public ConstraintFormula or(ConstraintFormula c, SubtypeHistory history) { return delegate.or(c, history); }

        @Override
        public ConstraintFormula removeTypesFromScope(List<VarType> types) {
            return new ReplacedConstraintFormula(delegate.removeTypesFromScope(types), newIVar, removedIVars);
        }
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

    public static ConstraintFormula fromBoolean(boolean b) {
        return b ? TRUE : FALSE;
    }

    public static ConstraintFormula lowerBound(_InferenceVarType var, Type bound, SubtypeHistory history) {
        debug.logStart(new String[]{"var","lowerBound"}, var, bound);
        if (history.subtypeNormal(bound, BOTTOM).isTrue()) {
            debug.logEnd("result", TRUE);
            return TRUE;
        }
        else {
//            IMultiMap<_InferenceVarType,Type> lowers = new MultiMap<_InferenceVarType,Type>();
//            lowers.putItem(var, bound);
            ConstraintFormula result =
                new ConjunctiveFormula(Useful.<_InferenceVarType,Type>emptyMultiMap(), 
                        Useful.singletonMultiMap(var, bound), history);
            debug.logEnd("result", result);
            return result;
        }
    }

    public static ConstraintFormula upperBound(_InferenceVarType var, Type bound, SubtypeHistory history) {
        debug.logStart(new String[]{"var","upperBound"}, var, bound);
        if (history.subtypeNormal(ANY, bound).isTrue()) {
            debug.logEnd("result", TRUE);
            return TRUE;
        }
        else{
            ConstraintFormula result =
                new ConjunctiveFormula(Useful.singletonMultiMap(var, bound), 
                        Useful.<_InferenceVarType,Type>emptyMultiMap(), history);
            debug.logEnd("result", result);
            return result;
        }
    }
    
    /** Merge this and another formula by asserting that they must both be true. */
    public abstract ConstraintFormula and(ConstraintFormula c, SubtypeHistory history);

    /**
     * Apply a type substitution to the contents of a formula.  Callers assume responsibility
     * for guaranteeing that the substitution will not change the satisfiability (or truth) of the
     * formula.  Substitutions of bounded inference variables must map to other (or the same)
     * inference variables, not arbitrary types.
     */
    public abstract ConstraintFormula applySubstitution(Lambda<Type, Type> sigma);

    /** Get the map of inference variable types to upper bounds **/
    public Map<_InferenceVarType, Type> getMap(){
    	return Collections.emptyMap();
    }



    /** Determine whether the formula is false for all inference variable instantiations. */
    public abstract boolean isFalse();

    /** Determine whether there exists some choice for inference variables that makes the formula true. */
    public boolean isSatisfiable() { return !isFalse(); }

    /** Determine whether the formula is true for all inference variable instantiations. */
    public abstract boolean isTrue();

    /** Indicates that the given types have just gone out of scope, so in the resulting
     *  ConstraintFormula they must not be mentioned. */
    public abstract ConstraintFormula removeTypesFromScope(List<VarType> types);
    
    /** Merge this and another formula by asserting that one of the two must be true. */
    public abstract ConstraintFormula or(ConstraintFormula c, SubtypeHistory history);

    protected ConstraintFormula solve() {
		return this;
	}
    
    static protected class TypeExpander extends NodeUpdateVisitor{
    	Map<_InferenceVarType,Type> bounds;
    	Set<_InferenceVarType> context;
    	TypeExpander(Map<_InferenceVarType,Type> _bounds){
    		bounds=_bounds;
    		context= new HashSet<_InferenceVarType>();
    	}
    	TypeExpander extend(_InferenceVarType t){
    		TypeExpander temp = new TypeExpander(bounds);
    		temp.context.addAll(context);
    		temp.context.add(t);
    		return temp;
    	}
    	
    	@Override
    	public Type for_InferenceVarType(_InferenceVarType that) {
    		if(context.contains(that)){
    			return that;
    		}
    		else{
    			Type bound=bounds.get(that);
    			TypeExpander v = this.extend(that);
    			//return NodeFactory.makeFixedPointType(that,(Type)bound.accept(v));
    			return (Type)bound.accept(v);
    		}
    	}
    	
    }

}
