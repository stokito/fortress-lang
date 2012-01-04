/*******************************************************************************
 Copyright 2009,2010, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.nativeHelpers;

import com.sun.fortress.compiler.runtimeValues.FException;
import com.sun.fortress.compiler.runtimeValues.FZZ;
import com.sun.fortress.compiler.runtimeValues.Utility;
import java.math.BigInteger;

public class simpleArbitraryPrecisionArith {
	
	
    public static BigInteger parse(String s) {  
    	return new BigInteger(s);
    }
  
    public static String toString(BigInteger b) {  
    	return b.toString();
    }
    
    public static BigInteger add(BigInteger a, BigInteger b) {
    	return a.add(b);
    }

    public static BigInteger sub(BigInteger a, BigInteger b) {
    	return a.subtract(b);
    }

    public static BigInteger mul(BigInteger a, BigInteger b) {
    	return a.multiply(b);
    }

    public static BigInteger div(BigInteger a, BigInteger b) { 
    	if (b.equals(BigInteger.ZERO)) throw Utility.makeFortressException("fortress.CompilerBuiltin$DivisionByZero"); 
    	return a.divide(b);
    }

    public static BigInteger neg(BigInteger a) { 
    	return a.negate();
    }

    public static BigInteger abs(BigInteger a) {
    	return a.abs();
    }

    public static boolean lt(BigInteger a, BigInteger b) {
    	return (a.compareTo(b) == -1? true : false);
    }
	
    public static boolean le(BigInteger a, BigInteger b) {
    	int x = a.compareTo(b);
    	return (x == -1 || x == 0 ? true : false);
    }

    public static boolean gt(BigInteger a, BigInteger b) {
    	return (a.compareTo(b) == 1? true : false);
    }    

    public static boolean ge(BigInteger a, BigInteger b) {
    	int x = a.compareTo(b);
    	return (x == 1 || x == 0 ? true : false);
    }

    public static boolean eq(BigInteger a, BigInteger b) {
    	return (a.compareTo(b) == 0? true : false);
    }

    public static BigInteger not(BigInteger a) {
    	return a.not();
    }

    public static BigInteger and(BigInteger a, BigInteger b) {
    	return a.and(b);
    }

    public static BigInteger oor(BigInteger a, BigInteger b) {
    	return a.or(b);
    }

    public static BigInteger xor(BigInteger a, BigInteger b) {
    	return a.xor(b);
    }
	
    public static BigInteger ZZtoBI(BigInteger i) {
    	return i;
    }
    
    public static BigInteger BItoZZ(BigInteger i) {
    	return i;
    }
}
