/*******************************************************************************
 Copyright 2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

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
