/*******************************************************************************
    Copyright 2009,2011, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler.runtimeValues;

public final class FVector extends fortress.CompilerBuiltin.Vector.DefaultTraitMethods
        implements fortress.CompilerBuiltin.Vector {
    private int[] val;

    private FVector(int[] a) {val = a;}

    public static FVector make(int s) {
        return new FVector(new int[s]);
    }

    public String toString() {
        String result = "[";
        for (int i = 0; i < val.length; i++)
            result = result + val[i] + " ";
        result = result + "]";
        return result;
    }

    public int dim() {return val.length;}

    public FString asSring() { return new FString(toString());}
    
    public int getIndexedValue(int i) {return val[i];}
    public void putIndexedValue(int i, int x) {val[i] = x;}

    public int[] getValue() {return val;}
    public static FVector make(int[] v) {
        return new FVector(v);
    }
 
@Override
    public RTTI getRTTI() { return RTTIv.ONLY; }
    
    public static class RTTIv extends RTTI {
        private RTTIv() { super(FVector.class); };
        public static final RTTI ONLY = new RTTIv();
    }
}
