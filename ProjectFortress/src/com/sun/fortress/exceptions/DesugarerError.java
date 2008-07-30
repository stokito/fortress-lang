package com.sun.fortress.exceptions;

import com.sun.fortress.nodes_util.Span;

public class DesugarerError extends CompilerError {

	public DesugarerError(String msg) {
		super(msg);
	}
	
	public DesugarerError(Span span, String msg) {
		super(span, msg);
	}

}
