/*******************************************************************************
    Copyright 2009,2011, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler.runtimeValues;

public final class FBoolean extends fortress.CompilerBuiltin.Boolean.DefaultTraitMethods
        implements fortress.CompilerBuiltin.Boolean  {
    public static final FBoolean TRUE = new FBoolean(true);
    public static final FBoolean FALSE = new FBoolean(false);

    final boolean val;

    FBoolean(boolean x) { val = x; }
    public String toString() { return "" + val;}
    public boolean getValue() {return val;}
    public static FBoolean make(boolean x) {return x ? TRUE : FALSE;}

    @Override
    public RTTI getRTTI() { return /* fortress.CompilerBuiltin.Boolean. */ RTTIc.ONLY; }
    
    public static class RTTIc extends fortress.CompilerBuiltin.Boolean.RTTIc {
        private RTTIc() { super(FBoolean.class); };
        public static final RTTI ONLY = new RTTIc();
    }

}
