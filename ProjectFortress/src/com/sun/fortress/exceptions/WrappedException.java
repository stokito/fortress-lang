/*******************************************************************************
    Copyright 2008, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.exceptions;

import java.io.PrintWriter;
import java.io.StringWriter;

import com.sun.fortress.exceptions.transactions.AbortedException;

public class WrappedException extends StaticError {

    /**
     * Make Eclipse happy
     */
    private static final long serialVersionUID = 4517154293769839846L;

    private final Throwable throwable;
	private boolean debug;

	@Override
	public String getMessage() {
		return throwable.getMessage();
	}

	@Override
	public String stringName() {
		return throwable.getMessage();
	}

	@Override
	public String toString() {
		if (this.debug) {
			StringWriter sw = new StringWriter();
		    PrintWriter pw = new PrintWriter(sw);
			throwable.printStackTrace(pw);
			return sw.toString();
		}
		return throwable.getMessage();
	}

	@Override
	public String at() {
		// TODO Auto-generated method stub
		return "no line information";
	}

	@Override
	public String description() {
		// TODO Auto-generated method stub
		return "";
	}

	@Override
	public Throwable getCause() {
		return throwable;
	}

    private void temp(Throwable th) {
        if (th instanceof AbortedException) {
            System.out.println("Creating a Wrapped Exception");
            Thread.dumpStack();
        }
    }        

	public WrappedException(Throwable th) {
		this(th, false);
        temp(th);
	}

	public WrappedException(Throwable th, boolean db) {
		throwable = th;
		debug = db;
        temp(th);
	}

}
