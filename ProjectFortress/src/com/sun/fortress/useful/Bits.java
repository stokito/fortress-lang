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

package com.sun.fortress.useful;

public class Bits {
    public static int ceilLogTwo(int y) {
        int x = -y;
        if (x > 0) {
            return 32;
        }
        int l = 0;

        while (-1 << l > x) {
            l++;
        }
        return l;

    }

    public static long mask(int n) {
        if (n <= 0 || n > 64) throw new IllegalArgumentException("Expected input between 0 and 64, got " + n);
        return (-1L) >>> (64 - n);
    }
}
