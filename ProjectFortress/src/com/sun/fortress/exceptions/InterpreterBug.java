/*******************************************************************************
    Copyright 2008,2009, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.exceptions;

import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.useful.HasAt;

/**
 * An InterpreterBug should be thrown when the interpreter finds
 * itself in an inconsistent state, and wants to provide feedback on
 * the Fortress source program which will enable the inconsistency to
 * be debugged and/or worked around.
 */
public class InterpreterBug extends FortressException {

    /**
     * Make Eclipse happy
     */
    private static final long serialVersionUID = 6117319678737763139L;

    public InterpreterBug() {
        super();
    }

    public InterpreterBug(HasAt loc, Environment env, String arg0) {
        super(loc,env,arg0);
    }

    public InterpreterBug(HasAt loc, String arg0) {
        super(loc,arg0);
    }

    public InterpreterBug(HasAt loc1, HasAt loc2, Environment env, String arg0) {
        super(loc1,loc2,env,arg0);
    }

    public InterpreterBug(String arg0) {
        super(arg0);
    }

    public InterpreterBug(String arg0, Throwable arg1) {
        super(arg0, arg1);
    }

    public InterpreterBug(HasAt loc, Environment env, String arg0, Throwable arg1) {
        super(loc,env,arg0, arg1);
    }

    public InterpreterBug(Throwable arg0) {
        super(arg0);
    }

    public InterpreterBug(HasAt loc, String string, Throwable ex) {
        super(loc,string,ex);
    }

    public static <T> T bug(String msg) {
        throw new InterpreterBug("** bug! " + msg);
    }

    public static <T> T bug(HasAt loc, Environment env, String arg0) {
        throw new InterpreterBug(loc, env, arg0);
    }

    public static <T> T bug(HasAt loc, String arg0) {
        throw new InterpreterBug(loc, arg0);
    }

    public static <T> T bug(HasAt loc1, HasAt loc2, Environment env, String arg0) {
        throw new InterpreterBug(loc1, loc2, env, arg0);
    }

    public static <T> T bug(String arg0, Throwable arg1) {
        throw new InterpreterBug(arg0, arg1);
    }

    public static <T> T bug(HasAt loc, Environment env, String arg0, Throwable arg1) {
        throw new InterpreterBug(loc, env, arg0, arg1);
    }

    public static <T> T bug(Throwable arg0) {
        throw new InterpreterBug(arg0);
    }

    public static <T> T bug(HasAt loc, String string, Throwable ex) {
        throw new InterpreterBug(loc, string, ex);
    }
}
