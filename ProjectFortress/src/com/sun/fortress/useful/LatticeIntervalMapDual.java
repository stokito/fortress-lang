/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.useful;

import java.util.Comparator;

public class LatticeIntervalMapDual<T, U, L extends LatticeOps<U>> extends LatticeIntervalMapBase<T, U, L>
        implements BoundingMap<T, U, L> {

    public LatticeIntervalMapDual(BATree2<T, U, U> table2,
                                  LatticeOps<U> lattice_operations,
                                  LatticeIntervalMap<T, U, L> supplied_dual) {
        super(table2, lattice_operations, supplied_dual);
    }

    public LatticeIntervalMapDual(BATree2<T, U, U> table2, LatticeOps<U> lattice_operations) {
        super(table2, lattice_operations, null);
    }

    public LatticeIntervalMapDual(LatticeOps<U> lattice_operations, Comparator<T> comparator) {
        super(new BATree2<T, U, U>(comparator), lattice_operations, null);
    }

    public LatticeIntervalMapDual<T, U, L> copy() {
        return new LatticeIntervalMapDual<T, U, L>(table.copy(), lattice);
    }

    public LatticeIntervalMapBase<T, U, L> dual() {
        if (dualMap == null) {
            synchronized (table) {
                if (dualMap == null) dualMap = new LatticeIntervalMap<T, U, L>(table, lattice.dual(), this);
            }
        }
        return dualMap;
    }

    protected U lower(BATree2Node<T, U, U> node) {
        return node.data2;
    }

    protected U upper(BATree2Node<T, U, U> node) {
        return node.data1;
    }

    protected void putPair(T k, U lower, U upper) {
        table.putPair(k, upper, lower);
    }

    /**
     * Used for backtracking during unification
     */
    public void assign(BoundingMap<T, U, L> replacement) {
        if (replacement instanceof LatticeIntervalMapDual) {
            LatticeIntervalMapDual<T, U, L> lim = (LatticeIntervalMapDual<T, U, L>) replacement;
            table = lim.table;
            dualMap = null;
        } else {
            throw new Error("Replacement must be LatticeIntervalMapDual");
        }
    }


}
