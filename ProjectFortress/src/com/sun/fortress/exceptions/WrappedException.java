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

import java.io.PrintWriter;
import java.io.StringWriter;

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

	public WrappedException(Throwable th) {
		this(th, false);
	}

	public WrappedException(Throwable th, boolean db) {
		throwable = th;
		debug = db;
	}

}
