/*******************************************************************************
    Copyright 2009,2011, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler.runtimeValues;

public final class FZZ32Vector extends fortress.CompilerBuiltin.ZZ32Vector.DefaultTraitMethods
        implements fortress.CompilerBuiltin.ZZ32Vector {
    private int[] val;
    int dim_x;
    int dim_y;

    private FZZ32Vector(int[] a, int x) {val = a; dim_x = x;}
    private FZZ32Vector(int[] a, int x, int y) {val = a; dim_x = x; dim_y = y;}

    public static FZZ32Vector make(int s) {
        return new FZZ32Vector(new int[s], s);
    }

    public static FZZ32Vector make (int s, int t) {
        return new FZZ32Vector(new int[s * t], s, t);
    }

    public String toString() {
        String result = "[";
        for (int i = 0; i < val.length; i++)
            result = result + val[i] + " ";
        result = result + "]";
        return result;
    }

    public int dim()  {return val.length;}
    public int rows() {return dim_x;}
    public int cols() {return dim_y;}

    public FString asSring() { return new FString(toString());}
    
    public int getIndexedValue(int i) {return val[i];}
    public int getIndexedValue(int i, int j) {return val[i*dim_x + j];}

    public void putIndexedValue(int i, int x) {val[i] = x;}
    public void putIndexedValue(int i, int j, int x) {val[i*dim_x + j] = x;}

    public int[] getValue() {return val;}

    public static FZZ32Vector make(int[] v) {
        return new FZZ32Vector(v, v.length);
    }
 
@Override
    public RTTI getRTTI() { return RTTIv.ONLY; }
    
    public static class RTTIv extends RTTI {
        private RTTIv() { super(FZZ32Vector.class); };
        public static final RTTI ONLY = new RTTIv();
    }
}
