/*******************************************************************************
    Copyright 2008, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.exceptions;


public class RedefinitionError extends ProgramError {

    /**
     * Thrown when we try to redefine an already-defined Fortress variable.
     *
     * We retain the old and new values explicitly as part of a
     * stopgap measure.  The current (naive) linker attempts to create
     * duplicate definitions when performing certain kinds of
     * recursive import.  We catch this problem and silently ignore it
     * within the linker (in Driver.java).  We expect this problem to
     * go away once we have better disambiguation and linking code, so
     * we've limited the workaround to the code that causes the
     * problem in the first place rather than infecting eg BetterEnv
     * with yet another group of environment-updating methods.
     */
    private static final long serialVersionUID = -2151402502448010576L;
//    private String ofWhat;
//    private String name;
    public final Object existingValue;
    public final Object attemptedReplacementValue;

    public RedefinitionError(String ofWhat, String name, Object existingValue, Object attemptedReplacementValue) {
        super("Redefinition of " + ofWhat + " " + name + " existing value=" + existingValue +
                ", second value=" + attemptedReplacementValue);
//        this.ofWhat = ofWhat;
//        this.name = name;
        this.existingValue = existingValue;
        this.attemptedReplacementValue = attemptedReplacementValue;
    }

}
