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

public class simpleLongArith {

    public static String longToString(long x) {
        return Long.toString(x);
    }

     public static long parseLong(String s) {
        return Long.parseLong(s);
    }

    public static long longAdd(long a, long b) {
        return a + b;
    }

    public static long longSub(long a, long b) {
        return a - b;
    }

    public static long longMul(long a, long b) {
        return a * b;
    }

    public static long longDiv(long a, long b) {
        return a / b;
    }

    public static long longNeg(long a) {
        return -a;
    }

    public static boolean longLT(long a, long b) {
        return a < b;
    }

    public static boolean longLE(long a, long b) {
        return a <= b;
    }

    public static boolean longGT(long a, long b) {
        return a > b;
    }

    public static boolean longGE(long a, long b) {
        return a > b;
    }

    public static boolean longEQ(long a, long b) {
        return a == b;
    }

    public static long longAbs(long a) {
        return Math.abs(a);
    }

    public static long intToLong(int i) {
	return (long)i;
    }
}
