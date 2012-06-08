/*******************************************************************************
    Copyright 2009,2011, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler.runtimeValues;

public final class FZZ32 extends fortress.CompilerBuiltin.ZZ32.DefaultTraitMethods
        implements fortress.CompilerBuiltin.ZZ32 {
    final int val;

    private FZZ32(int x) { val = x; }
    public String toString() { return String.valueOf(val);}
    public FJavaString asString() { return FJavaString.make(String.valueOf(val));}
    public int getValue() {return val;}
    public static FZZ32 make(int x) {
        return new FZZ32(x);
    }
    public static FZZ32 plus(FZZ32 a, FZZ32 b) {return make(a.getValue() + b.getValue());}
    
    @Override
    public RTTI getRTTI() { return RTTIc.ONLY; }
    
    public static class RTTIc extends fortress.CompilerBuiltin.ZZ32.RTTIc {
        private RTTIc() { super(FZZ32.class); };
        public static final RTTI ONLY = new RTTIc();
    }
}
