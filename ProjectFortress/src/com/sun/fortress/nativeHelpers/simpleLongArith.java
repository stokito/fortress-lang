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


    // Multiply non-negative longs only, returning -1 on overflow
    private static long longCautiousNonNegativeMul(long a, long b) {
	if (a < 2 || b < 2) return a * b;
	if (((int)a)==a && ((int)b)==b) return a * b;
	if ((Long.MAX_VALUE / a) < b) return -1;
        return a * b;
    }

    // Largest n for which `n CHOOSE k` is guaranteed to be representable as a long
    private final static int maxLongChooseN = 66;
    private final static long[] tempLongChooseArray = new long[maxLongChooseN/2];
    private final static long[] longChooseTable = new long[chooseAccess(maxLongChooseN + 1, 2)];

    // Entry k is the largest n for which `n CHOOSE (k+2)` can be computed by
    // the fast algorithm without overflow occurring at any intermediate step.
    private final static long[] maxLongChooseSafeN = { 3037000500L, 2642246, 86251, 11724, 3218, 1313, 684,
						       419, 287, 214, 169, 139, 119, 105, 95, 87, 81, 76, 73,
						       70, 68, 66, 64, 63, 62, 62, 61, 61, 61, 61, 61, 61 };
    // This index computation uses the 1-D array longChooseTable densely to hold a strangely
    // triangular 2-D array. It should be used only for 4 <= n <= maxLongChooseN and 2 <= k <= n/2.
    // The 1-D array holds elements for (n,k) = (4,2) (5,2) (6,2) (6,3) (7,2) (7,3) (8,2) (8,3) (8,4)
    // (9,2) (9,3) (9,4) (10,2) (10,3) (10,4) (10,5) (11,2) (11,3) (11,4) (11,5) and so on.
    // This represents (slightly over) one-half of Pascal's triangle with the outermost two
    // layers discarded.
    private static int chooseAccess(int n, int k) { return ((n-3)>>1)*((n-2)>>1) + (k-2); }

    static {
	for (int n = 4; n <= maxLongChooseN; n++) {
	    for (int k = 2; k <= n/2; k++) {
		// The table entries for row n-1 are already done, so they are available for computing row n.
		longChooseTable[chooseAccess(n, k)] = longOverflowingChoose(n-1, k-1) + longOverflowingChoose(n-1, k);
	    }
	}
    }

    // Computes `n CHOOSE k`, or -1 if that value is not representable as an long.
    public static long longSlowCautiousChoose(long n, long kk) {
	if (kk < 0 || kk > n) return 0;
        if (kk > (n >> 1)) kk = n - kk;
        if (kk < 2) return (kk == 0) ? 1 : n;
        if (n > maxLongChooseN && (kk << 1) > maxLongChooseN) return -1;
	int k = (int)kk;
	for (int j = 0; j < k; j++) tempLongChooseArray[j] = n - j;
	for (int j = k; j > 1; j--) {
	    // You can't just try to find an array element that j divides;
	    // you have to pick j apart into its prime factors.  Example: `40 CHOOSE 9`.
	    int q = j;
	    while (q > 1) {
		int pf = q;
		int newq = 1;
		// Find just one prime factor of q (if none found by this loop, q itself is prime).
		for (int p = 2; p * p <= q; p = ((p-1)|1)+2) {
		    // Note that p takes on the values 2, 3, 5, 7, 9, 11, 13, 15, ...
		    // (For maxLongChooseN/2=33, only the values 2, 3, 5 are needed.)
		    int s = q / p;
		    if (s*p == q) { pf = p; newq = s; break; }
		}
		// Now divide some element of the array by that prime factor
		found: {
		    for (int m = 0; m < k; m++) {
			long w = tempLongChooseArray[m] / pf;
			if (w * pf == tempLongChooseArray[m]) {
			    tempLongChooseArray[m] = w;
			    break found;
			}
		    }
		    System.out.println("n=" + n + ",k=" + k + ",j=" + j);
		    for (int z = 0; z < k; z++) System.out.println(tempLongChooseArray[z]);
		    throw new UnknownError("THIS SHOULDN'T HAPPEN in longSlowCautiousChoose");
		}
		q = newq;
	    }
	}
	long result = tempLongChooseArray[0];
	for (int j = 1; j < k; j++) {
	    result = longCautiousNonNegativeMul(result, tempLongChooseArray[j]);
	    if (result == -1) break;
	}
	return result;
    }

    // This is the one we want to use from Fortress code.  It tries to be fast.
    public static long longOverflowingChoose(long n, long kk) {
	if (kk < 0 || kk > n) return 0;
        if (kk > (n >> 1)) kk = n - kk;
        if (kk < 2) return (kk == 0) ? 1 : n;
	if (n <= maxLongChooseN) return longChooseTable[chooseAccess((int)n, (int)kk)];
	if (kk > maxLongChooseN)
	    throw Utility.makeFortressException("fortress.CompilerBuiltin$IntegerOverflow");
	int k = (int)kk;
	if (n > maxLongChooseSafeN[k-2]) {
	    long q = longSlowCautiousChoose(n, k);
	    if (q == -1) throw Utility.makeFortressException("fortress.CompilerBuiltin$IntegerOverflow");
	    return q;
	}
	// Fast algorithm (no overflow checking required)
	long r = n;
	for (int j = 1; j < k; j++) r = (r * (n - j)) / (j + 1);
	return r;
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
        return a >= b;
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

    public static long longLeftShiftByIntMod64(long a, int b) {
        return a << b;
    }

    public static long longRightShiftByIntMod64(long a, int b) {
        return a >> b;
    }

    public static long longLeftShiftByLongMod64(long a, long b) {
        return a << b;
    }

    public static long longRightShiftByLongMod64(long a, long b) {
        return a >> b;
    }

    // This version handles signed shift distances and checks for arithmetic overflow.
    public static long longShift(long a, int b) {
	if (b < 0) {
	    if (b < -63) return a >> 63;
	    else return a >> (-b);
        } else if (b > 63) {
	    if (a == 0) return 0;
	    else throw Utility.makeFortressException("fortress.CompilerBuiltin$IntegerOverflow");
        }
	else {
	    long result = a << b;
	    if ((result >> b) == a) return result;
	    else throw Utility.makeFortressException("fortress.CompilerBuiltin$IntegerOverflow");
	}
    }

}
