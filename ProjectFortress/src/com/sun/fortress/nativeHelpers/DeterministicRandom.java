/*******************************************************************************
 Copyright 2012, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.nativeHelpers;

import com.sun.fortress.runtimeSystem.FortressTaskRunner;
import com.sun.fortress.runtimeSystem.BaseTask;

import com.sun.fortress.useful.MagicNumbers;

public class DeterministicRandom {
    private static final int NUMBER_OF_MIXES = 4;

    private static double fromLong(long n) {
        /* Treat the top 53 bits as a mantissa for a double in [0,1).
         * Recall that IEEE FP is stored in an implicit-1 form;
         * therefore, shift 0's off the top and subtract from the
         * exponent until a 1 is seen.  At that point, convert
         * directly to double, dropping the (implicit) 1 in the
         * process. */
        int exp = 1022;
        while (n > 0) {
            // shift zero bit off the top and decrement exponent
            n = n << 1;
            exp = exp - 1;
        }
        if (exp == 1022 && n == 0) {
            return 0;
        } else {
            n = ((n << 1) >>> 12); // drop 1, shift into place.
            return Double.longBitsToDouble(((long) exp << 52) + n);
        }
    }
    
    // phi(z) = floor(z / sqrt(m)) + sqrt(m) * (z mod sqrt(m))
    // This calculation might lose some degree of accuracy
    private static long swapHighLow(long z) {
        double sqrtm = Math.sqrt((double) Long.MAX_VALUE);
        return (long) (Math.floor(z / sqrtm) + (sqrtm * (z % sqrtm)));
    }

    // f(z) = phi(2z^2 + z) mod m
    // This calculation might lose some degree of accuracy
    private static long mixBits(long compressed) {
        long z = compressed;
        for (int i = 0; i < NUMBER_OF_MIXES; i++) {
            z = swapHighLow(2 * (z ^ 2) + z) % Long.MAX_VALUE;
        }
        return z;
    }

    /* Returns a deterministic random number in the interval 
     * [0, exclusiveMax) */
    public static double deterministicRandomDouble(double exclusiveMax) {
        FortressTaskRunner ftr = (FortressTaskRunner) Thread.currentThread();
        long dotProduct = ftr.task().makeNewRandom();
        return fromLong(mixBits(dotProduct)) * exclusiveMax;
    }

    /* Returns a deterministic random integer in the interval
     * [inclusiveLeast, exclusiveMax) */
    public static int deterministicRandomInt(int inclusiveMin, int exclusiveMax) {
        int difference = exclusiveMax - inclusiveMin;
        double randomDouble = deterministicRandomDouble((double) difference);
        return ((int) Math.floor(randomDouble)) + inclusiveMin;
    }

}
