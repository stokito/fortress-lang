/*******************************************************************************
    Copyright 2009,2011, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler.runtimeValues;

import com.sun.fortress.compiler.runtimeValues.FZZ.RTTIc;
import java.math.BigInteger;

public class FZZ  extends fortress.CompilerBuiltin.ZZ.DefaultTraitMethods implements fortress.CompilerBuiltin.ZZ  {
    final BigInteger val;

    private FZZ(String x) { val = new BigInteger(x); }
    public String toString() { return val.toString();}
    public BigInteger getValue() {return val;}
    public static FZZ make(String x) {return new FZZ(x);}
    public static FZZ make(BigInteger x) {return new FZZ(x.toString());}
    
    @Override
    public RTTI getRTTI() { return RTTIc.ONLY; }
    
    public static class RTTIc extends fortress.CompilerBuiltin.ZZ.RTTIc {
        private RTTIc() { super(FZZ.class); };
        public static final RTTI ONLY = new RTTIc();
    }
}
