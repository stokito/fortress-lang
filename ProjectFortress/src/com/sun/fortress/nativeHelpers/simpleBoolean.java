/*******************************************************************************
    Copyright 2011 Sun Microsystems, Inc.,
    4150 Network Circle, Santa Clara, California 95054, U.S.A.
    All rights reserved.

    U.S. Government Rights - Commercial software.
    Government users are subject to the Sun Microsystems, Inc. standard
    license agreement and applicable provisions of the FAR and its supplements.

    Use is subject to license terms.

    This distribution may include materials developed by third parties.

    Sun, Sun Microsystems, the Sun logo and Java are trademarks or registered
    trademarks of Sun Microsystems, Inc. in the U.S. and other countries.
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
