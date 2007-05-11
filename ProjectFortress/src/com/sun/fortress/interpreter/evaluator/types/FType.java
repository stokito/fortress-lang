/*******************************************************************************
    Copyright 2007 Sun Microsystems, Inc.,
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

package com.sun.fortress.interpreter.evaluator.types;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.interpreter.evaluator.ProgramError;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.interpreter.nodes.IdType;
import com.sun.fortress.interpreter.nodes.RestType;
import com.sun.fortress.interpreter.nodes.StaticParam;
import com.sun.fortress.interpreter.nodes.TypeRef;
import com.sun.fortress.interpreter.useful.ABoundingMap;
import com.sun.fortress.interpreter.useful.BASet;
import com.sun.fortress.interpreter.useful.MagicNumbers;
import com.sun.fortress.interpreter.useful.Useful;


abstract public class FType implements Comparable<FType> {

    protected static final boolean DUMP_UNIFY = false;

    static Comparator<FType> comparator = new Comparator<FType>() {

        public int compare(FType arg0, FType arg1) {
            return arg0.compareTo(arg1);
        }

    };

    // static Random random = new Random(0xd06f00d);

    /* (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(FType arg0) {
        return serial - arg0.serial; /* Assume fewer than a billion types, hence no overflow. */
    }

    final String name;
    private final int serial;
    private final int hash;
    private BASet<FType> excludes = new BASet<FType>(comparator);
    private static int counter;
    protected volatile List<FType> transitiveExtends;
    // Must be volatile due to lazy initialization / double-checked locking.

    protected boolean isSymbolic;
    protected boolean cannotBeExtended;

    public synchronized void resetState() {
        transitiveExtends = null;
        excludes = new BASet<FType>(comparator);
    }

    public final boolean isSymbolic() {
        return isSymbolic;
    }

    public final boolean cannotBeExtended() {
        return cannotBeExtended;
    }

    public int hashCode() {
        return hash;
    }

    public String getName() {
        return name;
    }

    protected FType(String s) {
        name = s;
        synchronized(FType.class) {
            serial = ++counter;
        }
        hash = MagicNumbers.uniformHash(serial, MagicNumbers.F, MagicNumbers.T);
    }

    /**
     * Returns true iff val is a member of a (improper) subtype
     * of this type.
     * @param val
     * @return
     */
    public boolean typeMatch(FValue val) {
        // System.out.println(name+".typeMatch("+val+")");
        if (val == null)
            return false;
        if (val.type() == null)
            return false;
        return val.type().subtypeOf(this);
    }

    public String toString() {
        return name;
    }

    public List<FType> getExtends() {
        return Collections.emptyList();
    }

    protected List<FType> computeTransitiveExtends() {
        return Useful.<FType>list(this);
    }

    /**
     * Note that transitive extends includes this type.
     * Traits and objects do something more complex.
     *
     * @return
     */
    public List<FType> getTransitiveExtends() {
        if (transitiveExtends == null) {
            List<FType> tmp = computeTransitiveExtends();
            synchronized (this) {
                if (transitiveExtends == null) {
                    transitiveExtends = tmp;
                }
            }
        }
        return transitiveExtends;
    }

    public Set<FType> getExcludes() {
        return excludes.copy();
    }

    public void addExclude(FType t) {
        this.addExcludesInner(t);
        t.addExcludesInner(this);
    }

    private void addExcludesInner(FType t) {
        if (t == this)
            throw new ProgramError("TypeRef cannot exclude itself: " + t);
        excludes.syncPut(t);
    }

    public boolean excludesOther(FType other) {
        if (other instanceof FTypeArrow) // FTypeArrow handles other case
            return true;
        else return excludesOtherInner(other);
    }

    private boolean excludesOtherInner(FType other) {

        if (this == other)
            return false;

        // If neither type can be extended, then they exclude.

        // If one cannot be extended, and the other is not an
        // actual super, then they exclude.

        // Otherwise, look for exclusion in the supertypes.
        if (cannotBeExtended()) {
            if (other.cannotBeExtended)
                return true;

            // Optimization hack -- check memoized exclusion before doing work.
            if (excludes.contains(other))
                return true;

            if (getTransitiveExtends().contains(other))
                return false;

            // Memoize
            this.addExclude(other);
            return true;


        } else if (other.cannotBeExtended()) {
            // Optimization hack -- check memoized exclusion before doing work.
            if (excludes.contains(other))
                return true;

            if (other.getTransitiveExtends().contains(this))
                return false;

            // Memoize
            this.addExclude(other);
            return true;

        }

        // Not necessarily an optimization hack here; this is part of
        // the definition, but it also probes memoized results.

        if (excludes.contains(other))
            return true;

        if (other.getExcludes().contains(this)) {
            this.addExclude(other);
            return true;
        }

        for (FType t : getTransitiveExtends()) {
            BASet t_excludes = t.excludes;
            for (FType o : other.getTransitiveExtends()) {
                if ( !(t == this && o == other) && t_excludes.contains(o)) {
                    // Short-circuit any future queries
                    this.addExclude(o);
                    return true;
                }
            }
        }
        return false;

    }

    public Environment getEnv() {
        return BetterEnv.empty();
    }

    protected final boolean commonSubtypeOf(FType other) {
        return (this == other || other == FTypeDynamic.T || other==FTypeTop.T);
    }

    /**
     * Returns "this subtypeof other"
     * @param other
     * @return
     */
    @SuppressWarnings(value={"unchecked"})
    public boolean subtypeOf(FType other) {
        if (other == FTypeDynamic.T || other == FTypeTop.T)
            return true;
        Class us = getClass();
        Class them = other.getClass();
        if (us == them) return true;
        // TODO There's some type instantiations missing, but I do not know what
        // good ones would be, and the code is correct anyway.
        return them.isAssignableFrom(us);
    }

    public boolean equals(Object other) {
        if (this == other) return true;
        if (! (other instanceof FType)) return false;
        return this.getClass().equals(other.getClass());
    }

    /* Sadly, meet needs an iteration similar to join below, but lacks
     * the necessary subclass information. */
    public Set<FType> meet(FType t2) {
        if (this.equals(t2)) {
            return Useful.set(this);
        } else if (this.subtypeOf(t2)) {
            // Meet of X with dynamic is X.
            return Useful.set(this);
        } else if (t2.subtypeOf(this)) {
            return Useful.set(t2);
        }
        return Collections.emptySet();
    }

    public Set<FType> join(FType t2) {
        if (this.equals(t2)) {
            return Useful.set(this);
        } else if (t2.subtypeOf(this)) {
            // Join of x with dynamic is X.
            return Useful.set(this);
        } else if (this.subtypeOf(t2)) {
            return Useful.set(t2);
        } else {
            Set<FType> result = Collections.emptySet();
            for (FType candidate : getTransitiveExtends()) {
                if (!t2.subtypeOf(candidate)) continue;
                TreeSet<FType> newresult = new TreeSet<FType>();
                boolean addC = true;
                for (FType r : result) {
                    if (r.subtypeOf(candidate)) {
                        addC = false;
                        break;
                    } else if (candidate.subtypeOf(r)) {
                        /* Drop r */
                    } else {
                        newresult.add(r);
                    }
                }
                if (addC) {
                    newresult.add(candidate);
                    result = newresult;
                }
            }
            return result;
        }
    }

    /**
     * @param evaled
     * @return
     */
    public static Set<FType> join(List<FValue> evaled) {
        // A is the accumulated set of joined items.
        Set<FType> a = null;
        if (evaled.size() > 0) {
            for (FValue v: evaled) {
                // for each value, join in the value's type.
                TreeSet<FType> b = new TreeSet<FType>();
                FType vt = v.type();
                if (a == null)
                    b.add(vt);
                else {
                    // For each type in the accumulator, do the
                    // join with the next element, accumulating in b.
                    for (FType t : a) {
                        if (b.size() == 0)
                            b.add(t);
                        else
                            b.addAll(t.join(vt));
                    }
                    // b is our new "a".

                }
                a = b;

            }
        } else {
            // Empty set, best we can do
            a = FTypeDynamic.SingleT;
        }
        return a;
    }
    /**
     * Returns true iff candidate is more specific than current.
     * Note that it is possible that neither one of a pair of types
     * is more specific than the other,
     *
     * @param candidate
     * @param current
     * @return
     */
    public static boolean  moreSpecificThan(List<FType> candidate, List<FType> current) {
        boolean candidate_better = false;
        boolean best_better = false;

        if (definitelyShorterThan(candidate, current) ||
            definitelyShorterThan(current, candidate))
            return false;

        int l = candidate.size();
        if (l < current.size())
            l = current.size();

        for (int j = 0; j < candidate.size(); j++) {
            FType cand_type = Useful.clampedGet(candidate, j).deRest();
            FType best_type = Useful.clampedGet(current, j).deRest();

            if (cand_type.equals(best_type)) {
                continue;
            }

            if (cand_type.subtypeOf(best_type))
                candidate_better = true;
            if (best_type.subtypeOf(cand_type))
                best_better = true;
        }
        return candidate_better && !best_better;
    }


     public FType deRest() {
        return this;
    }

    /**
     * @param candidate
     * @param current
     * @return
     */
    private static boolean definitelyShorterThan(List<FType> candidate, List<FType> current) {
        return candidate.size() < current.size() && !(candidate.get(candidate.size()-1) instanceof FTypeRest);
    }

    protected boolean unifyNonVar(BetterEnv env, Set<StaticParam> tp_set,
            ABoundingMap<String, FType, TypeLatticeOps> abm, TypeRef val) {
        if (DUMP_UNIFY) {
            System.out.println("unify FType "+this+" and "+val);
            System.out.println("    ("+this.getClass().getName()+")");
        }
        return (val instanceof IdType &&
                name.equals(((IdType)val).getName().toString()));
    }

    /** One-sided unification of this fully-computed FType with a signature.
     * @param tp_set  The type variables which are subject to unification.
     * @param abm     Map of bounds on these variables (if any), updated.
     * @param val   The signature to be unified with.
     *
     * Does most of the boilerplate work of unification, including
     * variable unification and supertype chasing (in topological
     * order).  The stuff which varies between types is found in
     * unifyNonVar; this is why that method is overridable while this
     * one is final.
     */
    public final void unify(BetterEnv env, Set<StaticParam> tp_set, ABoundingMap<String, FType, TypeLatticeOps> abm, TypeRef val) {
        if (val instanceof RestType) {
            val = ((RestType) val).getType();
        }
        /* Check if val is a type variable */
        if (val instanceof IdType) {
            IdType id_val = (IdType) val;
            String nm = id_val.getName().toString();
            for (StaticParam tp : tp_set) {
                String k = tp.getName();
                if (k.equals(nm)) {
                    if (DUMP_UNIFY) System.out.println("Recording "+k+"="+this);
                    abm.joinPut(k, this);
                    return;
                }
            }
        }
        /* We want to unify with the most specific subtype possible, so */
        ABoundingMap<String,FType,TypeLatticeOps> savedAbm = abm.copy();
        for (FType t : getTransitiveExtends()) {
            if (t.unifyNonVar(env, tp_set, abm, val)) return;
            if (DUMP_UNIFY) System.out.println("            "+this+" !=  "+val);
            abm.assign(savedAbm);
        }
        throw new ProgramError(val,env,"Cannot unify "+this+"("+
                                       this.getClass()+")\n  with "+ val +
                                       "("+val.getClass()+")");
    }

}
