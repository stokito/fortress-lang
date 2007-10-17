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

package com.sun.fortress.interpreter.evaluator.transactions.exceptions;

/**
 * Thrown to indicate an error in the use of the transactional memory;
 * that is, a violation of the assumptions of use.
 */
public class PanicException extends java.lang.RuntimeException {

    /**
     * Creates new <code>PanicException</code> with no detail message.
     */
    public PanicException() {
    }

    public PanicException(String format, Object ... args) {
      super(String.format(format, args));
    }

    /**
     * Creates a new <code>PanicException</code> with the specified detail message.
     *
     * @param msg the detail message.
     */
    public PanicException(String msg) {
        super(msg);
    }

    /**
     * Creates an <code>PanicException</code> with the specified cause.
     *
     * @param cause Throwable that caused PanicException to be thrown
     */
    public PanicException(Throwable cause) {
        super(cause);
        StackTraceElement [] t0,t1, t;
        t0 = cause.getStackTrace();
        if (t0==null) return;
        t1 = getStackTrace();
        if (t1==null) {
            setStackTrace(t0);
            return;
        }
        t = new StackTraceElement[t0.length+t1.length];
        System.arraycopy(t0,0,t,0,t0.length);
        System.arraycopy(t1,0,t,t0.length,t1.length);
        setStackTrace(t);
    }
}
