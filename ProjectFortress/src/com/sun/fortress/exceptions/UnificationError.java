/*******************************************************************************
    Copyright 2008, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.exceptions;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;

import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.nodes.AbstractNode;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.useful.HasAt;

public class UnificationError extends ProgramError {

    /**
     * Make Eclipse happy
     */
    private static final long serialVersionUID = -7366357826493314427L;

    public UnificationError() {
        super();
    }

    public UnificationError(HasAt loc, Environment env, String arg0) {
        super(loc,env,arg0);
    }

    public UnificationError(HasAt loc, String arg0) {
        super(loc,arg0);
    }

    public UnificationError(String arg0) {
        super(arg0);
    }

    public static <T> T unificationError(String msg) {
        throw new UnificationError(msg);
    }

    public static <T> T unificationError(HasAt loc, String msg) {
        throw new UnificationError(loc,msg);
    }

    public static <T> T unificationError(HasAt loc, Environment env, String msg) {
        throw new UnificationError(loc,env,msg);
    }

}
