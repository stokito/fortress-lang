/*******************************************************************************
 Copyright 2009,2010, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.nativeHelpers;

import com.sun.fortress.compiler.runtimeValues.FException;
import com.sun.fortress.compiler.runtimeValues.Utility;

public class simpleIntArith {

    public static String intToString(int x) {
        return Integer.toString(x);
    }

     public static int parseInt(String s) {
        return Integer.parseInt(s);
    }


    // The original "Java-like" arithmetic operators

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
	if (b==0) throw Utility.makeFortressException("fortress.CompilerBuiltin$DivisionByZero");
        return a / b;
    }

    public static int intNeg(int a) {
        return -a;
    }

    public static int intAbs(int a) {
        return Math.abs(a);
    }

    public static int longToInt(long l) {
        return (int)l;
    }


    // Arithmetic operators with overflow checking

    public static int intOverflowingAdd(int a, int b) {
        int c = a + b;
	if (((c ^ a) & (c ^ b)) < 0) throw Utility.makeFortressException("fortress.CompilerBuiltin$IntegerOverflow");
        return c;
    }

    public static int intOverflowingSub(int a, int b) {
        int c = a - b;
	if (((a ^ b) & (a ^ c)) < 0) throw Utility.makeFortressException("fortress.CompilerBuiltin$IntegerOverflow");
        return c;
    }

    public static int intOverflowingMul(int a, int b) {
        return longOverflowingToInt((long)a * (long)b);
    }

    public static int intOverflowingDiv(int a, int b) {
	if (b==0) throw Utility.makeFortressException("fortress.CompilerBuiltin$DivisionByZero");
	if (b==(-1) && a==(-a)) throw Utility.makeFortressException("fortress.CompilerBuiltin$IntegerOverflow");
        return a / b;
    }

    public static int intOverflowingNeg(int a) {
	if (a==(-a)) throw Utility.makeFortressException("fortress.CompilerBuiltin$IntegerOverflow");
        return -a;
    }

    public static int intOverflowingAbs(int a) {
	if (a==(-a)) throw Utility.makeFortressException("fortress.CompilerBuiltin$IntegerOverflow");
        return Math.abs(a);
    }

    public static int longOverflowingToInt(long l) {
	int c = (int)l;
	if (c != l) throw Utility.makeFortressException("fortress.CompilerBuiltin$IntegerOverflow");
        return c;
    }


    // Arithmetic operators with wrapping

    public static int intWrappingAdd(int a, int b) {
        return a + b;
    }

    public static int intWrappingSub(int a, int b) {
        return a - b;
    }

    public static int intWrappingMul(int a, int b) {
        return a * b;
    }

    public static int intWrappingDiv(int a, int b) {
	if (b==0) throw Utility.makeFortressException("fortress.CompilerBuiltin$DivisionByZero");
        return a / b;
    }

    public static int intWrappingNeg(int a) {
        return -a;
    }

    public static int intWrappingAbs(int a) {
        return Math.abs(a);
    }

    public static int longWrappingToInt(long l) {
        return (int)l;
    }


    // Arithmetic operators with saturation.

    public static int intSaturatingAdd(int a, int b) {
        int c = a + b;
	if (((c ^ a) & (c ^ b)) < 0) return (c >> 31) ^ Integer.MIN_VALUE;
	return c;
    }

    public static int intSaturatingSub(int a, int b) {
        int c = a - b;
	if (((a ^ b) & (a ^ c)) < 0) return (a >> 31) ^ Integer.MAX_VALUE;
        return c;
    }

    public static int intSaturatingMul(int a, int b) {
        return longSaturatingToInt((long)a * (long)b);
    }

    public static int intSaturatingDiv(int a, int b) {
	if (b==0) throw Utility.makeFortressException("fortress.CompilerBuiltin$DivisionByZero");
	if (b==(-1) && a==(-a)) return Integer.MAX_VALUE;
        return a / b;
    }

    public static int intSaturatingNeg(int a) {
	if (a==(-a)) return Integer.MAX_VALUE;
        return -a;
    }

    public static int intSaturatingAbs(int a) {
	if (a==(-a)) return Integer.MAX_VALUE;
        return Math.abs(a);
    }

    public static int longSaturatingToInt(long l) {
	int c = (int)l;
	if (c != l) return ((int)(l >> 63)) ^ Integer.MAX_VALUE;
        return c;
    }


    // Operations whose results are always representable

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

    public static int intBitNot(int a) {
        return ~a;
    }

    public static int intBitAnd(int a, int b) {
        return a & b;
    }

    public static int intBitOr(int a, int b) {
        return a | b;
    }

    public static int intBitXor(int a, int b) {
        return a ^ b;
    }
    
}
