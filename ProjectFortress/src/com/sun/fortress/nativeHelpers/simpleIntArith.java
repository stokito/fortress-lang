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

    public static int longLowHalfToInt(long l) {
	return (int)l;
    }

    public static int longHighHalfToInt(long l) {
	return (int)(l >> 32);
    }

    public static int longMaskedToInt(long l, int m) {
	return ((int)l) & m;   // Masking means overflow cannot occur
    }

    // Multiply non-negative ints only, returning -1 on overflow
    private static int intCautiousNonNegativeMul(int a, int b) {
	long l = (long)a * (long)b;
	int c = (int)l;
	return (c == l) ? c : -1;
    }

    // Largest n for which `n CHOOSE k` is guaranteed to be representable as an int
    private final static int maxIntChooseN = 33;
    private final static int[] tempIntChooseArray = new int[maxIntChooseN/2];
    private final static int[] intChooseTable = new int[chooseAccess(maxIntChooseN + 1, 2)];

    // Entry k is the largest n for which `n CHOOSE (k+2)` can be computed by
    // the fast algorithm without overflow occurring at any intermediate step.
    private final static int[] maxIntChooseSafeN = { 46341, 1626, 338, 140, 82, 58, 46,
						  39, 35, 33, 31, 30, 30, 29, 29 };
    // This index computation uses the 1-D array intChooseTable densely to hold a strangely
    // triangular 2-D array. It should be used only for 4 <= n <= maxIntChooseN and 2 <= k <= n/2.
    // The 1-D array holds elements for (n,k) = (4,2) (5,2) (6,2) (6,3) (7,2) (7,3) (8,2) (8,3) (8,4)
    // (9,2) (9,3) (9,4) (10,2) (10,3) (10,4) (10,5) (11,2) (11,3) (11,4) (11,5) and so on.
    // This represents (slightly over) one-half of Pascal's triangle with the outermost two
    // layers discarded.
    private static int chooseAccess(int n, int k) { return ((n-3)>>1)*((n-2)>>1) + (k-2); }

    static {
	for (int n = 4; n <= maxIntChooseN; n++) {
	    for (int k = 2; k <= n/2; k++) {
		// The table entries for row n-1 are already done, so they are available for computing row n.
		intChooseTable[chooseAccess(n, k)] = intOverflowingChoose(n-1, k-1) + intOverflowingChoose(n-1, k);
	    }
	}
    }

//     static {
// 	System.out.println("Java triangle");
// 	for (int n = 0; n <= maxIntChooseN; n++) {
// 	    for (int k = 0; k < n; k++) {
// 		System.out.print(intSlowCautiousChoose(n, k));
// 		System.out.print(" ");
// 	    }
// 	    System.out.println(intSlowCautiousChoose(n, n));
// 	}
// 	System.out.println("End Java triangle");
//     }

    // Computes `n CHOOSE k`, or -1 if that value is not representable as an int.
    public static int intSlowCautiousChoose(int n, int k) {
	if (k < 0 || k > n) return 0;
        if (k > (n >> 1)) k = n - k;
        if (k < 2) return (k == 0) ? 1 : n;
        if (n > maxIntChooseN && (k << 1) > maxIntChooseN) return -1;
	for (int j = 0; j < k; j++) tempIntChooseArray[j] = n - j;
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
		    // (For maxLongChooseN/2=16, only the values 2 and 3 are needed.)
		    int s = q / p;
		    if (s*p == q) { pf = p; newq = s; break; }
		}
		// Now divide some element of the array by that prime factor
		found: {
		    for (int m = 0; m < k; m++) {
			int w = tempIntChooseArray[m] / pf;
			if (w * pf == tempIntChooseArray[m]) {
			    tempIntChooseArray[m] = w;
			    break found;
			}
		    }
		    System.out.println("n=" + n + ",k=" + k + ",j=" + j);
		    for (int z = 0; z < k; z++) System.out.println(tempIntChooseArray[z]);
		    throw new UnknownError("THIS SHOULDN'T HAPPEN in intSlowCautiousChoose");
		}
		q = newq;
	    }
	}
	int result = tempIntChooseArray[0];
	for (int j = 1; j < k; j++) {
	    result = intCautiousNonNegativeMul(result, tempIntChooseArray[j]);
	    if (result == -1) break;
	}
	return result;
    }

    // This is the one we want to use from Fortress code.  It tries to be fast.
    public static int intOverflowingChoose(int n, int k) {
       	// System.out.println("Entering " + n + " CHOOSE " + k + "; array length is " + intChooseTable.length);
	if (k < 0 || k > n) return 0;
        if (k > (n >> 1)) k = n - k;
        if (k < 2) return (k == 0) ? 1 : n;
	if (n <= maxIntChooseN) return intChooseTable[chooseAccess(n, k)];
	if (k > maxIntChooseN)
	    throw Utility.makeFortressException("fortress.CompilerBuiltin$IntegerOverflow");
	if (n > maxIntChooseSafeN[k-2]) {
	    int q = intSlowCautiousChoose(n, k);
	    if (q == -1) throw Utility.makeFortressException("fortress.CompilerBuiltin$IntegerOverflow");
	    return q;
	}
	// Fast algorithm (no overflow checking required)
	int r = n;
	for (int j = 1; j < k; j++) r = (r * (n - j)) / (j + 1);
	return r;
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
        return a >= b;
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

    public static int intLeftShiftByIntMod32(int a, int b) {
        return a << b;
    }

    public static int intRightShiftByIntMod32(int a, int b) {
        return a >> b;
    }

    public static int intLeftShiftByLongMod32(int a, long b) {
        return a << b;
    }

    public static int intRightShiftByLongMod32(int a, long b) {
        return a >> b;
    }

    // This version handles signed shift distances and checks for arithmetic overflow.
    public static int intShift(int a, int b) {
	if (b < 0) {
	    if (b < -31) return a >> 31;
	    else return a >> (-b);
        } else if (b > 31) {
	    if (a == 0) return 0;
	    else throw Utility.makeFortressException("fortress.CompilerBuiltin$IntegerOverflow");
        }
	else {
	    int result = a << b;
	    if ((result >> b) == a) return result;
	    else throw Utility.makeFortressException("fortress.CompilerBuiltin$IntegerOverflow");
	}
    }

    public static int intToIntPower(int a, int b) {
	if (b <= 0) {
	    if (b == 0 || a == 1) return 1;
	    else if (a < 0) throw Utility.makeFortressException("fortress.CompilerBuiltin$DivisionByZero");
	    else return 0;
	}
	long result = 1;
	long x = a;
	do {
	    if (x != (int) x) throw Utility.makeFortressException("fortress.CompilerBuiltin$IntegerOverflow");
	    if ((b & 1) != 0) {
		result *= x;
		if (result != (int) result) throw Utility.makeFortressException("fortress.CompilerBuiltin$IntegerOverflow");
	    }
	    b >>= 1;
	    x *= x;
	} while (b > 0);
	return (int) result;
    }

    public static int intExp(int a, int b) {
        double result = java.lang.Math.pow(a,b);
        if (result > Integer.MAX_VALUE)
            throw new RuntimeException("Overflow Error:");
        else
            return (int) result;
    }

    public static double intToDouble(int a) {
        return (double) a;
    }
    
}
