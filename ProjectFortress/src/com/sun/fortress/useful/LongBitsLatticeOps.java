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

public class LongBitsLatticeOps implements LatticeOps<Long> {

    public LatticeOps<Long> dual() {
        return VD;
    }

    public Long join(Long x, Long y) { /* up */
        long bx = x.longValue();
        long by = y.longValue();
        long b = bx | by;
        return b == bx ? x : b == by ? y : Long.valueOf(b);
    }

    public Long meet(Long x, Long y) { /* down */
        long bx = x.longValue();
        long by = y.longValue();
        long b = bx & by;
        return b == bx ? x : b == by ? y : Long.valueOf(b);
    }

    private final static Long max = Long.valueOf(-1L);
    private final static Long min = Long.valueOf(0L);

    public Long one() {
        return max;
    }

    public Long zero() {
        return min;
    }

    public static final LongBitsLatticeOps V = new LongBitsLatticeOps();
    public static final LatticeOps<Long> VD = new DualLattice<Long>(V);

}
