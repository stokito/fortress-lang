/*******************************************************************************
    Copyright 2009,2011, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler.runtimeValues;

import com.sun.fortress.compiler.runtimeValues.FNN64.RTTIc;

public class FNN64  extends fortress.CompilerBuiltin.NN64.DefaultTraitMethods implements fortress.CompilerBuiltin.NN64  {
    final long val;

    private FNN64(long x) { val = x; }
    public String toString() { return "" + val;}
    public long getValue() {return val;}
    public static FNN64 make(long x) {return new FNN64(x);}
    
    @Override
    public RTTI getRTTI() { return RTTIc.ONLY; }
    
    public static class RTTIc extends fortress.CompilerBuiltin.NN64.RTTIc {
        private RTTIc() { super(FNN64.class); };
        public static final RTTI ONLY = new RTTIc();
    }
}
