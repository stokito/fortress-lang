/*******************************************************************************
 Copyright 2009,2010, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.nativeHelpers;
import com.sun.fortress.compiler.runtimeValues.FVector;

public class simpleIntVector {
    //    public static int getIndexedValue(int[] v, int i) {return v[i];}
    //    public static void putIndexedValue(int[] v, int i, int val) {v[i] = val;}

    //    These should not be needed, but we have to crawl before we can run 

    public static int getIndexedValue(FVector v, int i) {return v.getIndexedValue(i);}
    public static void putIndexedValue(FVector v, int i, int val) {
        v.putIndexedValue(i, val);
    }

    public static FVector make(int i) {return FVector.make(i);}
}
