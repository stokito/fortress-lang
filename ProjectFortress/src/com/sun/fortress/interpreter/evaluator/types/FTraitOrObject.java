/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.evaluator.types;

import com.sun.fortress.exceptions.FortressException;
import static com.sun.fortress.exceptions.InterpreterBug.bug;
import static com.sun.fortress.exceptions.ProgramError.error;
import static com.sun.fortress.exceptions.ProgramError.errorMsg;
import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.interpreter.evaluator.EvalType;
import com.sun.fortress.interpreter.evaluator.values.*;
import com.sun.fortress.nodes.*;
import com.sun.fortress.useful.*;

import java.util.*;

abstract public class FTraitOrObject extends FTraitOrObjectOrGeneric {


    List<FType> extends_;
    final private HasAt at;

    volatile List<FType> properTransitiveExtends;
    // Must be volatile due to lazy initialization / double-checked locking.

    /**
     * Exactly what methods (traits) or fields (objects) are
     * supplied by this object or trait.
     */
    volatile BetterEnv suppliedMembers = null; // never written...

    /**
     * Exactly what methods (traits) or fields (objects) are
     * required by this object or trait.  For objects, this
     * must be empty; if it isn't, the contents form an error
     * message.
     */
    volatile BetterEnv requiredMembers = null; // never written...

    abstract protected void finishInitializing();

    abstract public BetterEnv getMembers();

    public BetterEnv getSuppliedMembers() {
        initializeRequiredAndSupplied();
        return suppliedMembers;
    }

    public BetterEnv getRequiredMembers() {
        initializeRequiredAndSupplied();
        return requiredMembers;
    }

    /**
     * Set extends, excludes, and replace the environment.
     *
     * @param extends_
     * @param excludes
     * @param replacementEnv
     */
    final public void setExtendsAndExcludes(List<FType> extends_, List<FType> excludes, Environment replacementEnv) {
        env = replacementEnv;
        setExtendsAndExcludes(extends_, excludes);
    }

    final public void setExtendsAndExcludes(List<FType> extends_, List<FType> excludes) {
        if (this.extends_ != null) throw new IllegalStateException("Second set of extends");
        if (extends_.size() == 0) {
            extends_ = FTypeTop.ONLY.getTransitiveExtends();
        }
        this.extends_ = extends_;
        // Here we need to add things to where clauses

        env.bless();
        initializeExcludes(excludes);
        finishInitializing();
        checkConstraints();
        enforceValueness();
    }

    private void initializeExcludes(List<FType> excludes) {
        if (excludes != null) for (FType t : excludes) {
            addExclude(t);
        }
    }

    private void enforceValueness() {
        if (isValueType()) return;
        for (FType t : getExtends()) {
            if (t instanceof FTraitOrObjectOrGeneric && t.isValueType()) {
                error(at, errorMsg(this, ": non-value subtype of value type ", t));
            }
        }
    }

    public List<FType> getExtends() {
        if (extends_ == null) bug(at, errorMsg(this, ": Get of unset extends"));
        // throw new IllegalStateException("Get of unset extends");
        return extends_;
    }

    /**
     * Returns extends, without checking for null;
     * if null, then extends has not yet been initialized.
     * Used by FType to implement constraint checks from
     * where clauses.
     *
     * @return
     */
    @Override
    protected List<FType> getExtendsNull() {
        return extends_;
    }


    /**
     * Only implemented by subtypes which extend GenericTypeInstance.
     * Method included here to permit sharing of the complicated code in
     * unifyNonVarGeneric.
     */
    protected FTypeGeneric getGeneric() {
        return bug(errorMsg("getGeneric() of non-Generic ", this));
    }

    /**
     * When dealing with operator type instantiation, we create a fresh
     * copy of the body and a fresh type to match.  This method allows
     * us to get back the original FTypeGeneric that gave rise to the type.
     */
    protected FTypeGeneric getOriginal() {
        return getGeneric().getOriginal();
    }

    /**
     * Only implemented by subtypes which extend GenericTypeInstance.
     * Method included here to permit sharing of the complicated code in
     * unifyNonVarGeneric.
     */
    protected List<FType> getTypeParams() {
        return bug(errorMsg("getTypeParams() of non-Generic ", this));
    }

    protected List<FType> getTypeParamsForName() {
        return bug(errorMsg("getTypeParamsForName() of non-Generic ", this));
    }

    protected List<FType> computeTransitiveExtends() {
        return traitsSortedBySpecificity();
    }

    /**
     * Transitive closure of extends, not including self
     *
     * @return
     */
    public List<FType> getProperTransitiveExtends() {
        if (properTransitiveExtends == null) {
            List<FType> tmp = getTransitiveExtends();
            tmp = tmp.subList(1, tmp.size());
            synchronized (this) {
                if (properTransitiveExtends == null) {
                    properTransitiveExtends = tmp;
                }
            }
        }
        return properTransitiveExtends;
    }

