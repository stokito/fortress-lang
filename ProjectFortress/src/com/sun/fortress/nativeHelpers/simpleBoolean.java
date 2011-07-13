/*******************************************************************************
    Copyright 2011, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.nativeHelpers;

public class simpleBoolean {

    public static boolean booleanAnd(boolean a, boolean b) {
        return a & b;
    }

    public static boolean booleanOr(boolean a, boolean b) {
        return a | b;
    }

    public static boolean booleanXor(boolean a, boolean b) {
        return a ^ b;
    }

    public static boolean booleanEqv(boolean a, boolean b) {
        return !(a ^ b);
    }

    public static boolean booleanNot(boolean a) {
        return !a;
    }

}
