/*******************************************************************************
 Copyright 2009,2010, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.nativeHelpers;

public class simpleChar {

    public static String charToString(char x) {
        return Character.toString(x);
    }

    public static boolean charLT(char a, char b) {
        return a < b;
    }

    public static boolean charLE(char a, char b) {
        return a <= b;
    }

    public static boolean charGT(char a, char b) {
        return a > b;
    }

    public static boolean charGE(char a, char b) {
        return a >= b;
    }

    public static boolean charEQ(char a, char b) {
        return a == b;
    }
}