    public HasAt getAt() {
        return at;
    }


    public String toString() {
        // TODO need to carry generic parameters through, perhaps.
        if (this instanceof GenericTypeInstance) {
            GenericTypeInstance gti_this = (GenericTypeInstance) this;
            return getName() + Useful.listInOxfords(gti_this.getTypeParamsForName());
        } else {
            return getName();
        }
    }

    public FTraitOrObject(String name, Environment env, HasAt at, List<Decl> members, AbstractNode def) {
        super(name, env, def);
        this.members = members;
        this.at = at;
    }

    public boolean equals(Object other) {
        return this == other;
    }

    public boolean subtypeOf(FType other) {
        // System.out.println(this+".subtypeOf("+other+")");
        if (commonSubtypeOf(other)) return true;
        for (FType t : getExtends()) {
            // Subtyping is transitive, right?
            // i.e. if S extends T, T extends U, then S subtypeof U
            if (t.subtypeOf(other)) return true;
        }
        return false;
    }

    private List<FType> traitsSortedBySpecificity() {
        ArrayList<TopSortItemImpl<FType>> unsorted = new ArrayList<TopSortItemImpl<FType>>();
        HashMap<FType, TopSortItemImpl<FType>> toItems = new HashMap<FType, TopSortItemImpl<FType>>();
        visitTrait(this, unsorted, toItems);
        List<TopSortItemImpl<FType>> sorted = TopSort.<TopSortItemImpl<FType>>breadthFirst(unsorted);
        ArrayList<FType> l = new ArrayList<FType>(sorted.size());
        for (TopSortItemImpl<FType> x : sorted) {
            l.add(x.x);
        }
        return l;
    }

    /**
     * Add trait to the extends relation for topsorting.
     * Recursively add all extended traits to the extends relation.
     *
     * @param t        Input, the trait to add to the relation.
     * @param unsorted Output, an unsorted list of items (traits)
     * @param toItems  Working storage, used to keep track of trait-to-item mapping.
     * @return
     */
    private static TopSortItemImpl<FType> visitTrait(FType t,
                                                     ArrayList<TopSortItemImpl<FType>> unsorted,
                                                     HashMap<FType, TopSortItemImpl<FType>> toItems) {
        TopSortItemImpl<FType> tsi = toItems.get(t);
        if (tsi == null) {
            tsi = new TopSortItemImpl<FType>(t);
            unsorted.add(tsi);
            toItems.put(t, tsi);
        }
        for (FType e : t.getExtends()) {
            if (e instanceof FType) {
                tsi.edgeTo(visitTrait((FType) e, unsorted, toItems));
            }
        }
        return tsi;
    }

    /**
     * An implementation of unifyNonVar specifically for types that
     * implement GenericTypeInstance.  They should override
     * unifyNonVar with a call to this method.
     */
    protected final boolean unifyNonVarGeneric(Environment e,
                                               Set<String> tp_set,
                                               BoundingMap<String, FType, TypeLatticeOps> abm,
                                               Type val) {
        if (val instanceof TraitSelfType)
            val = ((TraitSelfType)val).getNamed();
        if (DUMP_UNIFY) System.out.println("unify GT/O  " + this + " and " + val + " abm= " + abm);
        if (!(val instanceof TraitType)) {
            if (DUMP_UNIFY) System.out.println("   not TraitType");
            return false;
        }
        TraitType pt = (TraitType) val;
        // Use "e", not "env".
        EvalType eval_type = new EvalType(e.getTopLevel());
        // The if this is a generic-in-opr generic type, check that
        // the original (pre-partial-instantiation) type matches
        // the name in this context.
        FType eval_val_generic = eval_type.evalType(pt);
        if (getOriginal() != eval_val_generic) {
            if (DUMP_UNIFY) System.out.println(
                    "   getOriginal " + getOriginal() + " != " + eval_val_generic + "    " + getGeneric().getClass() +
                    " and " + eval_val_generic.getClass());
            return false;
        }
        Iterator<StaticArg> val_args_iterator = pt.getArgs().iterator();
        StaticArg targ = null;
        FType t = null;
        try {
            for (FType param_ftype : getTypeParams()) {
                targ = val_args_iterator.next();
                t = param_ftype;
                if (targ instanceof TypeArg) {
                    param_ftype.unify(env, tp_set, abm, ((TypeArg) targ).getTypeArg());
                } else if (param_ftype instanceof FTypeNat) {
                    param_ftype.unifyStaticArg(env, tp_set, abm, targ);
                } else {
                    bug(val, e, errorMsg("Can't handle unification of parameters ", this, " and ", val));
                }
            }
        }
        catch (FortressException p) {
            if (DUMP_UNIFY) System.out.println("   Arg unification failed.");
            return false;
        }
        catch (EmptyLatticeIntervalError p) {
            if (DUMP_UNIFY) System.out.println("Interval went empty unifying " + t + " and " + targ);
            return false;
        }
        return true;
    }

