/*******************************************************************************
 Copyright 2010 Sun Microsystems, Inc.,
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

public class simpleDoubleArith {

    public static String doubleToString(double x) {
        return Double.toString(x);
    }

     public static double parseDouble(String s) {
        return Double.parseDouble(s);
    }

    public static double doubleAdd(double a, double b) {
        return a + b;
    }

    public static double doubleSub(double a, double b) {
        return a - b;
    }

    public static double doubleMul(double a, double b) {
        return a * b;
    }

    public static double doubleDiv(double a, double b) {
        return a / b;
    }

    public static double doubleNeg(double a) {
        return -a;
    }

    public static boolean doubleLT(double a, double b) {
        return a < b;
    }

    public static boolean doubleLE(double a, double b) {
        return a <= b;
    }

    public static boolean doubleGT(double a, double b) {
        return a > b;
    }

    public static boolean doubleGE(double a, double b) {
        return a > b;
    }

    public static boolean doubleEQ(double a, double b) {
        return a == b;
    }

    public static double doubleAbs(double a) {
        return Math.abs(a);
    }

    public static double doublePow(double a, double b) {
        return Math.pow(a,b);
    }

    public static double doubleNanoTime() {
        return (double)System.nanoTime();
    }

    public static double floatToDouble(float f) {
return (double)f;
    }

}
