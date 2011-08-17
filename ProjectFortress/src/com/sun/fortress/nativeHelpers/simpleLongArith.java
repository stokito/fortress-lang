/*******************************************************************************
 Copyright 2010, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

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

    public static long longBitNot(long a) {
        return ~a;
    }

    public static long longBitAnd(long a, long b) {
        return a & b;
    }

    public static long longBitOr(long a, long b) {
        return a | b;
    }

    public static long longBitXor(long a, long b) {
        return a ^ b;
    }

}