    public void initializeRequiredAndSupplied() {
        if (requiredMembers == null) {
            synchronized (this) {
                if (requiredMembers == null) {
                    /* Begin with data about this type */

                    MultiMap<String, Overload> thisRequires = new MultiMap<String, Overload>();
                    MultiMap<String, Overload> thisSupplies = new MultiMap<String, Overload>();
                    MultiMap<String, Overload> overrides = new MultiMap<String, Overload>();

                    MultiMap<String, Overload> allMethods = new MultiMap<String, Overload>();

                    BetterEnv m = getMembers();

                    for (String s : m.youngestFrame()) {
                        FValue fv = m.getLeafValue(s);

                        if (fv instanceof OverloadedFunction) {
                            // Treat the overloaded function as a bag of separate
                            // definitions.
                            List<Overload> overloads = ((OverloadedFunction) fv).getOverloads();
                            for (Overload ov : overloads) {
                                MethodClosure sfcn = (MethodClosure) (ov.getFn());
                                methodClosureIntoRandS(s, sfcn, thisRequires, thisSupplies, overrides);
                            }
                        } else if (fv instanceof MethodClosure) methodClosureIntoRandS(s,
                                                                                       (MethodClosure) fv,
                                                                                       thisRequires,
                                                                                       thisSupplies,
                                                                                       overrides);
                    }

                    MultiMap<String, Overload> parentRequires = new MultiMap<String, Overload>();
                    MultiMap<String, Overload> parentSupplies = new MultiMap<String, Overload>();

                    // Supplied = union[exts] supplied + own supplied
                    // Required = union[exts] required + own required - Supplied

                    List<FType> parents = getExtends();
                    for (FType parent : parents) {
                        if (parent instanceof FTraitOrObject) {
                            FTraitOrObject toparent = (FTraitOrObject) parent;
                            BetterEnv ps = toparent.getSuppliedMembers();
                            BetterEnv pr = toparent.getRequiredMembers();
                            addEnvToMultiMap(ps, parentSupplies);
                            addEnvToMultiMap(pr, parentRequires);
                            addEnvToMultiMap(ps, allMethods);
                            addEnvToMultiMap(pr, allMethods);
                        } else {
                            // Some other sort of type.
                        }
                    }

                    // OverloadedFunction.OverloadComparisonResult ocr = new OverloadedFunction.OverloadComparisonResult();
                    // DEAD CODE?
                    // Thin allMethods by removing anything covered by an overriding function.
                    // for (String s : overrides.keySet()) {
                    //     Set<Overload> overs = overrides.get(s);
                    //     Set<Overload> alls = allMethods.get(s);
                    //     if (alls == null) continue;
                    //     for (Overload omc : overs) {
                    //         Iterator<Overload> imc = alls.iterator();
                    //         while (imc.hasNext()) {
                    //             Overload amc = imc.next();
                    //         }
                    //     }
                    // }

                    // Add everything from this type, too.
                    addEnvToMultiMap(m, allMethods);

                    // Consistency check: for each key in allMethods, the set of methods (overloadings) must be well-formed.


                }
            }
        }
    }

    private void addEnvToMultiMap(BetterEnv ps, MultiMap<String, Overload> mmap) {
        for (String s : ps.youngestFrame()) {
            FValue fv = ps.getLeafValue(s);

            if (fv instanceof OverloadedFunction) {
                // Treat the overloaded function as a bag of separate
                // definitions.
                List<Overload> overloads = ((OverloadedFunction) fv).getOverloads();
                for (Overload ov : overloads) {
                    MethodClosure mc = (MethodClosure) (ov.getFn());
                    Overload mo = wrapInOverload(mc);
                    mmap.putItem(s, mo);
                }
            } else if (fv instanceof MethodClosure) {
                MethodClosure mc = (MethodClosure) fv;
                Overload mo = wrapInOverload(mc);
                mmap.putItem(s, mo);
            }
        }
    }

    private Overload wrapInOverload(MethodClosure mc) {
        return new Overload.MethodOverload(mc);
    }

    private void methodClosureIntoRandS(String s,
                                        MethodClosure mc,
                                        MultiMap<String, Overload> thisRequires,
                                        MultiMap<String, Overload> thisSupplies,
                                        MultiMap<String, Overload> thisOverrides) {

        Overload mo = wrapInOverload(mc);

        if (mc.isOverride()) {
            thisOverrides.putItem(s, mo);
        }
        Applicable a = mc.getDef();
        if (a instanceof FnDecl) {
            thisSupplies.putItem(s, mo);
        } else {
            thisRequires.putItem(s, mo);
        }
    }

}
