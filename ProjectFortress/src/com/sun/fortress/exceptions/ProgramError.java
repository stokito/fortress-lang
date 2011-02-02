/*******************************************************************************
    Copyright 2008, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.exceptions;

import java.util.List;

import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.nodes.AbstractNode;
import com.sun.fortress.useful.HasAt;

import static com.sun.fortress.nodes_util.ErrorMsgMaker.makeErrorMsg;

public class ProgramError extends FortressException {

    /**
     * Make Eclipse happy
     */
    private static final long serialVersionUID = 6117319678737763138L;

    private static void errorSer(StringBuilder s, Object message) {
        if (message == null) {
            s.append("null");
        } else if (message instanceof AbstractNode) {
            s.append(makeErrorMsg((AbstractNode)message));
        } else if (message instanceof List<?>) {
            boolean first = true;
            s.append("[");
            for (Object elt : (List<?>)message) {
                if (!first) s.append(",");
                first = false;
                errorSer(s,elt);
            }
            s.append("]");
        } else {
            s.append(message.toString());
        }
    }

    public static String errorMsg(Object... messages) {
        StringBuilder fullMessage = new StringBuilder();
        for (Object message : messages) {
            errorSer(fullMessage, message);
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
