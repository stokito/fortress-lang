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
import java.util.List;

import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.interpreter.evaluator.types.FTypeTop;
import com.sun.fortress.interpreter.useful.HasAt;
import com.sun.fortress.interpreter.useful.TopSort;
import com.sun.fortress.interpreter.useful.TopSortItemImpl;
import com.sun.fortress.interpreter.useful.Useful;
import com.sun.fortress.interpreter.evaluator.InterpreterError;
import com.sun.fortress.interpreter.evaluator.ProgramError;

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
            throw new InterpreterError(membersOf.getAt(),"Get of unset extends");
        // throw new IllegalStateException("Get of unset extends");
        return extends_;
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
}
