/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.useful;

public class DualLattice<U> implements LatticeOps<U> {
    public boolean isForward() {
        return !original.isForward();
    }

    LatticeOps<U> original;

    public DualLattice(LatticeOps<U> original) {
        this.original = original;
    }

    public LatticeOps<U> dual() {
        return original;
    }

    public U join(U x, U y) {
        return original.meet(x, y);
    }

    public U meet(U x, U y) {
        return original.join(x, y);
    }

    public U one() {
        return original.zero();
    }

    public U zero() {
        return original.one();
    }

}
