/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.useful;

public class LongBitsLatticeOps implements LatticeOps<Long> {
    public boolean isForward() {
        return true;
    }

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
