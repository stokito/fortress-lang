/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.useful;

public class LongLatticeOps implements LatticeOps<Long> {
    public boolean isForward() {
        return true;
    }

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
