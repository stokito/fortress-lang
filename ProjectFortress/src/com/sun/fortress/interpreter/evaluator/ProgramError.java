/*******************************************************************************
    Copyright 2008 Sun Microsystems, Inc.,
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

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;

import com.sun.fortress.compiler.StaticError;
import com.sun.fortress.nodes.AbstractNode;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.useful.HasAt;

import static com.sun.fortress.nodes_util.ErrorMsgMaker.makeErrorMsg;

public class ProgramError extends FortressError {

    /**
     * Make Eclipse happy
     */
    private static final long serialVersionUID = 6117319678737763138L;

    public static String errorMsg(Object... messages) {
        StringBuffer fullMessage = new StringBuffer();
        for (Object message : messages) {
            if (message == null) {
                fullMessage.append("null");
            } else if (message instanceof AbstractNode) {
                fullMessage.append(makeErrorMsg((AbstractNode)message));
            }
            else {
                fullMessage.append(message.toString());
            }
        }
        return fullMessage.toString();
    }

    public ProgramError() {
        super();
    }

    public ProgramError(HasAt loc, Environment env, String arg0) {
        super(loc,env,arg0);
    }

    public ProgramError(HasAt loc, String arg0) {
        super(loc,arg0);
    }

    public ProgramError(HasAt loc1, HasAt loc2, Environment env, String arg0) {
        super(loc1,loc2,env,arg0);
    }

    public ProgramError(String arg0) {
        super(arg0);
    }

    public ProgramError(String arg0, Throwable arg1) {
        super(arg0, arg1);
    }

    public ProgramError(HasAt loc, Environment env, String arg0, Throwable arg1) {
        super(loc,env,arg0, arg1);
    }

    public ProgramError(Throwable arg0) {
        super(arg0);
    }

    public ProgramError(HasAt loc, String string, Throwable ex) {
        this(loc, null, string, ex);
    }


    public ProgramError(Iterable<? extends StaticError> errors) {
       super(errors);
    }

    public static <T> T error(String msg) {
        throw new ProgramError(msg);
    }

    public static <T> T error(HasAt loc, Environment env, String arg0) {
        throw new ProgramError(loc, env, arg0);
    }

    public static <T> T error(HasAt loc, String arg0) {
        throw new ProgramError(loc, arg0);
    }

    public static <T> T error(HasAt loc1, HasAt loc2, Environment env, String arg0) {
        throw new ProgramError(loc1, loc2, env, arg0);
    }

    public static <T> T error(String arg0, Throwable arg1) {
        throw new ProgramError(arg0, arg1);
    }

    public static <T> T error(HasAt loc, Environment env, String arg0, Throwable arg1) {
        throw new ProgramError(loc, env, arg0, arg1);
    }

    public static <T> T error(Throwable arg0) {
        throw new ProgramError(arg0);
    }

    public static <T> T error(HasAt loc, String string, Throwable ex) {
        throw new ProgramError(loc, string, ex);
    }
}
