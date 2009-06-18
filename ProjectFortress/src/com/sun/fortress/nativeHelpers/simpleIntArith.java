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

public class simpleIntArith {

    public static String intToString(int x) {
        return Integer.toString(x);
    }
    public static int parseInt(String s) { return Integer.parseInt(s); }

    public static int intAdd(int a, int b) { return a+b; }
    public static int intSub(int a, int b) { return a-b; }
    public static int intMul(int a, int b) { return a*b; }
    public static int intNeg(int a) { return -a; }
    public static boolean intLT(int a, int b) {return a < b;}
    public static boolean intLE(int a, int b) {return a <= b;}
    public static boolean intGT(int a, int b) {return a > b;}
    public static boolean intEQ(int a, int b) {return a == b;}
    public static long iMulL(int a, int b) { return (long) a * (long) b; }
}
