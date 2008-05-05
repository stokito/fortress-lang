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

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.Collections;
import edu.rice.cs.plt.collect.CollectUtil;
import edu.rice.cs.plt.lambda.Lambda;

import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes.InferenceVarType;

import com.sun.fortress.compiler.typechecker.TypeAnalyzer.SubtypeHistory;

import static com.sun.fortress.compiler.Types.ANY;
import static com.sun.fortress.compiler.Types.BOTTOM;
import static edu.rice.cs.plt.debug.DebugUtil.debug;
import static edu.rice.cs.plt.debug.DebugUtil.error;

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

    /** A conjunction of a number of binding constraints on inference variables.
     *  Clients are responsible for insuring that all constructed formulas are
     *  neither unsatisfiable (due to conflicting bounds on a variable) nor
     *  true (due to trivial bounds).
     */
    public static class SimpleFormula extends ConstraintFormula {
        private Map<InferenceVarType, Type> _upperBounds;
        private Map<InferenceVarType, Type> _lowerBounds;
        
        private SimpleFormula(Map<InferenceVarType, Type> upperBounds, Map<InferenceVarType, Type> lowerBounds) {
            _upperBounds = upperBounds;
            _lowerBounds = lowerBounds;
        }

        public ConstraintFormula and(ConstraintFormula f, SubtypeHistory history) {
            if (f.isTrue()) { return this; }
            else if (f.isFalse()) { return f; }
            else if (f instanceof SimpleFormula) { return merge((SimpleFormula) f, history); }
            else { throw new RuntimeException("unexpected case"); }
        }

        public ConstraintFormula or(ConstraintFormula f, SubtypeHistory history) {
            if (f.isTrue()) { return f; }
            else if (f.isFalse()) { return this; }
            else {
                // simplification for now -- arbitrarily pick one
                return this;
            }
        }
        
        public ConstraintFormula applySubstitution(Lambda<Type, Type> sigma) {
            Map<InferenceVarType, Type> newUppers =
                new HashMap<InferenceVarType, Type>(_upperBounds.size());
            Map<InferenceVarType, Type> newLowers =
                new HashMap<InferenceVarType, Type>(_lowerBounds.size());
            for (Map.Entry<InferenceVarType, Type> e : _upperBounds.entrySet()) {
                newUppers.put((InferenceVarType) sigma.value(e.getKey()), sigma.value(e.getValue()));
            }
            for (Map.Entry<InferenceVarType, Type> e : _lowerBounds.entrySet()) {
                newLowers.put((InferenceVarType) sigma.value(e.getKey()), sigma.value(e.getValue()));
            }
            return new SimpleFormula(newUppers, newLowers);
        }

        public boolean isTrue() { return false; }
        public boolean isFalse() { return false; }

        public String toString() {
            StringBuilder result = new StringBuilder();
            boolean first = true;
            for (InferenceVarType t :
                     CollectUtil.union(_upperBounds.keySet(), _lowerBounds.keySet())) {
                if (first) { result.append("("); first = false; }
                else { result.append(", "); }
                if (_upperBounds.containsKey(t)) {
                    if (_lowerBounds.containsKey(t)) {
                        result.append(_lowerBounds.get(t));
                        result.append(" <: ");
                    }
                    result.append(t);
                    result.append(" <: ");
                    result.append(_upperBounds.get(t));
                }
                else {
                    result.append(t);
                    result.append(" :> ");
                    result.append(_lowerBounds.get(t));
                }
            }
            result.append(")");
            return result.toString();
        }

        private ConstraintFormula merge(SimpleFormula f, SubtypeHistory history) {
            debug.logStart(new String[]{"this", "f"}, this, f);
            Map<InferenceVarType, Type> uppers = new HashMap<InferenceVarType, Type>();
            Map<InferenceVarType, Type> lowers = new HashMap<InferenceVarType, Type>();
            ConstraintFormula conditions = TRUE;

            Set<InferenceVarType> upperVars = CollectUtil.union(_upperBounds.keySet(), f._upperBounds.keySet());
            Set<InferenceVarType> lowerVars = CollectUtil.union(_lowerBounds.keySet(), f._lowerBounds.keySet());
            // Optimization may be possible here -- Sukyoung
            for (InferenceVarType t : CollectUtil.union(upperVars, lowerVars)) {
                Type upper = null;
                Type lower = null;
                if (_upperBounds.containsKey(t) && f._upperBounds.containsKey(t)) {
                    upper = history.meet(_upperBounds.get(t), f._upperBounds.get(t));
                }
                else if (_upperBounds.containsKey(t)) { upper = _upperBounds.get(t); }
                else if (f._upperBounds.containsKey(t)) { upper = f._upperBounds.get(t); }
                if (_lowerBounds.containsKey(t) && f._lowerBounds.containsKey(t)) {
                    lower = history.join(_lowerBounds.get(t), f._lowerBounds.get(t));
                }
                else if (_lowerBounds.containsKey(t)) { lower = _lowerBounds.get(t); }
                else if (f._lowerBounds.containsKey(t)) { lower = f._lowerBounds.get(t); }

                // What is this for? -- Sukyoung
                if (_upperBounds.containsKey(t) && f._lowerBounds.containsKey(t) ||
                    _lowerBounds.containsKey(t) && f._upperBounds.containsKey(t)) {
                    conditions = conditions.and(history.subtype(lower, upper), history);
                    if (!conditions.isTrue()) {
//                    if (conditions.isFalse()) {
                        debug.logEnd("result", FALSE);
                        return FALSE;
                    }
                }
                if (upper != null) { uppers.put(t, upper); }
                if (lower != null) { lowers.put(t, lower); }
            }
            ConstraintFormula result = new SimpleFormula(uppers, lowers).and(conditions, history);
            debug.logEnd("result", result);
            return result;
        }
    }


    public static ConstraintFormula upperBound(InferenceVarType var, Type bound, SubtypeHistory history) {
        debug.logStart(new String[]{"var","upperBound"}, var, bound);
        if (history.subtype(ANY, bound).isTrue()) {
            debug.logEnd("result", TRUE);
            return TRUE;
        }
        else {
            ConstraintFormula result =
                new SimpleFormula(Collections.singletonMap(var, bound),
                                  Collections.<InferenceVarType, Type>emptyMap());
            debug.logEnd("result", result);
            return result;
        }
    }

    public static ConstraintFormula lowerBound(InferenceVarType var, Type bound, SubtypeHistory history) {
        debug.logStart(new String[]{"var","lowerBound"}, var, bound);
        if (history.subtype(bound, BOTTOM).isTrue()) {
            debug.logEnd("result", TRUE);
            return TRUE;
        }
        else {
            ConstraintFormula result =
                new SimpleFormula(Collections.<InferenceVarType, Type>emptyMap(),
                                  Collections.singletonMap(var, bound));
            debug.logEnd("result", result);
            return result;
        }
    }

    public static ConstraintFormula fromBoolean(boolean b) {
        return b ? TRUE : FALSE;
    }

}
