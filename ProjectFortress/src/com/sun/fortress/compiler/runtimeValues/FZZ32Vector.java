/*******************************************************************************
    Copyright 2009,2011, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler.runtimeValues;

// It's kind of a kludge, but one of these babies can pretend to be either 1-D or 2-D.
// The 2-D variety can also be indexed using 1-D indexing.
public final class FZZ32Vector extends fortress.CompilerBuiltin.ZZ32Vector.DefaultTraitMethods
        implements fortress.CompilerBuiltin.ZZ32Vector {
    private int[] val;
    int lower_x, dim_x, lower_y, dim_y;

    private FZZ32Vector(int[] a, int l1, int d1, int l2, int d2) {
	val = a; lower_x = l1; dim_x = d1; lower_y = l2; dim_y = d2;
    }

    public static FZZ32Vector make(int l1, int d1, int l2, int d2) {
        return new FZZ32Vector(new int[(d2 == 0) ? d1 : d1 * d2], l1, d1, l2, d2);
    }

    public String toString() {
	StringBuffer result = new StringBuffer();
	if (dim_y == 0) {
	    // One-dimensional case
	    result.append("[");
	    for (int j = 0; j < dim_x; j++) {
		if (j > 0) result.append(" ");
		result.append(this.getIndexedValue(j));
	    }
	    result.append("]");
	} else {
	    // Two-dimensional case
	    result.append("\n(");
	    result.append(dim_x);
	    result.append(",");
	    result.append(dim_y);
	    result.append(")\n[");
	    for (int i = 0; i < dim_x; i++) {
		for (int j = 0; j < dim_y; j++) {
		    result.append(" ");
		    result.append(this.getIndexedValue(i,j));
		}
		if (i < dim_x-1) result.append("\n ");
		else result.append(" ]");
	    }
	}
	return result.toString();
    }

    public int dim()  {return val.length;}
    public int rows() {return dim_x;}
    public int cols() {return dim_y;}

    public FJavaString asString() { return FJavaString.make(toString());}
    
    public int getIndexedValue(int i) {return val[i-lower_x];}
    public int getIndexedValue(int i, int j) {return val[(i-lower_x)*dim_y + (j-lower_y)];}

    public void putIndexedValue(int i, int newval) {val[i-lower_x] = newval;}
    public void putIndexedValue(int i, int j, int newval) {val[(i-lower_x)*dim_y + (j-lower_y)] = newval;}

    public int[] getValue() {return val;}

    public static FZZ32Vector make(int[] v) {
        return new FZZ32Vector(v, 0, v.length, 0, 0);
    }
 
@Override
    public RTTI getRTTI() { return RTTIv.ONLY; }
    
    public static class RTTIv extends fortress.CompilerBuiltin.ZZ32Vector.RTTIc {
        private RTTIv() { super(FZZ32Vector.class); };
        public static final RTTI ONLY = new RTTIv();
    }
}
