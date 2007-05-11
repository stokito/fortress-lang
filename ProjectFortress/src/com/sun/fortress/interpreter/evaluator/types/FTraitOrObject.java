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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.interpreter.evaluator.EvalType;
import com.sun.fortress.interpreter.evaluator.ProgramError;
import com.sun.fortress.interpreter.evaluator.InterpreterError;
import com.sun.fortress.interpreter.nodes.StaticParam;
import com.sun.fortress.interpreter.nodes.ParamType;
import com.sun.fortress.interpreter.nodes.StaticArg;
import com.sun.fortress.interpreter.nodes.StaticParam;
import com.sun.fortress.interpreter.nodes.TypeArg;
import com.sun.fortress.interpreter.nodes.TypeRef;
import com.sun.fortress.interpreter.useful.ABoundingMap;
import com.sun.fortress.interpreter.useful.HasAt;
import com.sun.fortress.interpreter.useful.TopSort;
import com.sun.fortress.interpreter.useful.TopSortItemImpl;
import com.sun.fortress.interpreter.useful.Useful;

abstract public class FTraitOrObject extends FType {
    List<FType> extends_;
    BetterEnv env;
    volatile List<FType> properTransitiveExtends;
    // Must be volatile due to lazy initialization / double-checked locking.
    BetterEnv membersOf;

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
            extends_ = FTypeTop.T.getTransitiveExtends();
        }
        this.extends_ = extends_;
        // Here we need to add things to where clauses

        env.bless();
        membersOf.bless();
        initializeExcludes(excludes);
        finishInitializing();
    }

    private void initializeExcludes(List<FType> excludes) {
        if (excludes != null)
            for (FType t : excludes)
                addExclude(t);
    }

    public List<FType> getExtends() {
        if (extends_ == null)
            throw new InterpreterError(membersOf.getAt(),
                                       this+": Get of unset extends");
        // throw new IllegalStateException("Get of unset extends");
        return extends_;
    }

    /** Only implemented by subtypes which extend GenericTypeInstance.
     *  Method included here to permit sharing of the complicated code in
     *  unifyNonVarGeneric.
     */
    protected FTypeGeneric getGeneric() {
        throw new InterpreterError("getGeneric() of non-Generic "+this);
    }

    /** Only implemented by subtypes which extend GenericTypeInstance.
     *  Method included here to permit sharing of the complicated code in
     *  unifyNonVarGeneric.
     */
    protected List<FType> getTypeParams() {
        throw new InterpreterError("getTypeParams() of non-Generic "+this);
    }

    protected List<FType> computeTransitiveExtends() {
        return traitsSortedBySpecificity();
    }

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
            return getName() + Useful.listInOxfords(gti_this.getTypeParams());
        } else {
            return getName();
        }
    }

    public FTraitOrObject(String name, BetterEnv env, HasAt at) {
        super(name);
        this.env = env;
        this.membersOf = new BetterEnv(at);
    }

    /**
     *
     * @return Environment for the interior of this trait or object.
     */
    public BetterEnv getEnv() {
        return env;
    }

    public BetterEnv getMembers() {
        return membersOf;
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
    protected final boolean unifyNonVarGeneric(BetterEnv e, Set<StaticParam> tp_set, ABoundingMap<String, FType, TypeLatticeOps> abm, TypeRef val) {
        if (FType.DUMP_UNIFY)
            System.out.println("unify GT/O  "+this+" and "+val);
        if (!(val instanceof ParamType)) {
            return false;
        }
        ParamType pt = (ParamType) val;
        EvalType eval_type = new EvalType(env);
        FType eval_val_generic = eval_type.evalType(pt.getGeneric());
        if (getGeneric() != eval_val_generic) {
            return false;
        }
        Iterator<StaticArg> val_args_iterator = pt.getArgs().iterator();
        try {
            for (FType param_ftype: getTypeParams()) {
                StaticArg targ = val_args_iterator.next();
                if (targ instanceof TypeArg) {
                    param_ftype.unify(env, tp_set, abm,
                                      ((TypeArg)targ).getType());
                } else if (param_ftype instanceof FTypeNat) {
                    param_ftype.unify(env, tp_set, abm, targ);
                } else {
                    throw new InterpreterError(val,e,"Can't handle unification of parameters "+this+" and "+val);
                }
            }
        } catch (ProgramError p) {
            return false;
        }
        return true;
    }
}
