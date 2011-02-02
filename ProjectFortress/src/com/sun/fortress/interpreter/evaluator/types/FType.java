/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.evaluator.types;

import static com.sun.fortress.exceptions.InterpreterBug.bug;
import static com.sun.fortress.exceptions.ProgramError.error;
import static com.sun.fortress.exceptions.ProgramError.errorMsg;
import static com.sun.fortress.exceptions.UnificationError.unificationError;
import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import static com.sun.fortress.interpreter.evaluator.values.OverloadedFunction.*;
import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.useful.*;

import java.util.*;

abstract public class FType implements Comparable<FType> {

    protected static final boolean DUMP_UNIFY = false;

    static public final Comparator<FType> comparator = new Comparator<FType>() {

        public int compare(FType arg0, FType arg1) {
            return arg0.compareTo(arg1);
        }

    };

    static public final ListComparer<FType> listComparer = new ListComparer<FType>();

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
    private Set<FType> excludes = new BASet<FType>(comparator);
    private volatile boolean excludesClosed = false;
    private static int counter;
    protected volatile List<FType> transitiveExtends;
    // Must be volatile due to lazy initialization / double-checked locking.

    private List<Pair<HasAt, FType>> mustExtend;
    // Where clauses in superclasses parameterized by Self can
    // introduce constraints that must be satisfied.

    protected boolean isSymbolic;
    protected boolean cannotBeExtended;

    protected int getSerial() {
        return serial;
    }

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

    public boolean isValueType() {
        return false;
    }

    public int hashCode() {
        return hash;
    }

    public String getName() {
        return name;
    }

    protected FType(String s) {
        name = s;
        synchronized (FType.class) {
            serial = ++counter;
        }
        hash = MagicNumbers.uniformHash(serial, MagicNumbers.F, MagicNumbers.T);
    }

    /**
     * Returns true iff val is a member of a (improper) subtype
     * of this type.
     */
    public boolean typeMatch(FValue val) {
        // System.out.println(name+".typeMatch("+val+")");
        if (val == null) return false;
        if (val.type() == null) return false;
        return val.type().subtypeOf(this);
    }

    public String toString() {
        return name;
    }

    public List<FType> getExtends() {
        return Collections.emptyList();
    }

    protected List<FType> getExtendsNull() {
        return getExtends();
    }

    protected List<FType> computeTransitiveExtends() {
        return Useful.<FType>list(this);
    }

