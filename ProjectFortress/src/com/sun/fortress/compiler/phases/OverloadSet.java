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

package com.sun.fortress.compiler.phases;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.sun.fortress.compiler.index.Function;
import com.sun.fortress.compiler.typechecker.TypeAnalyzer;
import com.sun.fortress.exceptions.InterpreterBug;
import com.sun.fortress.nodes.IdOrOpOrAnonymousName;
import com.sun.fortress.nodes.Param;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.useful.BASet;
import com.sun.fortress.useful.DefaultComparator;
import com.sun.fortress.useful.GMultiMap;
import com.sun.fortress.useful.Hasher;
import com.sun.fortress.useful.MagicNumbers;
import com.sun.fortress.useful.MultiMap;
import com.sun.fortress.useful.TopSort;
import com.sun.fortress.useful.TopSortItemImpl;
import com.sun.fortress.useful.Useful;

import edu.rice.cs.plt.tuple.Option;

public class OverloadSet implements Comparable<OverloadSet> {
    
    static class POType extends TopSortItemImpl<Type> {
        public POType(Type x) {
            super(x);
        }
    }
    
    /**
     * The set of functions that are less-specific-than-or-equal to the
     * parameters seen so far, so the parameter seen so far would be legal
     * for any of them.  The goal is to thin this set as more parameters
     * are found, and ultimately to choose the most specific one that
     * remains.
     */
    final Set<Function> lessSpecificThanSoFar;
    final IdOrOpOrAnonymousName name;
    /**
     * Used to answer subtype questions.
     */
    final TypeAnalyzer ta;
    /**
     * All the indices that have been tested already.
     * Dispatch begins at the "most profitable" index, which
     * is defined to be the one with the greatest variation.
     * (Alternative plan might be to order them by the one
     * with the smallest subsets, thus guaranteeing log depth).
     */
    final BASet<Integer> testedIndices;
    
    /**
     * Assuming we have a parent (are not the root of a tree of OverloadSets),
     * what is that parent.
     */
    final OverloadSet parent;
    /**
     * Assuming we have a parent that dispatched on a parameter, how did we
     * get here?
     */
    final Type selectedParameterType;
    
    final int paramCount;

    /**
     * Which parameter is used to split this set into subsets?
     */
    int dispatchParameterIndex;
    OverloadSet[] children;
    boolean splitDone;
    
    OverloadSet(IdOrOpOrAnonymousName name, TypeAnalyzer ta,
                Set<Function> lessSpecificThanSoFar,
                BASet<Integer> testedIndices, OverloadSet parent, Type selectedParameterType, int paramCount) {
        this.name = name;
        this.ta = ta;
        this.lessSpecificThanSoFar = lessSpecificThanSoFar;
        this.testedIndices = testedIndices;
        this.parent = parent;
        this.selectedParameterType = selectedParameterType;
        this.paramCount = paramCount;
    }
    
    OverloadSet(IdOrOpOrAnonymousName name, TypeAnalyzer ta, Set<Function> defs, int n) {
        this(name, ta, defs,
            new BASet<Integer>(DefaultComparator.<Integer>normal()),
            null, null, n);
        
        // Ensure that they are all the same size.
        for (Function f : lessSpecificThanSoFar) {
            if (CodeGenerationPhase.debugOverloading)
                System.err.println("Overload: " + f);
            List<Param> parameters = f.parameters();
            int this_size = parameters.size();
            if (this_size != paramCount)
                InterpreterBug.bug("Need to handle variable arg dispatch elsewhere " + name);
            
        }
    }
    
    public String toString() {
        if (lessSpecificThanSoFar.size() == 1) {
            return lessSpecificThanSoFar.iterator().next().toString();
        }
        if (splitDone) {
            return toStringR("");
        } else {
            return lessSpecificThanSoFar.toString();
        }
    }
    
