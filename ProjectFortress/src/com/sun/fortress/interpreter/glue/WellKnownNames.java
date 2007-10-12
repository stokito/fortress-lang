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

package com.sun.fortress.interpreter.glue;

public class WellKnownNames {
    public static final String defaultSelfName = "self";
    public static final String secretSelfName = "$self"; // The true name of the object
    public static final String secretParentName = "$parent"; // The true name of the object
    public static String arrayTrait(int rank) {
        return "Array"+rank;
    }
    public static String arrayMaker(int rank) {
        return "__builtinFactory"+rank;
    }
    public static String arrayElementTypeName = "T";
    public static String arrayGetter = "get";
    public static String arrayPutter = "put";

    public static String setTrait = "Set";
    public static String setMaker = "SetMaker";
    public static String setElementTypeName = "T";

    public static String loopMethod = "loop";
    public static String generateMethod = "generate";
    public static String generatorCross = "cross";

    public static String forbiddenException = "ForbiddenException";
    public static String atomicConflictException = "AtomicConflict";
}
