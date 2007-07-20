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

public class LongLatticeOps implements LatticeOps<Long> {

    public LatticeOps<Long> dual() {
        return new DualLattice<Long>(this);
    }

    public Long join(Long x, Long y) { /* up */
        return x.compareTo(y) < 0 ? y : x;
    }

    public Long meet(Long x, Long y) { /* down */
        return x.compareTo(y) > 0 ? y : x;
    }

    private final static Long max = Long.valueOf(Long.MAX_VALUE);
    private final static Long min = Long.valueOf(Long.MIN_VALUE);

    public Long one() {
        return max;
    }

    public Long zero() {
        return min;
    }

}
