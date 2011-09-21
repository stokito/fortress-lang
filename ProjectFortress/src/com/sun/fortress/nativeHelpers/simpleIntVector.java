/*******************************************************************************
 Copyright 2009,2010, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.nativeHelpers;
import com.sun.fortress.compiler.runtimeValues.FZZ32Vector;

public class simpleIntVector {
    //    public static int getIndexedValue(int[] v, int i) {return v[i];}
    //    public static void putIndexedValue(int[] v, int i, int val) {v[i] = val;}

    //    These should not be needed, but we have to crawl before we can run 
    // We need indices to start at 1 for our ProcFreqFinalTests, so do the updates
    // here for now.

    public static int getIndexedValue(FZZ32Vector v, int i) {
        return v.getIndexedValue(i-1);
    }
    public static int getIndexedValue(FZZ32Vector v, int i, int j) {
        return v.getIndexedValue(i-1,j-1);}

    public static void putIndexedValue(FZZ32Vector v, int i, int val) {
        v.putIndexedValue(i-1, val);
    }
    public static void putIndexedValue(FZZ32Vector v, int i, int j, int val) {
        v.putIndexedValue(i-1, j-1, val);
    }

    public static int getSize(FZZ32Vector v) {return v.dim();}
    public static int getRows(FZZ32Vector v) {return v.rows();}
    public static int getCols(FZZ32Vector v) {return v.cols();}

    public static FZZ32Vector make(int i) {return FZZ32Vector.make(i);}
    public static FZZ32Vector make(int i,int j) {return FZZ32Vector.make(i,j);}
}