    /**
     * Note that transitive extends includes this type.
     * Traits and objects do something more complex.
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

    private void computeTransitiveExcludes() {
        exclDump("Computing transitive excludes for ", this, ":");
        for (FType t : getExtends()) {
            for (FType x : t.getExcludes()) {
                addExclude(x);
                exclDump(t, " excludes ", x, "; ");
            }
        }
        excludesClosed = true;
        excludes = Collections.unmodifiableSet(new HashSet<FType>(excludes));
    }

    public Set<FType> getExcludes() {
        if (!excludesClosed) computeTransitiveExcludes();
        return excludes;
    }

    public Set<FType> getTransitiveComprises() {
        BASet<FType> res = new BASet<FType>(comparator);
        res.add(this);
        return res;
    }

    public Set<FType> getComprises() {
        return null;
    }

    public void addExclude(FType t) {
        if (t == this) {
            exclDumpSkip();
            if (t instanceof FTraitOrObject) {
                bug(((FTraitOrObject) this).getAt(), errorMsg("Type cannot exclude itself: ", t));
            } else {
                bug(errorMsg("Type cannot exclude itself: ", t));
            }
        }
        if (t instanceof SymbolicType || this instanceof SymbolicType) {
            // exclDumpSkip();
            // System.out.println("Added symbolic excl "+t+" and "+this);
        }
        this.addExcludesInner(t);
        t.addExcludesInner(this);
    }

    private void addExcludesInner(FType t) {
        if (excludes instanceof BASet) {
            ((BASet<FType>) excludes).syncPut(t);
        }
    }

    public boolean excludesOther(FType other) {
        if (this == other) {
            exclDumpln("No.  Equal.");
            return false;
        }
        if (other instanceof FTypeArrow) // FTypeArrow handles other case
            return true;
        if (other instanceof FTypeTuple) // FTypeTuple handles other case
            return true;
        if (excludesOtherSimply(other)) {
            return true;
        }
        return excludesOtherInner(other);
    }

    protected boolean excludesOtherSimply(FType other) {
        // If neither type can be extended, then they exclude.
        if (cannotBeExtended() && other.cannotBeExtended()) {
            exclDumpln("Excludes.  Neither can be extended.");
            return true;
        }
        if (this.getExcludes().contains(other)) {
            exclDumpln("Excludes (cached).");
            return true;
        }
        if (other.getExcludes().contains(this)) {
            exclDumpln("Other excludes (transitive closure just happened).");
            return true;
        }
        return false;
    }

    protected boolean excludesOtherInner(FType other) {
        // If other is Symbolic, use it instead.
        if (other instanceof SymbolicType) {
            exclDump("\nFlipping;");
            return other.excludesOtherInner(this);
        }
        // Look in the supertypes of non-extensible type.
        if (cannotBeExtended()) {
            if (getTransitiveExtends().contains(other)) {
                exclDumpln("No.  Transitive extend contains.");
                return false;
            }

            exclDumpln("Excludes.  Non-extensible + no supertype");
            // Memoize
            this.addExclude(other);
            return true;

        } else if (other.cannotBeExtended()) {
            if (other.getTransitiveExtends().contains(this)) {
                exclDumpln("No.  Contains transitive extend.");
                return false;
            }

            exclDumpln("Excludes.  No supertype + non-extensible");
            // Memoize
            this.addExclude(other);
            return true;
        }
        // Next, check that a supertype of other isn't excluded by us.
        exclDump("\nChecking for supertype exclusion of ", this, ": ");
        for (FType t1 : other.getTransitiveExtends()) {
            if (excludes.contains(t1)) {
                exclDumpln("Excludes supertype ", t1, ".");
                addExclude(other);
                return true;
            }
            exclDump("Not ", t1, "; ");
        }
        // And vice versa.
        exclDump("\nChecking for supertype exclusion of ", other, ": ");
        Set<FType> oexcl = other.getExcludes();
        for (FType t1 : this.getTransitiveExtends()) {
            if (oexcl.contains(t1)) {
                exclDumpln("Excludes supertype ", t1, ".");
                addExclude(other);
                return true;
            }
            exclDump("Not ", t1, "; ");
        }
        // We shouldn't need to check the other way 'round by reflexivity.
        // Now check transitive comprises for exclusion.
        // If any does not exclude, we don't exclude.  If all pairs
        // exclude, we exclude.
        if (getComprises() == null && other.getComprises() == null) {
            exclDumpln("No comprises clauses, no exclusion.");
            return false;
        }
        exclDump("\n");
        for (FType t1 : getTransitiveComprises()) {
            for (FType t2 : other.getTransitiveComprises()) {
                exclDump("Checking extends of comprised ", t1, " and ", t2, "; ");
                if (!t1.excludesOther(t2)) {
                    return false;
                }
            }
        }
        exclDumpln("Comprises all exclude.");
        addExclude(other);
        return true;
    }

    public Environment getWithin() {
        throw new Error("Not supposed to happen");
    }

    protected final boolean commonSubtypeOf(FType other) {
        return (this == other || other == FTypeTop.ONLY);
    }

    /**
     * Returns "this subtypeof other"
     */
    public boolean subtypeOf(FType other) {
        if (this == other) return true;
        if (other == FTypeTop.ONLY) return true;
        if (other == BottomType.ONLY) return false;
        bug(errorMsg("Couldn't figure out ", this, " <: ", other));
        return false;
        // // This is the old reflection-loving subtype check.
        // // We shouldn't ever use it anymore.
        // Class us = getClass();
        // Class them = other.getClass();
        // System.err.println("Resorting to reflection for "+us+" <: "+them);
        // if (us == them) return true;
        // // TODO There's some type instantiations missing, but I do not know what
        // // good ones would be, and the code is correct anyway.
        // return them.isAssignableFrom(us);
    }

