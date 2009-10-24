/*******************************************************************************
    Copyright 2009 Sun Microsystems, Inc.,
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

package com.sun.fortress.compiler.typechecker.constraints;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.sun.fortress.compiler.typechecker.constraints.ConstraintUtil.*;

import com.sun.fortress.compiler.Types;
import com.sun.fortress.compiler.typechecker.SubtypeHistory;
import com.sun.fortress.compiler.typechecker.constraints.ConstraintUtil.TypeExpander;
import com.sun.fortress.exceptions.InterpreterBug;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.NodeDepthFirstVisitor;
import com.sun.fortress.nodes.NodeDepthFirstVisitor_void;
import com.sun.fortress.nodes.NodeUpdateVisitor;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes.TypeDepthFirstVisitor;
import com.sun.fortress.nodes.VarType;
import com.sun.fortress.nodes._InferenceVarType;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.nodes_util.SourceLoc;
import com.sun.fortress.useful.IMultiMap;
import com.sun.fortress.useful.MultiMap;
import com.sun.fortress.useful.UsefulPLT;

import edu.rice.cs.plt.collect.CollectUtil;
import edu.rice.cs.plt.collect.ConsList;
import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.lambda.Lambda;
import edu.rice.cs.plt.lambda.Runnable1;
import edu.rice.cs.plt.tuple.Option;

/**
 * A conjunction of a number of binding constraints on inference variables.
 * We maintain an invariant that if (i1,i2) is in uppers (12,i1) is in lowers.
 */
public class ConjunctiveFormula extends ConstraintFormula {
    final private SubtypeHistory _history;

    final private IMultiMap<_InferenceVarType,Type> ivarLowerBounds;

    final private IMultiMap<_InferenceVarType,Type> ivarUpperBounds;

    public ConjunctiveFormula(IMultiMap<_InferenceVarType,Type> ivarUpperBounds,
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

        this.ivarLowerBounds = UsefulPLT.shrinkMultiMap(newlowers);
        this.ivarUpperBounds = UsefulPLT.shrinkMultiMap(newuppers);
        _history = h;
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
        return new ConjunctiveFormula(new_uppers, new_lowers, this._history);
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
                        if( UsefulPLT.consListContains(that, cur_path) && cur_path.size() > 1 )
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
    public ConjunctiveFormula merge(ConjunctiveFormula c, SubtypeHistory hist) {
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
        if( c.equals(trueFormula()) ) {
            return trueFormula();
        }
        else if( c.equals(falseFormula()) ) {
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
            trueFormula() :
            new ReplacedConstraintFormula(new ConjunctiveFormula(new_uppers, new_lowers, this._history), new_ivar, to_remove);
    }

        
    // Returns a solved constraint formula, solved by doing the steps in 20.2 of spec1 beta
    @Override
        public ConstraintFormula solve() {

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
            if( this._history.subtypeNormal(glb, lub).isFalse() ){
                solvable = false;
                //return FALSE;
            }
            else{
                inferred_types.put(ivar, NodeFactory.makeIntersectionType(lub, glb));
            }
        }
        if(solvable){
            return new SolvedFormula(inferred_types, _history);
        }
        else{
            return new FailedSolvedFormula(inferred_types, _history);
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

            SourceLoc ivar_loc = NodeUtil.getSpan(ivar).getBegin();

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
                                          new ConjunctiveFormula(new_uppers, new_lowers, _history),
                                          _history);
    }
}
