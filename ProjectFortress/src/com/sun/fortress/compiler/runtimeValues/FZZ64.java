/*******************************************************************************
    Copyright 2009,2011, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler.runtimeValues;

public class FZZ64  extends fortress.CompilerBuiltin.ZZ64.DefaultTraitMethods implements fortress.CompilerBuiltin.ZZ64  {
    final long val;

    private FZZ64(long x) { val = x; }
    public String toString() { return "" + val;}
    public long getValue() {return val;}
    public static FZZ64 make(long x) {return new FZZ64(x);}
    
    @Override
    public RTTI getRTTI() { return RTTIc.ONLY; }
    
    public static class RTTIc extends RTTI {
        private RTTIc() { super(FZZ64.class); };
        public static final RTTI ONLY = new RTTIc();
    }
}
