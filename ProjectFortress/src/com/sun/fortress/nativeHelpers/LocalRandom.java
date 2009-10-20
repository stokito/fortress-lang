/*******************************************************************************
 Copyright 2009 Sun Microsystems, Inc.,
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
