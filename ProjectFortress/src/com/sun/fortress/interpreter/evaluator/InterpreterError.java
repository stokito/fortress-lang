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

import com.sun.fortress.interpreter.useful.HasAt;

/*
 * An InterpreterError should be thrown when the interpreter finds
 * itself in an inconsistent state, and wants to provide feedback on
 * the Fortress source program which will enable the inconsistency to
 * be debugged and/or worked around.
 */

public class InterpreterError extends ProgramError {

    /**
     * Make Eclipse happy
     */
    private static final long serialVersionUID = 6117319678737763139L;

    public InterpreterError() {
        super();
    }

    public InterpreterError(HasAt loc, Environment env, String arg0) {
        super(loc,env,arg0);
    }

    public InterpreterError(HasAt loc, String arg0) {
        super(loc,arg0);
    }

    public InterpreterError(HasAt loc1, HasAt loc2, Environment env, String arg0) {
        super(loc1,loc2,env,arg0);
    }

    public InterpreterError(String arg0) {
        super(arg0);
    }

    public InterpreterError(String arg0, Throwable arg1) {
        super(arg0, arg1);
    }

    public InterpreterError(HasAt loc, Environment env, String arg0, Throwable arg1) {
        super(loc,env,arg0, arg1);
    }

    public InterpreterError(Throwable arg0) {
        super(arg0);
    }

    public InterpreterError(HasAt loc, String string, Throwable ex) {
        super(loc,string,ex);
    }

}
