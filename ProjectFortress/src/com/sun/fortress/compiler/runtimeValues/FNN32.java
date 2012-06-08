/*******************************************************************************
    Copyright 2009,2011, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler.runtimeValues;

public final class FNN32 extends fortress.CompilerBuiltin.NN32.DefaultTraitMethods
        implements fortress.CompilerBuiltin.NN32 {
    final int val;

    private FNN32(int x) { val = x; }
    public String toString() { return String.valueOf(val);}
    public FJavaString asString() { return FJavaString.make(String.valueOf(val));}
    public int getValue() {return val;}
    public static FNN32 make(int x) {
        return new FNN32(x);
    }
    public static FNN32 plus(FNN32 a, FNN32 b) {return make(a.getValue() + b.getValue());}
    
    @Override
    public RTTI getRTTI() { return RTTIc.ONLY; }
    
    public static class RTTIc extends fortress.CompilerBuiltin.NN32.RTTIc {
        private RTTIc() { super(FNN32.class); };
        public static final RTTI ONLY = new RTTIc();
    }
}