    private String toStringR(String indent) {
        if (lessSpecificThanSoFar.size() == 1) {
            return indent + lessSpecificThanSoFar.iterator().next().toString();
        } else {
            String s =  indent + "#" + dispatchParameterIndex + "\n";
            for (int i = 0; i < children.length; i++) {
                OverloadSet os = children[i];
                s += indent + os.selectedParameterType + "->" + os.toStringR(indent + "   ") + "\n";
            }
            return s;
        }
    }
    
    void split() {
        if (splitDone)
            return;
        
        if (lessSpecificThanSoFar.size() == 1) {
                return;
            // If there are no other alternatives, then we are done.
        }
        
        {
            // Accumulate sets of parameter types.
            int nargs = paramCount;;
            
            MultiMap<Type, Function>[] typeSets = new MultiMap[nargs];
            for (int i = 0; i < nargs; i++) {
                typeSets[i] = new MultiMap<Type, Function>();
            }
            
            for (Function f : lessSpecificThanSoFar) {
                List<Param> parameters = f.parameters();
                int i = 0;
                for (Param p : parameters) {
                    if (testedIndices.contains(i)) {
                        i++;
                        continue;
                    }
                    Option<Type> ot = p.getIdType();
                    Option<Type> ovt = p.getVarargsType();
                    if (ovt.isSome()) {
                        InterpreterBug.bug("Not ready to handle compilation of overloaded varargs yet, function is " + f);
                    }
                    if (ot.isNone()) {
                        InterpreterBug.bug("Missing type for parameter " + i + " of " + f);
                    }
                    Type t = ot.unwrap();
                    typeSets[i++].putItem(t, f);
                }       
            }
            
            // Choose parameter index with greatest variation.
            // Choose parameter index with the smallest largest subset.
            int besti = -1; int best = 0;
            boolean greatest_variation = false;
            for (int i = 0; i < nargs; i++) {
                if (testedIndices.contains(i))
                    continue;
                if (greatest_variation) {
                    if (typeSets[i].size() > best) {
                        best = typeSets[i].size();
                        besti = i;
                    }
                } else {
                    MultiMap<Type, Function> mm = typeSets[i];
                    int largest = 0;
                    for (Set<Function> sf : mm.values()) {
                        if (sf.size() > largest)
                            largest = sf.size();
                    }
                    if (besti == -1 || largest < best) {
                        besti = i;
                        best = largest;
                    }
                }
            }
            
            // dispatch on maxi'th parameter.
            dispatchParameterIndex = besti;
            Set<Type> dispatchTypes = typeSets[dispatchParameterIndex].keySet();
           
           
            children = new OverloadSet[best];
            BASet<Integer> childTestedIndices = testedIndices.putNew(besti);
            
            int i = 0;
            TopSortItemImpl<Type>[] potypes =
                new OverloadSet.POType[dispatchTypes.size()];
            /* Convert set of dispatch types into something that can be
               (topologically) sorted. */
            for (Type t : dispatchTypes) {
                potypes[i] = new POType(t);
                i++;
            }
            
            /*
             * Figure out ordering relationship for top sort.  O(N^2) work,
             * hope N is not too large.
             */
            for (i = 0; i < potypes.length; i++) {
                for (int j = i+1; j < potypes.length; j++) {
                    Type ti = potypes[i].x;
                    Type tj = potypes[j].x;
                    if (ta.subtype(ti, tj).isTrue()) {
                        potypes[i].edgeTo(potypes[j]);
                    } else if (ta.subtype(tj, ti).isTrue()) {
                        potypes[j].edgeTo(potypes[i]);
                    }
                }
            }
            
            List<TopSortItemImpl<Type>> specificFirst = TopSort.depthFirst(potypes);
            children = new OverloadSet[specificFirst.size()];
            Set<Function> alreadySelected = new HashSet<Function>();    
            
            // fill in children.
            for (i = 0; i < specificFirst.size(); i++) {
                Type t = specificFirst.get(i).x;
                Set<Function> childLSTSF = new HashSet<Function>();
                
                    for (Function f : lessSpecificThanSoFar) {
//                        if (alreadySelected.contains(f))
//                            continue;
                        List<Param> parameters = f.parameters();
                        Param p = parameters.get(dispatchParameterIndex);
                        Type pt = p.getIdType().unwrap();
                        if (ta.subtype(t, pt).isTrue()) {
                            childLSTSF.add(f);
                            alreadySelected.add(f);
                          
                        }
                    }
                
               childLSTSF = thin(childLSTSF, childTestedIndices);
                    
               // ought to not be necessary
               if (paramCount == childTestedIndices.size()) {
                        // Choose most specific member of lessSpecificThanSoFar
                   childLSTSF = mostSpecificMemberOf(childLSTSF);
                        
                }
                    
                children[i] =
                    new OverloadSet(name, ta, childLSTSF,
                            childTestedIndices, this, t, paramCount);
            }
            for (OverloadSet child: children) {
                child.split();
            }
        }
        splitDone = true;
    }

