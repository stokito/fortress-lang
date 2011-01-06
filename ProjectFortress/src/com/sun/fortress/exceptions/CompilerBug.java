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

package com.sun.fortress.exceptions;

import com.sun.fortress.useful.HasAt;

/**
 * An CompilerBug should be thrown when the Compiler finds
 * itself in an inconsistent state, and wants to provide feedback on
 * the Fortress source program which will enable the inconsistency to
 * be debugged and/or worked around.
 */
public class CompilerBug extends FortressException {

    /**
     * Make Eclipse happy
     */
    private static final long serialVersionUID = 6117319678737763139L;

    public CompilerBug() {
        super();
    }

    public CompilerBug(HasAt loc, String arg0) {
        super(loc,arg0);
    }

    public CompilerBug(String arg0) {
        super(arg0);
    }

    public CompilerBug(String arg0, Throwable arg1) {
        super(arg0, arg1);
    }

    public CompilerBug(Throwable arg0) {
        super(arg0);
    }

    public CompilerBug(HasAt loc, String string, Throwable ex) {
        super(loc,string,ex);
    }

    public static <T> T bug(String msg) {
        throw new CompilerBug("** bug! " + msg);
    }

    public static <T> T bug(HasAt loc, String arg0) {
        throw new CompilerBug(loc, arg0);
    }

    public static <T> T bug(String arg0, Throwable arg1) {
        throw new CompilerBug(arg0, arg1);
    }

     public static <T> T bug(Throwable arg0) {
        throw new CompilerBug(arg0);
    }

    public static <T> T bug(HasAt loc, String string, Throwable ex) {
        throw new CompilerBug(loc, string, ex);
    }
}
