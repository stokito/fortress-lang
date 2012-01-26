/*******************************************************************************
    Copyright 2009,2011, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler.runtimeValues;

import java.math.BigInteger;

import com.sun.fortress.compiler.runtimeValues.FZZ32.RTTIc;
import com.sun.fortress.compiler.runtimeValues.FJavaString;

public final class FIntLiteral extends fortress.CompilerBuiltin.IntLiteral.DefaultTraitMethods
        implements fortress.CompilerBuiltin.IntLiteral {
    // Using a store-the-relevant-form-in-one-field trick
    // as that's supposedly better (and IntLiterals should
    // always be transient objects with minimal lifetime).

    static final long BOGUS = 0xdeadbeefcafebabeL;

    public final long smallerVal;
    public final String largerVal;

    private FIntLiteral(long smallerVal) {
        this.smallerVal = smallerVal;
        this.largerVal = null;
    }

    private FIntLiteral(String largerVal) {
        this.largerVal = largerVal;
        this.smallerVal = BOGUS;
    }

    public static FIntLiteral make(int x) {
        return new FIntLiteral((long)x);
    }

    public static FIntLiteral make(long x) {
        return new FIntLiteral(x);
    }

    public static FIntLiteral make(String s) {
        return new FIntLiteral(s);
    }

    public String toString() {
        if (largerVal != null) return largerVal;
        return Long.toString(smallerVal);
    }

    public FJavaString asString() {
	// This was slipping through, because code generator was failing.  For now, do something useful.
        // return null; /* replaced in generated code; necessary for primitive hierarchy */
	return FJavaString.make(this.toString());
    }

    public Error outOfRange(String t) {
        return new Error("Not in range for "+t+": "+this);
    }

    public FZZ32 asZZ32() {
        if (Integer.MIN_VALUE <= smallerVal && smallerVal <= Integer.MAX_VALUE) {
            return FZZ32.make((int)smallerVal);
        }
        throw outOfRange("ZZ32");
    }

    public FZZ64 asZZ64() {
        if (largerVal == null) return FZZ64.make(smallerVal);
        throw outOfRange("ZZ64");
    }

    // This is a cheap fix. Problem in codeGen.forIntLiteral.
    public FNN32 asNN32() {
        if (0 <= (smallerVal & 0x00000000FFFFFFFFL) && (smallerVal & 0x00000000FFFFFFFFL) <= 0x00000000FFFFFFFFL) 
    	    return FNN32.make((int)smallerVal);
    	throw outOfRange("NN32");
    }

    public FNN64 asNN64() {
        if (largerVal == null) 
    	    return FNN64.make((long)smallerVal);
    	throw outOfRange("NN64");
    }
    
    public FZZ asZZ() {
        return FZZ.make(this.toString());
    }

    public FRR64 asRR64() {
        if (largerVal == null) return FRR64.make((double)smallerVal);
        return FRR64.make(Double.valueOf(largerVal));
    }


    public FRR32 asRR32() {
        if (largerVal == null) return FRR32.make((float)smallerVal);
        return FRR32.make(Float.valueOf(largerVal));
    }
    
    @Override
    public RTTI getRTTI() { return RTTIc.ONLY; }
    
    public static class RTTIc extends RTTI {
        private RTTIc() { super(FIntLiteral.class); };
        public static final RTTI ONLY = new RTTIc();
    }
}
