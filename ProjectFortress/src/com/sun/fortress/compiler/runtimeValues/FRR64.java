/*******************************************************************************
    Copyright 2009,2011, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler.runtimeValues;

public final class FRR64 extends fortress.CompilerBuiltin.RR64.DefaultTraitMethods
        implements fortress.CompilerBuiltin.RR64 {
    final double val;

    private FRR64(double x) { val = x; }
    public String toString() { return String.valueOf(val); }
    public FJavaString asString() { return FJavaString.make(String.valueOf(val)); }
    public double getValue() {return val;}
    public static FRR64 make(double x) {return new FRR64(x);}

    @Override
    public RTTI getRTTI() { return RTTIc.ONLY; }
    
    public static class RTTIc extends fortress.CompilerBuiltin.RR64.RTTIc {
        private RTTIc() { super(FRR64.class); };
        public static final RTTI ONLY = new RTTIc();
    }
}
