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
        /** Treat the top 52 bits as a mantissa for a double in [1,2),
         ** then subtract 1.  Recall that IEEE FP is stored in an
         ** implicit-1 form.  Exponent of 1023 and significand zero equals 1.0
         **/
        return Double.longBitsToDouble((1023L << 52) | (n >>> 12)) - 1.0;
    }
    
    /** From the paper: phi(z) = floor(z / sqrt(m)) + sqrt(m) * (z mod sqrt(m)) 
     ** Since this calculation actually just flips the high and low bits of a long, 
     ** we can cut some corners. **/
    private static long swapHighLow(long z) {
        // double sqrtm = Math.sqrt((double) Long.MAX_VALUE);
        // return (long) (Math.floor(z / sqrtm) + (sqrtm * (z % sqrtm)));
        return (z >>> 32) | (z << 32);
    }

    /** From the paper:  f(z) = phi((2z^2 + z) mod m)
     ** The compiler will do the mod for us, so we can ignore it **/
    private static long mixBits(long compressed) {
        long z = compressed;
        for (int i = 0; i < NUMBER_OF_MIXES; i++) {
            // To do this effectively in Java: 2 * z * z + z
            z = swapHighLow(2 * z * z + z);
        }
        return z;
    }

    /** Returns a deterministic random number in the interval 
     ** [0, exclusiveMax) */
    public static double deterministicRandomDouble(double exclusiveMax) {
        FortressTaskRunner ftr = (FortressTaskRunner) Thread.currentThread();
        long dotProduct = ftr.task().makeNewRandom();
        return fromLong(mixBits(dotProduct)) * exclusiveMax;
    }

    /** Returns a deterministic random integer in the interval
     ** [inclusiveLeast, exclusiveMax) */
    public static int deterministicRandomInt(int inclusiveMin, int exclusiveMax) {
        long difference = (long) exclusiveMax - (long) inclusiveMin;
        double randomDouble = deterministicRandomDouble((double) difference);
        return ((int) Math.floor(randomDouble)) + inclusiveMin;
    }

}
