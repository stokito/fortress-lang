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

package com.sun.fortress.interpreter.evaluator;

public class RedefinitionError extends ProgramError {

    /**
     *
     */
    private static final long serialVersionUID = -2151402502448010576L;
//    private String ofWhat;
//    private String name;
//    private Object existingValue;
//    private Object attemptedReplacementValue;

    public RedefinitionError() {
        super();
        // TODO Auto-generated constructor stub
    }

    public RedefinitionError(String arg0) {
        super(arg0);
        // TODO Auto-generated constructor stub
    }

    public RedefinitionError(String arg0, Throwable arg1) {
        super(arg0, arg1);
        // TODO Auto-generated constructor stub
    }

    public RedefinitionError(Throwable arg0) {
        super(arg0);
        // TODO Auto-generated constructor stub
    }

    public RedefinitionError(String ofWhat, String name, Object existingValue, Object attemptedReplacementValue) {
        super("Redefinition of " + ofWhat + " " + name + " existing value=" + existingValue +
                ", second value=" + attemptedReplacementValue);
//        this.ofWhat = ofWhat;
//        this.name = name;
//        this.existingValue = existingValue;
//        this.attemptedReplacementValue = attemptedReplacementValue;
    }

    public RedefinitionError(String ofWhat, String name, Object attemptedReplacementValue) {
        super("Redefinition of " + ofWhat + " " + name +
                ", second value=" + attemptedReplacementValue);
//        this.ofWhat = ofWhat;
//        this.name = name;
//        this.existingValue = existingValue;
//        this.attemptedReplacementValue = attemptedReplacementValue;
    }

}
