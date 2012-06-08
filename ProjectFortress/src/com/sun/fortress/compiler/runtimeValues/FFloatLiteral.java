/*******************************************************************************
    Copyright 2011, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler.runtimeValues;

import com.sun.fortress.compiler.runtimeValues.FIntLiteral.RTTIc;

public final class FFloatLiteral extends fortress.CompilerBuiltin.FloatLiteral.DefaultTraitMethods
        implements fortress.CompilerBuiltin.FloatLiteral {

    private String val;

    private FFloatLiteral(String val) {
        this.val = val;
    }

    private FFloatLiteral(double val) {
        this(new Double(val).toString());
    }

    public static FFloatLiteral make(float x) {
        return new FFloatLiteral((double)x);
    }

    public static FFloatLiteral make(double x) {
        return new FFloatLiteral(x);
    }

    public static FFloatLiteral make(String s) {
        return new FFloatLiteral(s);
    }

    public String toString() {
        return val;
    }

    public FJavaString asString() {
        return null; /* replaced in generated code; necessary for primitive hierarchy */
    }

    public Error outOfRange(String t) {
        return new Error("Not in range for "+t+": "+this);
    }

    public FRR64 asRR64() {
        return FRR64.make(Double.valueOf(val));
    }


    public FRR32 asRR32() {
        return FRR32.make(Float.valueOf(val));
    }
    
    @Override
    public RTTI getRTTI() { return RTTIc.ONLY; }
    
    public static class RTTIc extends RTTI {
        private RTTIc() { super(FFloatLiteral.class); };
        public static final RTTI ONLY = new RTTIc();
    }
    
}
