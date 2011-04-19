/*******************************************************************************
 Copyright 2009,2010, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.nativeHelpers;

public class simpleIntArith {

    public static String intToString(int x) {
        return Integer.toString(x);
    }

     public static int parseInt(String s) {
        return Integer.parseInt(s);
    }

    public static int intAdd(int a, int b) {
        return a + b;
    }

    public static int intSub(int a, int b) {
        return a - b;
    }

    public static int intMul(int a, int b) {
        return a * b;
    }

    public static int intDiv(int a, int b) {
        return a / b;
    }

    public static int intNeg(int a) {
        return -a;
    }

    public static boolean intLT(int a, int b) {
        return a < b;
    }

    public static boolean intLE(int a, int b) {
        return a <= b;
    }

    public static boolean intGT(int a, int b) {
        return a > b;
    }

    public static boolean intGE(int a, int b) {
        return a > b;
    }

    public static boolean intEQ(int a, int b) {
        return a == b;
    }

    public static long iMulL(int a, int b) {
        return (long) a * (long) b;
    }

    public static int intAbs(int a) {
        return Math.abs(a);
    }

    public static int longToInt(long l) {
        return (int)l;
    }

}
