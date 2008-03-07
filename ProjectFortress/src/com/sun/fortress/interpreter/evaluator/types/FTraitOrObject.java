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

package com.sun.fortress.interpreter.evaluator.types;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.interpreter.evaluator.BuildTraitEnvironment;
import com.sun.fortress.interpreter.evaluator.EvalType;
import com.sun.fortress.interpreter.evaluator.FortressError;
import com.sun.fortress.nodes.AbsDeclOrDecl;
import com.sun.fortress.nodes.AbstractNode;
import com.sun.fortress.nodes.InstantiatedType;
import com.sun.fortress.nodes.StaticArg;
import com.sun.fortress.nodes.StaticParam;
import com.sun.fortress.nodes.TraitAbsDeclOrDecl;
import com.sun.fortress.nodes.TraitObjectAbsDeclOrDecl;
import com.sun.fortress.nodes.TypeArg;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.useful.BoundingMap;
import com.sun.fortress.useful.EmptyLatticeIntervalError;
import com.sun.fortress.useful.HasAt;
import com.sun.fortress.useful.TopSort;
import com.sun.fortress.useful.TopSortItemImpl;
import com.sun.fortress.useful.Useful;

import static com.sun.fortress.interpreter.evaluator.ProgramError.errorMsg;
import static com.sun.fortress.interpreter.evaluator.ProgramError.error;
import static com.sun.fortress.interpreter.evaluator.InterpreterBug.bug;

abstract public class FTraitOrObject extends FTraitOrObjectOrGeneric {


    List<FType> extends_;
    HasAt at;
    volatile List<FType> properTransitiveExtends;
    // Must be volatile due to lazy initialization / double-checked locking.
    
    volatile BetterEnv suppliedMembers;
    volatile BetterEnv requiredMembers;

    abstract protected void finishInitializing();

    /**
     * Set extends, excludes, and replace the environment.
     *
     * @param extends_
     * @param excludes
     * @param replacementEnv
     */
    final public void setExtendsAndExcludes(List<FType> extends_, List<FType> excludes, BetterEnv replacementEnv) {
        env = replacementEnv;
        setExtendsAndExcludes(extends_, excludes);
    }

    final public void setExtendsAndExcludes(List<FType> extends_, List<FType> excludes) {
        if (this.extends_ != null)
            throw new IllegalStateException("Second set of extends");
        if (extends_.size()==0) {
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
        if (excludes != null)
            for (FType t : excludes)
                addExclude(t);
    }

    private void enforceValueness() {
        if (isValueType()) return;
        for (FType t : getExtends()) {
            if (t instanceof FTraitOrObjectOrGeneric && t.isValueType()) {
                error(at,
                      errorMsg(this, ": non-value subtype of value type ",t));
            }
        }
    }

    public List<FType> getExtends() {
        if (extends_ == null)
            bug(at, errorMsg(this, ": Get of unset extends"));
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
    @Override protected List<FType> getExtendsNull() {
        return extends_;
    }



    /** Only implemented by subtypes which extend GenericTypeInstance.
     *  Method included here to permit sharing of the complicated code in
     *  unifyNonVarGeneric.
     */
    protected FTypeGeneric getGeneric() {
        return bug(errorMsg("getGeneric() of non-Generic ",this));
    }

    /** Only implemented by subtypes which extend GenericTypeInstance.
     *  Method included here to permit sharing of the complicated code in
     *  unifyNonVarGeneric.
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


    public String toString() {
        // TODO need to carry generic parameters through, perhaps.
        if (this instanceof GenericTypeInstance) {
            GenericTypeInstance gti_this = (GenericTypeInstance) this;
            return getName() + Useful.listInOxfords(gti_this.getTypeParamsForName());
        } else {
            return getName();
        }
    }

    public FTraitOrObject(String name, BetterEnv env, HasAt at, List<? extends AbsDeclOrDecl> members, AbstractNode def) {
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
        for (TopSortItemImpl<FType> x : sorted) l.add(x.x);
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
                tsi.edgeTo(visitTrait((FType)e,unsorted, toItems));
            }
         }
         return tsi;
    }

    /**
     * An implementation of unifyNonVar specifically for types that
     * implement GenericTypeInstance.  They should override
     * unifyNonVar with a call to this method.
     */
    protected final boolean unifyNonVarGeneric(BetterEnv e, Set<StaticParam> tp_set, BoundingMap<String, FType, TypeLatticeOps> abm, Type val) {
        if (FType.DUMP_UNIFY)
            System.out.println("unify GT/O  "+this+" and "+val + " abm= " + abm);
        if (!(val instanceof InstantiatedType)) {
            return false;
        }
        InstantiatedType pt = (InstantiatedType) val;
        EvalType eval_type = new EvalType(env);
        FType eval_val_generic = eval_type.evalType(pt.getName());
        if (getGeneric() != eval_val_generic) {
            return false;
        }
        Iterator<StaticArg> val_args_iterator = pt.getArgs().iterator();
        Type a = null;
        FType t = null;
        try {
            for (FType param_ftype: getTypeParams()) {
                StaticArg targ = val_args_iterator.next();
                t = param_ftype;
                if (targ instanceof TypeArg) {
                    a = ((TypeArg)targ).getType();
                    param_ftype.unify(env, tp_set, abm,
                                      ((TypeArg)targ).getType());
                } else if (param_ftype instanceof FTypeNat) {
                    a = targ;
                    param_ftype.unify(env, tp_set, abm, targ);
                } else {
                    bug(val,e,
                        errorMsg("Can't handle unification of parameters ",
                                 this, " and ", val));
                }
            }
        } catch (FortressError p) {
            return false;
        } catch (EmptyLatticeIntervalError p) {
            if (DUMP_UNIFY)
                System.out.println("Interval went empty unifying " + t + " and " + a);
            return false;
        }
        return true;
    }
}
