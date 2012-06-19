/*******************************************************************************
 Copyright 2009,2010, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.nativeHelpers;
import com.sun.fortress.compiler.runtimeValues.FStringVector;
import com.sun.fortress.runtimeSystem.MainWrapper;

public class simpleStringVector {

    public static String getIndexedValue(FStringVector v, int i) {return v.getIndexedValue(i);}
    public static void putIndexedValue(FStringVector v, int i, String val) {
        v.putIndexedValue(i, val);
    }

    public static int getSize(FStringVector v) {return v.dim();}

    public static FStringVector make(int i) {return FStringVector.make(i);}
    
}
