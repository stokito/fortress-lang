/*******************************************************************************
 Copyright 2009,2010, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.nativeHelpers;
import com.sun.fortress.compiler.runtimeValues.FZZ32Vector;

public class simpleIntVector {

    public static String asString(FZZ32Vector v) { return v.toString(); }

    public static int getIndexedValue(FZZ32Vector v, int i) {
        return v.getIndexedValue(i);
    }
    public static int getIndexedValue(FZZ32Vector v, int i, int j) {
       	// System.out.println("(" + getRows(v) + "," + getCols(v) + ")[" + i + "," + j + "]");
        return v.getIndexedValue(i,j);}

    public static void putIndexedValue(FZZ32Vector v, int i, int val) {
	// System.out.println("(" + getRows(v) + "," + getCols(v) + ")[" + i + "] := " + val);
        v.putIndexedValue(i, val);
    }
    public static void putIndexedValue(FZZ32Vector v, int i, int j, int val) {
       	// System.out.println("(" + getRows(v) + "," + getCols(v) + ")[" + i + "," + j + "] := " + val);
        v.putIndexedValue(i, j, val);
    }

    public static int getSize(FZZ32Vector v) {return v.dim();}
    public static int getRows(FZZ32Vector v) {return v.rows();}
    public static int getCols(FZZ32Vector v) {return v.cols();}

    public static FZZ32Vector make(int l1,int d1,int l2,int d2) {return FZZ32Vector.make(l1,d1,l2,d2);}
}
