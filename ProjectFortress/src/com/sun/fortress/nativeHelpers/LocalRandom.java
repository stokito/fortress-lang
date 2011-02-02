/*******************************************************************************
 Copyright 2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.nativeHelpers;
import jsr166y.ThreadLocalRandom;

/** A static wrapper to jsr166y.threadLocalRandom.  No way to re-seed
    or set seed; this is for "good enough" non-replayable random
    numbers, not for algorithms that require reproducability or
    high-quality randomness. */
public class LocalRandom {

    public static double localRandomDouble(double mag) {
        return ThreadLocalRandom.current().nextDouble(mag);
    }

    public static int localRandomInt(int least, int exclusiveMost) {
        return ThreadLocalRandom.current().nextInt(least, exclusiveMost);
    }
}
