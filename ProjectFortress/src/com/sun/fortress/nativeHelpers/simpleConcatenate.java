/*******************************************************************************
 Copyright 2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.nativeHelpers;

public class simpleConcatenate {

    public static String nativeConcatenate(String s1, String s2) {
        return s1 + s2;
    }

    public static int nativeStrlen(String s1) {
        return s1.length();
    }

}
