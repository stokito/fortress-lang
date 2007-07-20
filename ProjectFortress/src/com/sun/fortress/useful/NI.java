/*******************************************************************************
    Copyright 2007 Sun Microsystems, Inc.,
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

package com.sun.fortress.useful;

/**
 * A bunch of generic methods, inspired by the knights who say this.getClass().getSimpleName().
 *
 * The vowels all throw exceptions.
 *
 * The consonants are filters.
 */
public class NI {

    /**
     * Not Allowed.
     */
    public static <T> T na() {
        throw new Error("Not allowed");
    }

    public static <T> T na(String what) {
        throw new Error("Not allowed: " + what);
    }

    /**
     * Not Implemented.
     */
    public static <T> T ni() {
        throw new Error("Not implemented");
    }

    /**
     * Not Possible.
     */
    public static <T> T np() {
        throw new Error("Not possible");
    }

   /**
     * Not Yet.
     */
    public static <T> T nyi() {
        throw new Error("Not yet implemented");
    }

   /**
     * Not Yet.
     */
    public static <T> T nyi(String name) {
        throw new Error(name + ": Not yet implemented");
    }

    /**
     * Identity function for non-nulls.
     * Throws a CHECKED exception.
    */
    public static <T> T cnnf(T x) throws CheckedNullPointerException {
        if (x == null)
            throw new CheckedNullPointerException("Null not allowed");
        return x;
    }
    
    /**
     * Identity function for non-nulls.
    */
    public static <T> T nnf(T x) {
        if (x == null)
            throw new NullPointerException("Null not allowed");
        return x;
    }

}
