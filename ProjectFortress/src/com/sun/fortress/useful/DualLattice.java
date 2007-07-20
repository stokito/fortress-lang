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

package com.sun.fortress.useful;

public class DualLattice<U> implements LatticeOps<U> {

    LatticeOps<U> original;

    public DualLattice(LatticeOps<U> original) {
        this.original = original;
    }

    public LatticeOps<U> dual() {
        return original;
    }

    public U join(U x, U y) {
        return original.meet(x,y);
    }

    public U meet(U x, U y) {
        return original.join(x,y);
    }

    public U one() {
        return original.zero();
    }

    public U zero() {
        return original.one();
    }

}
