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

package com.sun.fortress.exceptions;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;

import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.nodes.AbstractNode;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.useful.HasAt;

public class UnificationError extends ProgramError {

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