    public boolean equals(Object other) {
        return this == other;
        //        if (this == other) return true;
        //        if (! (other instanceof FType)) return false;
        //        return this.getClass().equals(other.getClass());
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

    public static Set<FType> join(List<FValue> evaled) {
        List<FType> tys = new ArrayList<FType>(evaled.size());
        for (FValue v : evaled) {
            tys.add(v.type());
        }
        return joinTypes(tys);
    }

    public static Set<FType> joinTypes(List<FType> types) {
        // A is the accumulated set of joined items.
        Set<FType> a = null;
        if (types.size() > 0) {
            for (FType vt : types) {
                // for each value, join in the value's type.
                TreeSet<FType> b = new TreeSet<FType>();
                if (a == null) {
                    b.add(vt);
                } else {
                    // For each type in the accumulator, do the
                    // join with the next element, accumulating in b.
                    for (FType t : a) {
                        b.addAll(t.join(vt));
                    }
                }
                a = b;
            }
            // Now a may contain non-minimal types.
            if (a.size() > 1) {
                TreeSet<FType> r = new TreeSet<FType>();
                for (FType c : a) {
                    if (r.size() == 0) {
                        r.add(c);
                    } else {
                        boolean addC = true;
                        TreeSet<FType> s = new TreeSet<FType>();
                        for (FType d : r) {
                            if (c.subtypeOf(d)) {
                                /* Drop d */
                            } else if (d.subtypeOf(c)) {
                                addC = false;
                                s = r;
                                break;
                            } else {
                                s.add(d);
                            }
                        }
                        if (addC) s.add(c);
                        r = s;
                    }
                }
                a = r;
            }
        } else {
            // Empty set, best we can do
            a = FTypeTop.SingleSet; // why isn't this a real empty set?
        }
        return a;
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
        int sz = candidate.size();
        return sz < current.size() && (sz == 0 || !(candidate.get(sz - 1) instanceof FTypeRest));
    }

    protected boolean unifyNonVar(Environment env,
                                  Set<String> tp_set,
                                  BoundingMap<String, FType, TypeLatticeOps> abm,
                                  Type val) {
        boolean rc = false;
        FType other = null;
        if (!(val instanceof VarType)) {
            if (DUMP_UNIFY) System.out.print("builtin ");
            if (val instanceof TupleType && NodeUtil.isVoidType((TupleType) val)) {
                other = FTypeVoid.ONLY;
            }
        } else if (name.equals(NodeUtil.nameString(((VarType) val).getName()))) {
            if (DUMP_UNIFY) System.out.print("iso ");
            rc = true;
        } else {
            VarType vtval = (VarType) val;
            other = env.getTypeNull(vtval);


            if (DUMP_UNIFY && other == null) System.out.print("undef second ");
        }
        if (!rc) {
            if (abm.isForward()) {
                // Let unification succeed if there's a subtype relationship.
                if (other == null) {
                    // Uninitialized type.  Deal gracefully with absent type info.
                    rc = this instanceof BottomType;
                } else {
                    rc = this.subtypeOf(other);
                }
            } else {
                // Let unification succed if there's reverse subtyping.
                if (other == null) {
                    // Uninitialized type.  Deal gracefully with absent type info.
                    rc = this instanceof FTypeTop;
                } else {
                    rc = other.subtypeOf(this);
                }
            }
        }
        if (DUMP_UNIFY) {
            System.out.println("unify FType " + this + " and " + val + (rc ? " OK " : " NO ") + "(" +
                               this.getClass().getSimpleName() + "), abm=" + abm);
        }

        return rc;
    }

    /**
     * Utility function that provides unifyNonVar implementation for
     * symbolic types such as nat and bool.
     */
    public static boolean unifySymbolic(FType self,
                                        Environment env,
                                        Set<String> tp_set,
                                        BoundingMap<String, FType, TypeLatticeOps> abm,
                                        Type val) {
        /* Unification has failed due to a fundamental kind error.
           Report that and fail. */
        unificationError(val, env, errorMsg("Can't unify nat parameter ", self, " and  type argument ", val));
        return false;
    }


    /**
     * One-sided unification of this fully-computed FType with a signature.
     *
     * @param tp_set The type variables which are subject to unification.
     * @param abm    Map of bounds on these variables (if any), updated.
     * @param val    The signature to be unified with.
     *               <p/>
     *               Does most of the boilerplate work of unification, including
     *               variable unification and supertype chasing (in topological
     *               order).  The stuff which varies between types is found in
     *               unifyNonVar; this is why that method is overridable while this
     *               one is final.
     */
    public final void unify(Environment env,
                            Set<String> tp_set,
                            BoundingMap<String, FType, TypeLatticeOps> abm,
                            Type val) {
        /* anything can unify with Any */
        if (val instanceof AnyType) {
            return;
        }
        /* Check if val is a type variable */
        else if (val instanceof VarType) {
            VarType id_val = (VarType) val;
            String nm = NodeUtil.nameString(id_val.getName());
            if (tp_set.contains(nm)) {
                if (DUMP_UNIFY) System.out.print(
                        "Trying " + nm + "=" + this + "(" + val.getClass().getSimpleName() + ")");
                try {
                    abm.joinPut(nm, this);
                }
                catch (EmptyLatticeIntervalError el) {
                    if (DUMP_UNIFY) System.out.println("Out of bounds");
                    unificationError(val, errorMsg("Actual type ", this, " out of bounds for variable ", nm));
                    return;
                }
                catch (Error th) {
                    if (DUMP_UNIFY) System.out.println(" fail " + th.getMessage());
                    throw th;
                }
                catch (RuntimeException th) {
                    if (DUMP_UNIFY) System.out.println(" fail " + th.getMessage());
                    throw th;
                }
                if (DUMP_UNIFY) System.out.println(" result abm= " + abm);
                return;
            }
        }
        /* We want to unify with the most specific subtype possible, so */
        BoundingMap<String, FType, TypeLatticeOps> savedAbm = abm.copy();
        for (FType t : getTransitiveExtends()) {
            if (t.unifyNonVar(env, tp_set, abm, val)) return;
            if (DUMP_UNIFY) System.out.println("            " + t + " !=  " + val + ", abm=" + abm);
            abm.assign(savedAbm);
        }
        if (DUMP_UNIFY) System.out.println("    Can't unify " + this + " with " + val);
        unificationError(val, env, errorMsg("Cannot unify ",
                                            this,
                                            "(",
                                            this.getClass(),
                                            ")\n  with ",
                                            val,
                                            "(",
                                            val.getClass(),
                                            ") abm=" + abm));
    }

    /**
     * Unify with a static arg.
     */
    public void unifyStaticArg(Environment env,
                               Set<String> tp_set,
                               BoundingMap<String, FType, TypeLatticeOps> abm,
                               StaticArg val) {
        if (DUMP_UNIFY) System.out.println("    Can't unify " + this + " with " + val);
        unificationError(val, env, errorMsg("Cannot unify ",
                                            this,
                                            "(",
                                            this.getClass(),
                                            ")\n  with ",
                                            val,
                                            "(",
                                            val.getClass(),
                                            ") abm=" + abm));
    }


    public void mustExtend(FType st, HasAt constraint_loc) {
        List<FType> curr_extends = getExtendsNull();
        if (curr_extends == null) {

            if (mustExtend == null) mustExtend = new ArrayList<Pair<HasAt, FType>>();

            mustExtend.add(new Pair<HasAt, FType>(constraint_loc, st));
        } else {
            if (!subtypeOf(st)) {
                error(constraint_loc, errorMsg("", this, " must subtype ", st));
            }
        }
    }

    protected void checkConstraints() {
        if (mustExtend != null) {
            String failures = "";
            for (Pair<HasAt, FType> p : mustExtend) {
                if (!subtypeOf(p.getB())) {
                    String failure = errorMsg("At ", p.getA(), " ", this, " must subtype ", p.getB());
                    if (failures.length() == 0) failures = failure;
                    else failures = failures + "\n" + failure;
                }
            }
            if (failures.length() > 0) error(failures);
        }
        mustExtend = null;
    }

    public static boolean anyAreSymbolic(List<FType> args) {
        for (FType t : args) {
            if (t.isSymbolic()) return true;
        }
        return false;
    }
}