    private Set<Function> thin(Set<Function> childLSTSF, final Set<Integer> childTestedIndices) {
        /*
         * Hashes together functions that are equal in their unexamined parameter lists.
         */
        Hasher<Function> hasher = new Hasher<Function>() {

            @Override
            public boolean equiv(Function x, Function y) {
                List<Param> px = x.parameters();
                List<Param> py = y.parameters();
                for (int i = 0; i < px.size(); i++) {
                    if (childTestedIndices.contains(i))
                        continue;
                    Type tx = px.get(i).getIdType().unwrap();
                    Type ty = px.get(i).getIdType().unwrap();
                    if (! tx.equals(ty))
                        return false;
                }
                return true;
            }

            @Override
            public long hash(Function x) {
                int h = MagicNumbers.T;
                
                List<Param> px = x.parameters();
                for (int i = 0; i < px.size(); i++) {
                    if (childTestedIndices.contains(i))
                        continue;
                    Type tx = px.get(i).getIdType().unwrap();
                    h = h * MagicNumbers.t + tx.hashCode();
                }
                return h;
            }
            
        };
        
        /*
         * Creates map from (some) functions to the
         * equivalence sets to which they are members.
         */
        GMultiMap<Function, Function> eqSetMap = new GMultiMap<Function, Function>(hasher);
        for (Function f : childLSTSF)
            eqSetMap.putItem(f, f);
        
        Set<Function> tmp = new HashSet<Function>();
        
        /*
         * Take the most specific member of each equivalence set, and union
         * those together.
         */
        for (Set<Function> sf : eqSetMap.values())
            tmp.addAll(mostSpecificMemberOf(sf));
        
        return tmp;
    }

    /**
     * 
     */
    private Set<Function> mostSpecificMemberOf(Set<Function> set) {
        Function msf = null;
        for (Function candidate : set) {
            if (msf == null)
                msf = candidate;
            else {
                List<Param> msf_parameters = msf.parameters();
                List<Param> cand_parameters = candidate.parameters();
                if (msf_parameters.size() != cand_parameters.size()) {
                    InterpreterBug.bug("Diff length parameter lists, should not be possible");
                }
                boolean cand_better = true;
                for (int i = 0; i < msf_parameters.size(); i++) {
                    // Not handling varargs yet!
                    Type msf_t = msf_parameters.get(i).getIdType().unwrap();
                    Type cand_t = cand_parameters.get(i).getIdType().unwrap();
                    // if any type of the candidate is not a subtype(or eq) 
                    // of the corresponding type of the msf, then the candidate
                    // is NOT better.
                    if (! ta.subtype(cand_t, msf_t).isTrue()) {
                        cand_better = false;
                        break;
                    }
                }
                if (cand_better)
                    msf = candidate;
                
            }
        }
        if (msf == null)
            return Collections.<Function>emptySet();
        else
            return Collections.singleton(msf);
    }

    @Override
    public int compareTo(OverloadSet o) {
        // TODO Auto-generated method stub
        return name.stringName().compareTo(o.name.stringName());
    }
    
    public IdOrOpOrAnonymousName getName() {
        return name;
    }
    
}