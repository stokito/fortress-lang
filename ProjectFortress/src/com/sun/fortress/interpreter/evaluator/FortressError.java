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
import java.util.ArrayList;

import com.sun.fortress.compiler.StaticError;
import com.sun.fortress.nodes.AbstractNode;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.nodes_util.ErrorMsgMaker;
import com.sun.fortress.useful.HasAt;


public abstract class FortressError extends RuntimeException {

    /**
     * Make Eclipse happy
     */
    private static final long serialVersionUID = 6117319678737763137L;
    private static final boolean dumpEnv = false;

    private ArrayList<HasAt> where = new ArrayList<HasAt>();
    private HasAt where2;
    private Environment within;
    private Iterable<? extends StaticError> staticErrors;

    public static String errorMsg(Object... messages) {
        return ErrorMsgMaker.errorMsg(messages);
    }

    public FortressError setWhere(HasAt where) {
        /* The null test here is because the unit tests sometimes
           fling around null location information. */
        if (where!=null) this.where.add(where);
        return this;
    }

    public FortressError setContext(HasAt where, Environment within) {
        if (this.within != null) this.within = within;
        setWhere(where);
        return this;
    }

    public FortressError() {
        super();
    }

    public FortressError(HasAt loc, Environment env, String arg0) {
        super(arg0);
        setWhere(loc);
        within = env;
    }

    public FortressError(HasAt loc, String arg0) {
        super(arg0);
        setWhere(loc);

    }

    public FortressError(HasAt loc1, HasAt loc2, Environment env, String arg0) {
        super(arg0);
        setWhere(loc1); where2 = loc2; within = env;

    }

    public FortressError(String arg0) {
        super(arg0);

    }

    public FortressError(String arg0, Throwable arg1) {
        super(arg0, arg1);

    }

    public FortressError(HasAt loc, Environment env, String arg0, Throwable arg1) {
        super(arg0, arg1);
        setWhere(loc); within = env;

    }

    public FortressError(Throwable arg0) {
        super(arg0);

    }

    public FortressError(HasAt loc, String string, Throwable ex) {
        this(loc, null, string, ex);
    }

    public FortressError(Iterable<? extends StaticError> errors) {
        this.staticErrors = errors;
    }

    /* (non-Javadoc)
     * @see java.lang.Throwable#getMessage()
     */
    @Override
    public String getMessage() {
        String msg = super.getMessage();
        if (msg == null)
            msg = "";
        if (staticErrors != null) {
            for (StaticError se : staticErrors) {
                if (msg.length() > 0) {
                    msg = msg + "\n";
                }
                
                msg = msg + se.getMessage();
            }
        }
        if (where.size() > 0) {
            StringBuffer res = new StringBuffer();
            res.append('\n');
            res.append(where.get(0).at());
            if (where2 != null) {
                res.append(": and\n");
                res.append(where2.at());
            }
            res.append(": ");
            res.append(msg);
            if (where.size() > 1) {
                /* If additional location information was provided while
                   unwinding from an error, print it. */
                res.append("\nContext:\n");
                for (HasAt loc : where) {
                    res.append(loc.at());
                    res.append("\n");
                }
            }
            return res.toString();
        } else {
            return msg;
        }
    }

    /* (non-Javadoc)
     * @see java.lang.Throwable#printStackTrace()
     */
    @Override
    public void printStackTrace() {
        // TODO Auto-generated method stub
        super.printStackTrace();
    }

    /**
     *
     */
    private void printInterpreterStackTrace(PrintWriter app) {
        if (dumpEnv && within != null) {
            try {
            within.dump(app);
            } catch (IOException ex) {
                app.println("Error dumping interpreter environment");
                ex.printStackTrace(app);
            }
        }
    }

    /**
     *
     */
    public void printInterpreterStackTrace(PrintStream app) {
        if (dumpEnv && within != null) {
            try {
            within.dump(app);
            } catch (IOException ex) {
                app.println("Error dumping interpreter environment");
                ex.printStackTrace(app);
            }
        }
    }

    /* (non-Javadoc)
     * @see java.lang.Throwable#printStackTrace(java.io.PrintStream)
     */
    @Override
    public void printStackTrace(PrintStream arg0) {
        // TODO Auto-generated method stub
        super.printStackTrace(arg0);
        printInterpreterStackTrace(arg0);
    }

    /* (non-Javadoc)
     * @see java.lang.Throwable#printStackTrace(java.io.PrintWriter)
     */
    @Override
    public void printStackTrace(PrintWriter arg0) {
        // TODO Auto-generated method stub
        super.printStackTrace(arg0);
        printInterpreterStackTrace(arg0);
    }
    
    public Iterable<? extends StaticError> getStaticErrors() {
        return staticErrors;
    }

}
