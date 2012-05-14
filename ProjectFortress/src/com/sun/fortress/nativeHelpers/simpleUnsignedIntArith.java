/*******************************************************************************
 Copyright 2009,2010, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.nativeHelpers;

import com.sun.fortress.compiler.runtimeValues.FException;
import com.sun.fortress.compiler.runtimeValues.Utility;

public class simpleUnsignedIntArith {

    public static long toLong(int i) {
	long l = i;
	l &= 0x00000000FFFFFFFFL;
	return l;
    }

    public static int toUnsignedIntOverflow(long l) {
	long t = l & 0xFFFFFFFF00000000L;
	if (t != 0) throw Utility.makeFortressException("fortress.CompilerBuiltin$IntegerOverflow");
	return (int) l; 
    }

    public static String unsignedIntToString(int x) {
        return Long.toString(toLong(x));
    }

    public static int parseUnsignedInt(String s) {  
        try { 
	    if (s.charAt(0) == '-') throw Utility.makeFortressException("fortress.CompilerBuiltin$IntegerOverflow");
	}
	catch (IndexOutOfBoundsException e) { 
	    throw Utility.makeFortressException("fortress.CompilerBuiltin$IntegerOverflow");
	}
	long c = Long.parseLong(s);
        return toUnsignedIntOverflow(c);
    }

    public static int unsignedIntOverflowingAdd(int a, int b) {
        long c = toLong(a) + toLong(b);
        return toUnsignedIntOverflow(c);
    }

    public static int unsignedIntOverflowingSub(int a, int b) {
	long c = toLong(a) - toLong(b);
	return toUnsignedIntOverflow(c);
    }

    public static int unsignedIntOverflowingMul(int a, int b) {
	long c = toLong(a) * toLong(b);
        return toUnsignedIntOverflow(c);
    }

    public static int unsignedIntOverflowingDiv(int a, int b) {
	if (b==0) throw Utility.makeFortressException("fortress.CompilerBuiltin$DivisionByZero");
	long c = toLong(a) / toLong(b);
        return (int) c; 
    }

    public static int unsignedIntOverflowingNeg(int a) {  
	if (a == 0) return 0;
	throw Utility.makeFortressException("fortress.CompilerBuiltin$IntegerOverflow");
    }

    public static int unsignedIntOverflowingAbs(int a) {
        return a;
    }

    public static int longOverflowingToUnsignedInt(long l) {
        return toUnsignedIntOverflow(l);
    }

    public static int unsignedIntOverflowingChoose(int n, int k) {
	return toUnsignedIntOverflow(simpleLongArith.longOverflowingChoose(toLong(n),toLong(k)));
    }

    public static int unsignedIntWrappingAdd(int a, int b) {
        return a + b;
    }

    public static int unsignedIntWrappingSub(int a, int b) {
        return a - b;
    }

    public static int unsignedIntWrappingMul(int a, int b) {
        return a * b;
    }

    public static int unsignedIntWrappingDiv(int a, int b) { 
	if (b==0) throw Utility.makeFortressException("fortress.CompilerBuiltin$DivisionByZero");
        return (int) (toLong(a) / toLong(b));
    }

    public static int unsignedIntWrappingNeg(int a) { 
        return -a;
    }

    public static int unsignedIntWrappingAbs(int a) {
        return a;
    }

    public static int longWrappingToUnsignedInt(long l) {
        return (int)l;
    }

    public static int unsignedIntSaturatingAdd(int a, int b) {
        long c = toLong(a) + toLong(b);
	if (c > 0x00000000FFFFFFFFL) return -1;
	return (int) c;
    }

    public static int unsignedIntSaturatingSub(int a, int b) {
	if (toLong(b) > toLong(a)) return 0;
        long c = toLong(a) - toLong(b);
        return (int) c;
    }

    public static int unsignedIntSaturatingMul(int a, int b) {
	long c = toLong(a) * toLong(b);
	if (c > 0x00000000FFFFFFFFL) return -1;
	return (int) c;
    }

    public static int unsignedIntSaturatingDiv(int a, int b) {
	if (b==0) throw Utility.makeFortressException("fortress.CompilerBuiltin$DivisionByZero");
        return (int) (toLong(a) / toLong(b)); 
    }

    public static int unsignedIntSaturatingNeg(int a) { 
        return 0;
    }

    public static int unsignedIntSaturatingAbs(int a) {
        return a;
    }

    public static int longSaturatingToUnsignedInt(long l) {
	int c = (int)l;
	if (c != l) return ((int)(l >> 63)) ^ Integer.MAX_VALUE;
        return c;
    }

    public static boolean unsignedIntLT(int a, int b) {
	return toLong(a) < toLong(b);
    }
	
    public static boolean unsignedIntLE(int a, int b) {
        return toLong(a) <= toLong(b);
    }

    public static boolean unsignedIntGT(int a, int b) {
        return toLong(a) > toLong(b);
    }    

    public static boolean unsignedIntGE(int a, int b) {
        return toLong(a) >= toLong(b);
    }

    public static boolean unsignedIntEQ(int a, int b) {
        return toLong(a) == toLong(b);
    }

    public static int unsignedIntBitNot(int a) {
        return ~a;
    }

    public static int unsignedIntBitAnd(int a, int b) {
        return a & b;
    }

    public static int unsignedIntBitOr(int a, int b) {
        return a | b;
    }

    public static int unsignedIntBitXor(int a, int b) {
        return a ^ b;
    }

    public static int unsignedIntExp(int a, int b) {  
        double result = java.lang.Math.pow(unsignedIntToDouble(a),unsignedIntToDouble(b));
        if (result > 0x00000000FFFFFFFFL)
	    throw Utility.makeFortressException("fortress.CompilerBuiltin$IntegerOverflow");
        else
            return (int) result;
    }

    public static double unsignedIntToDouble(int a) {
        return (double) toLong(a);
    }
    
    public static int makeNN32FromZZ32WithSpecialCompilerHackForNN32ResultType(int x) {return x;}
    public static int makeZZ32FromNN32WithSpecialCompilerHackForNN32ArgumentType(int x) {return x;}

}
