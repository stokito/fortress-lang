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

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;

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

    public ProgramError setWhere(HasAt where) {
        this.where = where;
        return this;
    }

    public ProgramError setWhere2(HasAt where2) {
        this.where2 = where2;
        return this;
        }

    public ProgramError setWithin(Environment within) {
        this.within = within;
        return this;
        }

    public ProgramError() {
        super();

    }

    public ProgramError(HasAt loc, Environment env, String arg0) {
        super(arg0);
        where = loc; within = env;

    }

    public ProgramError(HasAt loc, String arg0) {
        super(arg0);
        where = loc;

    }

    public ProgramError(HasAt loc1, HasAt loc2, Environment env, String arg0) {
        super(arg0);
        where = loc1; where2 = loc2; within = env;

    }

    public ProgramError(String arg0) {
        super(arg0);

    }

    public ProgramError(String arg0, Throwable arg1) {
        super(arg0, arg1);

    }

    public ProgramError(HasAt loc, Environment env, String arg0, Throwable arg1) {
        super(arg0, arg1);
        where = loc; within = env;

    }

    public ProgramError(Throwable arg0) {
        super(arg0);

    }

    public ProgramError(HasAt loc, String string, Throwable ex) {
        this(loc, null, string, ex);
    }
}
