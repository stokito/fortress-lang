/*******************************************************************************
    Copyright 2009,2011, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler.runtimeValues;

public class FJavaString extends fortress.CompilerBuiltin.JavaString.DefaultTraitMethods
        implements fortress.CompilerBuiltin.JavaString {
    final String val;

    private FJavaString(String x) { val = x; }
    public String getValue() { return val;}
    public String toString() { return val;}
    public static FJavaString make(String s) { return new FJavaString(s);}

    @Override
    public RTTI getRTTI() { return RTTIc.ONLY; }
    
    public static class RTTIc extends RTTI {
        private RTTIc() { super(FJavaString.class); };
        public static final RTTI ONLY = new RTTIc();
    }

}
