/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.exceptions;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;

import edu.rice.cs.plt.tuple.Option;
import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.nodes_util.ErrorMsgMaker;
import com.sun.fortress.useful.HasAt;


public abstract class FortressException extends RuntimeException {

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

    public FortressException setWhere(HasAt where) {
        /* The null test here is because the unit tests sometimes
           fling around null location information. */
        if (where!=null) this.where.add(where);
        return this;
    }

    public FortressException setContext(HasAt where, Environment within) {
        setWithin(within);
        return setWhere(where);
    }

    public FortressException setWithin(Environment within) {
        if (this.within != null) this.within = within;
        return this;
    }

    public FortressException() {
        super();
    }

    public FortressException(HasAt loc, Environment env, String arg0) {
        super(arg0);
        setWhere(loc);
        within = env;
    }

    public FortressException(HasAt loc, String arg0) {
        super(arg0);
        setWhere(loc);

    }

    public FortressException(HasAt loc1, HasAt loc2, Environment env, String arg0) {
        super(arg0);
        setWhere(loc1); where2 = loc2; within = env;

    }

    public FortressException(String arg0) {
        super(arg0);

    }

    public FortressException(String arg0, Throwable arg1) {
        super(arg0, arg1);

    }

    public FortressException(HasAt loc, Environment env, String arg0, Throwable arg1) {
        super(arg0, arg1);
        setWhere(loc); within = env;

    }

    public FortressException(Throwable arg0) {
        super(arg0);

    }

    public FortressException(HasAt loc, String string, Throwable ex) {
        this(loc, null, string, ex);
    }

    public FortressException(Iterable<? extends StaticError> errors) {
        this.staticErrors = errors;
    }

    public Option<HasAt> getLoc() {
        if ( this.where.isEmpty() )
            return Option.none();
        else return Option.wrap(this.where.get(0));
    }

    public String getOriginalMessage() {
        return super.getMessage();
    }

    /* (non-Javadoc)
     * @see java.lang.Throwable#getMessage()
     */
    @Override
    public String getMessage() {
        String msg = super.getMessage();
        StringBuilder buf = new StringBuilder();
        buf.append(msg);
        if (msg == null)
            msg = "";
        if (staticErrors != null) {
            for (StaticError se : staticErrors) {
                if (msg.length() > 0) {
                    buf.append("\n");
                }
                buf.append(se.getMessage());
            }
        }
        msg = buf.toString();
        if (where.size() > 0) {
            StringBuilder res = new StringBuilder();
            res.append(where.get(0).at());
            if (where2 != null) {
                res.append(": and\n");
                res.append(where2.at());
            }
            res.append(":\n");
            res.append(msg);
            if (where.size() > 1) {
                /* If additional location information was provided while
                   unwinding from an error, print it. */
                res.append("\nContext:\n");
                for (HasAt loc : where) {
                    res.append(loc.at());
                    res.append(":\n");
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
