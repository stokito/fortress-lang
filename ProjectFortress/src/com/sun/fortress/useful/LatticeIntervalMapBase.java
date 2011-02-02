/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/
package com.sun.fortress.useful;

import java.util.AbstractMap;
import java.util.Set;

public abstract class LatticeIntervalMapBase<T, U, L extends LatticeOps<U>> extends AbstractMap<T, U>
        implements BoundingMap<T, U, L> {

    BATree2<T, U, U> table;
    LatticeOps<U> lattice;
    protected volatile LatticeIntervalMapBase<T, U, L> dualMap;
    // Must be volatile due to lazy initialization / double-checked locking.

    public boolean isForward() {
        return lattice.isForward();
    }

    public LatticeIntervalMapBase(BATree2<T, U, U> table2,
                                  LatticeOps<U> lattice_operations,
                                  LatticeIntervalMapBase<T, U, L> supplied_dual) {
        table = table2;
        lattice = lattice_operations;
        dualMap = supplied_dual;
    }

    public boolean leq(U lower, U upper) {
        U lmu = lattice.meet(lower, upper);
        return lmu.equals(lower);
    }

    abstract protected U lower(BATree2Node<T, U, U> node);

    abstract protected U upper(BATree2Node<T, U, U> node);

    abstract protected void putPair(T k, U lower, U upper);

    /**
     * puts min/intersection of v and old.
     * returns the new (potentially lower) upper bound.
     *
     * @throws EmptyLatticeIntervalError
     */
    public U meetPut(T k, U v) {
        BATree2Node<T, U, U> old = table.getNode(k);
        if (old != null) {
            U lower = lower(old);
            U upper = lattice.meet(upper(old), v);
            checkOrdered(lower, upper);
            putPair(k, lower, upper);
            return upper;
        } else {
            putPair(k, lattice.zero(), v);
            return v;
        }
    }

    /**
     * @param lower
     * @param upper
     * @throws EmptyLatticeIntervalError
     */
    private void checkOrdered(U lower, U upper) throws Error {
        if (!leq(lower, upper)) throw new EmptyLatticeIntervalError();
    }

    /**
     * puts max/union of v and old.
     * returns the new (potentially higher) lower bound.
     *
     * @throws EmptyLatticeIntervalError
     */
    public U joinPut(T k, U v) {
        BATree2Node<T, U, U> old = table.getNode(k);
        if (old != null) {
            U lower = lattice.join(lower(old), v);
            U upper = upper(old);
            checkOrdered(lower, upper);
            putPair(k, lower, upper);
            return lower;
        } else {
            putPair(k, v, lattice.one());
            return v;
        }
    }

    /**
     * Puts the lower (bottom) end of an interval.
     * Returns null if the interval was previously missing,
     * otherwise returns the old lower bound.
     * This has the same effect on the map as joinPut, but
     * returns a different result.
     */
    public U put(T k, U v) {
        BATree2Node<T, U, U> old = table.getNode(k);
        if (old != null) {
            U lower = lattice.join(lower(old), v);
            U upper = upper(old);
            checkOrdered(lower, upper);
            putPair(k, lower, upper);
            return lower(old);
        } else {
            putPair(k, v, lattice.one());
            return null;
        }
    }

    /**
     * Returns the lower (bottom) end of an interval.
     */
    @SuppressWarnings ("unchecked")
    public U get(Object k) {
        BATree2Node<T, U, U> old = table.getNode((T) k);
        return old == null ? null : lower(old);
    }

    /**
     * Returns the lower (bottom) end of an interval.
     */
    public U getLower(T k) {
        BATree2Node<T, U, U> old = table.getNode(k);
        return old == null ? null : lower(old);
    }

    /**
     * Returns the upper (top) end of an interval.
     */
    public U getUpper(T k) {
        BATree2Node<T, U, U> old = table.getNode(k);
        return old == null ? null : upper(old);
    }

    @Override
    public Set<java.util.Map.Entry<T, U>> entrySet() {
        throw new Error("unimplemented");
    }

    public String toString() {
        return table.toString();
    }

    /**
     * Used for backtracking during unification
     */
    abstract public void assign(BoundingMap<T, U, L> replacement);


}
