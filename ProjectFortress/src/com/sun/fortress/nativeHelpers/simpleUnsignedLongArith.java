/*******************************************************************************
 Copyright 2009,2010, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.nativeHelpers;

import com.sun.fortress.compiler.runtimeValues.FException;
import com.sun.fortress.compiler.runtimeValues.Utility;

public class simpleUnsignedLongArith {
	
    public static long parseUnsignedLong(String s) {  
    	return 0;
    }

    public static String unsignedLongToString(long x) {
        return Long.toString(x);
    }
    
    public static boolean ltu(long a, long b) {
    	return ((((~a & b) | ((~a | b) & (a - b))) >>> 63) == 1);
    }
	
    public static boolean leu(long a, long b) {
    	return ((((~a | b) & ((a ^ b) | ~(b - a))) >>> 63) == 1);
    }

    public static boolean gtu(long b, long a) {
    	return ((((~a & b) | ((~a | b) & (a - b))) >>> 63) == 1);
    }    

    public static boolean geu(long b, long a) {
    	return ((((~a | b) & ((a ^ b) | ~(b - a))) >>> 63) == 1);
    }
    
    public static long divu(long n, long d) {
    	if (d < 0) {
    		if (ltu(n,d)) return 0;
    		else return 1;
    	}
    	else {
    		long q = ((n >>> 1) / d) << 1;
    		long r = n - (q * d);
    		if (geu(r,d)) q = q + 1; 
    		return q;
    	}
    }
    
    public static long unsignedLongOverflowingAdd(long a, long b) {
    	long r = a + b;
    	if (ltu(r,a)) throw Utility.makeFortressException("fortress.CompilerBuiltin$IntegerOverflow");
    	return r;
    }

    public static long unsignedLongOverflowingSub(long a, long b) {
    	long r = a - b;
    	if (gtu(r,a)) throw Utility.makeFortressException("fortress.CompilerBuiltin$IntegerOverflow");
    	return r;
    }

    public static long unsignedLongOverflowingMul(long a, long b) {
    	long r = a * b;
    	if (b == 0) return r; 
    	if (divu(r,b) != a) throw Utility.makeFortressException("fortress.CompilerBuiltin$IntegerOverflow");
    	return r;
    }

    public static long unsignedLongOverflowingDiv(long n, long d) {
    	if (d==0) throw Utility.makeFortressException("fortress.CompilerBuiltin$DivisionByZero");
    	return divu(n,d);
    }

    public static long unsignedLongOverflowingNeg(long a) {  
    	if (a == 0) return 0;
    	throw Utility.makeFortressException("fortress.CompilerBuiltin$IntegerOverflow");
    }

    public static long unsignedLongOverflowingAbs(long a) {
    	return a;
    }

    public static long unsignedLongWrappingAdd(long a, long b) {
    	return a + b;
    }

    public static long unsignedLongWrappingSub(long a, long b) {
    	return a - b;
    }

    public static long unsignedLongWrappingMul(long a, long b) {
    	return a * b;
    }

    public static long unsignedLongWrappingDiv(long n, long d) { 
    	if (d==0) throw Utility.makeFortressException("fortress.CompilerBuiltin$DivisionByZero");
    	return divu(n,d);
    }

    public static long unsignedLongWrappingNeg(long a) { 
    	return a;
    }

    public static long unsignedLongWrappingAbs(long a) {
    	return a;
    }

    public static int longWrappingToUnsignedInt(long l) {
    	return (int) l;
    }

    public static long unsignedLongSaturatingAdd(long a, long b) {
    	long r = a + b;
    	if (ltu(r,a)) return -1;
    	return r;
    }

    public static long unsignedLongSaturatingSub(long a, long b) {
    	long r = a - b;
    	if (gtu(r,a)) return 0;
    	return r;
    }

    public static long unsignedLongSaturatingMul(long a, long b) {
    	long r = a * b;
    	long q = divu(r,b);
    	if (q != a) return -1;
    	return r;
    }

    public static long unsignedLongSaturatingDiv(long n, long d) {
    	if (d==0) throw Utility.makeFortressException("fortress.CompilerBuiltin$DivisionByZero");
    	return divu(n,d);
    }

    public static long unsignedLongSaturatingNeg(long a) {  
    	return 0;
    }

    public static long unsignedLongSaturatingAbs(long a) {
    	return a;
    }

    public static int longSaturatingToUnsignedInt(long l) {
    	return (int) l;
    }

    public static boolean unsignedLongLT(long a, long b) {
    	return ltu(a,b);
    }
	
    public static boolean unsignedLongLE(long a, long b) {
    	return leu(a,b);
    }

    public static boolean unsignedLongGT(long a, long b) {
    	return gtu(a,b);
    }    

    public static boolean unsignedLongGE(long a, long b) {
    	return geu(a,b);
    }

    public static boolean unsignedLongEQ(long a, long b) {
    	return a == b;
    }

    public static long unsignedLongBitNot(long a) {
    	return ~a;
    }

    public static long unsignedLongBitAnd(long a, long b) {
    	return a & b;
    }

    public static long unsignedLongBitOr(long a, long b) {
    	return a | b;
    }

    public static long unsignedLongBitXor(long a, long b) {
    	return a ^ b;
    }

    public static long unsignedLongLeftShiftByIntMod64(long a, int b) {
        return a << b;
    }

    public static long unsignedLongRightShiftByIntMod64(long a, int b) {
        return a >>> b;
    }

    public static long unsignedLongLeftShiftByLongMod64(long a, long b) {
        return a << b;
    }

    public static long unsignedLongRightShiftByLongMod64(long a, long b) {
        return a >>> b;
    }

    // This version handles signed shift distances and checks for arithmetic overflow.
    public static long unsignedLongShift(long a, int b) {
	if (b < 0) {
	    if (b < -63) return 0;
	    else return a >>> (-b);
        } else if (b > 63) {
	    if (a == 0) return 0;
	    else throw Utility.makeFortressException("fortress.CompilerBuiltin$IntegerOverflow");
        }
	else {
	    long result = a << b;
	    if ((result >>> b) == a) return result;
	    else throw Utility.makeFortressException("fortress.CompilerBuiltin$IntegerOverflow");
	}
    }

    // NIY
    public static long unsignedLongExp(long a, long b) {  
    	return a + b;
    }
	
    public static long makeNN64FromZZ64WithSpecialCompilerHackForNN64ResultType(long x) {return x;}
    public static long makeZZ64FromNN64WithSpecialCompilerHackForNN64ArgumentType(long x) {return x;}
    
}