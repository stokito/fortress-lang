package com.sun.fortress.exceptions;

import java.io.PrintWriter;
import java.io.StringWriter;

public class WrappedException extends StaticError {

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