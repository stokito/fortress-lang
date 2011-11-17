/*******************************************************************************
    Copyright 2009,2011, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler.runtimeValues;

public final class FStringVector extends fortress.CompilerBuiltin.StringVector.DefaultTraitMethods
        implements fortress.CompilerBuiltin.StringVector {
    private String[] val;

    public FStringVector(String[] a) {val = a;}

    public static FStringVector make(int s) {
        return new FStringVector(new String[s]);
    }

    public String toString() {
        String result = "[";
        for (int i = 0; i < val.length; i++)
            result = result + val[i] + " ";
        result = result + "]";
        return result;
    }

    public int dim() {return val.length;}

    public FJavaString asSring() { return FJavaString.make(toString());}
    
    public String getIndexedValue(int i) {return val[i];}
    public void putIndexedValue(int i, String x) {val[i] = x;}

    public String[] getValue() {return val;}

    public static FStringVector make(String[] v) {
        return new FStringVector(v);
    }
 
@Override
    public RTTI getRTTI() { return RTTIv.ONLY; }
    
    public static class RTTIv extends RTTI {
        private RTTIv() { super(FStringVector.class); };
        public static final RTTI ONLY = new RTTIv();
    }
}
