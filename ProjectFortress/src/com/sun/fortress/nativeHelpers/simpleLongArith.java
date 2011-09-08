/*******************************************************************************
 Copyright 2010, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.nativeHelpers;

import com.sun.fortress.compiler.runtimeValues.FException;
import com.sun.fortress.compiler.runtimeValues.Utility;

public class simpleLongArith {

    public static String longToString(long x) {
        return Long.toString(x);
    }

     public static long parseLong(String s) {
        return Long.parseLong(s);
    }


    // The original "Java-like" arithmetic operators

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
	if (b==0) throw Utility.makeFortressException("fortress.CompilerBuiltin$DivisionByZero");
        return a / b;
    }

    public static long longNeg(long a) {
        return -a;
    }

    public static long longAbs(long a) {
        return Math.abs(a);
    }


    // Arithmetic operators with overflow checking

    public static long longOverflowingAdd(long a, long b) {
        long c = a + b;
	if (((c ^ a) & (c ^ b)) < 0) throw Utility.makeFortressException("fortress.CompilerBuiltin$IntegerOverflow");
        return c;
    }

    public static long longOverflowingSub(long a, long b) {
        long c = a - b;
	if (((a ^ b) & (a ^ c)) < 0) throw Utility.makeFortressException("fortress.CompilerBuiltin$IntegerOverflow");
        return c;
    }

    public static long longOverflowingMul(long a, long b) {
	if (a==(-a)) {
	    if ((b >> 1) != 0)
		throw Utility.makeFortressException("fortress.CompilerBuiltin$IntegerOverflow");
	    return a * b;
	}
	if (b==(-b)) {
	    if ((a >> 1) != 0)
		throw Utility.makeFortressException("fortress.CompilerBuiltin$IntegerOverflow");
	    return a * b;
	}
	if (((int)a)==a && ((int)b)==b) return a * b;
	if ((Long.MAX_VALUE / Math.abs(a)) < Math.abs(b))
	    throw Utility.makeFortressException("fortress.CompilerBuiltin$IntegerOverflow");
        return a * b;
    }

    public static long longOverflowingDiv(long a, long b) {
	if (b==0) throw Utility.makeFortressException("fortress.CompilerBuiltin$DivisionByZero");
	if (b==(-1) && a==(-a)) throw Utility.makeFortressException("fortress.CompilerBuiltin$IntegerOverflow");
        return a / b;
    }

    public static long longOverflowingNeg(long a) {
	if (a==(-a)) throw Utility.makeFortressException("fortress.CompilerBuiltin$IntegerOverflow");
        return -a;
    }

    public static long longOverflowingAbs(long a) {
	if (a==(-a)) throw Utility.makeFortressException("fortress.CompilerBuiltin$IntegerOverflow");
        return Math.abs(a);
    }


    // Arithmetic operators with wrapping

    public static long longWrappingAdd(long a, long b) {
        return a + b;
    }

    public static long longWrappingSub(long a, long b) {
        return a - b;
    }

    public static long longWrappingMul(long a, long b) {
        return a * b;
    }

    public static long longWrappingDiv(long a, long b) {
	if (b==0) throw Utility.makeFortressException("fortress.CompilerBuiltin$DivisionByZero");
        return a / b;
    }

    public static long longWrappingNeg(long a) {
        return -a;
    }

    public static long longWrappingAbs(long a) {
        return Math.abs(a);
    }


    // Arithmetic operators with wrapping

    public static long longSaturatingAdd(long a, long b) {
        long c = a + b;
	if (((c ^ a) & (c ^ b)) < 0) return (c >> 63) ^ Long.MIN_VALUE;
        return c;
    }

    public static long longSaturatingSub(long a, long b) {
        long c = a - b;
	if (((a ^ b) & (a ^ c)) < 0) return (a >> 63) ^ Long.MAX_VALUE;
        return c;
    }

    public static long longSaturatingMul(long a, long b) {
	if (a==(-a)) {
	    if ((b >> 1) != 0) return (b >> 63) ^ Long.MIN_VALUE; 
	    return a * b;
	}
	if (b==(-b)) {
	    if ((a >> 1) != 0) return (a >> 63) ^ Long.MIN_VALUE; 
	    return a * b;
	}
	if (((int)a)==a && ((int)b)==b) return a * b;
	if ((Long.MAX_VALUE / Math.abs(a)) < Math.abs(b))
	    return ((a^b) >> 63) ^ Long.MAX_VALUE; 
        return a * b;
    }

    public static long longSaturatingDiv(long a, long b) {
	if (b==0) throw Utility.makeFortressException("fortress.CompilerBuiltin$DivisionByZero");
	if (b==(-1) && a==(-a)) return Long.MAX_VALUE;
        return a / b;
    }

    public static long longSaturatingNeg(long a) {
	if (a==(-a)) return Long.MAX_VALUE;
        return -a;
    }

    public static long longSaturatingAbs(long a) {
	if (a==(-a)) return Long.MAX_VALUE;
        return Math.abs(a);
    }


    // Operations whose results are always representable

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
