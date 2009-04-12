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
import com.sun.fortress.useful.TopSort;
import com.sun.fortress.useful.TopSortItemImpl;
import com.sun.fortress.useful.Useful;

import edu.rice.cs.plt.tuple.Option;

class OverloadSet {
    
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
    final TypeAnalyzer ta;
    final BASet<Integer> testedIndices;
    
    final OverloadSet parent;
    final Type selectedParameterType;

    int dispatchParameterIndex;
    OverloadSet[] children;
    boolean splitDone;
    
    OverloadSet(IdOrOpOrAnonymousName name, TypeAnalyzer ta,
                Set<Function> lessSpecificThanSoFar,
                BASet<Integer> testedIndices, OverloadSet parent, Type selectedParameterType) {
        this.name = name;
        this.ta = ta;
        this.lessSpecificThanSoFar = lessSpecificThanSoFar;
        this.testedIndices = testedIndices;
        this.parent = parent;
        this.selectedParameterType = selectedParameterType;
    }
    
    OverloadSet(IdOrOpOrAnonymousName name, TypeAnalyzer ta, Set<Function> defs) {
        this(name, ta, defs,
            new BASet<Integer>(DefaultComparator.<Integer>normal()),
            null, null);
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
        
        int the_size = -1;

        for (Function f : lessSpecificThanSoFar) {
            if (CodeGenerationPhase.debugOverloading)
                System.err.println("Overload: " + f);
            List<Param> parameters = f.parameters();
            int this_size = parameters.size();
            if (the_size == -1)
                the_size = this_size;
            else if (the_size != this_size) {
                InterpreterBug.bug("Need to handle variable arg dispatch elsewhere " + name);
                return;
            }
        }
        
        {
            // Accumulate sets of parameter types.
            int nargs = the_size;
            Set<Type>[] typeSets = new Set[nargs];
            for (int i = 0; i < nargs; i++) {
                typeSets[i] = new HashSet<Type>();
            }
            
            for (Function f : lessSpecificThanSoFar) {
                List<Param> parameters = f.parameters();
                int i = 0;
                for (Param p : parameters) {
                    Option<Type> ot = p.getIdType();
                    Option<Type> ovt = p.getVarargsType();
                    if (ovt.isSome()) {
                        InterpreterBug.bug("Not ready to handle compilation of overloaded varargs yet, function is " + f);
                    }
                    if (ot.isNone()) {
                        InterpreterBug.bug("Missing type for parameter " + i + " of " + f);
                    }
                    Type t = ot.unwrap();
                    typeSets[i++].add(t);
                }       
            }
            
            // Choose parameter index with greatest variation.
            
            int maxi = -1; int max = 0;
            for (int i = 0; i < nargs; i++) {
                if (testedIndices.contains(i))
                    continue;
                if (typeSets[i].size() > max) {
                    max = typeSets[i].size();
                    maxi = i;
                }
            }
            
            // dispatch on maxi'th parameter.
            dispatchParameterIndex = maxi;
            Set<Type> dispatchTypes = typeSets[dispatchParameterIndex];
           
           
            children = new OverloadSet[max];
            BASet<Integer> childTestedIndices = testedIndices.putNew(maxi);
            
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
                
               if (the_size == childTestedIndices.size()) {
                        // Choose most specific member of lessSpecificThanSoFar
                   childLSTSF = mostSpecificMemberOf(childLSTSF);
                        
                }
                    
                children[i] =
                    new OverloadSet(name, ta, childLSTSF,
                            childTestedIndices, this, t);
            }
            for (OverloadSet child: children) {
                child.split();
            }
        }
        splitDone = true;
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
    
}