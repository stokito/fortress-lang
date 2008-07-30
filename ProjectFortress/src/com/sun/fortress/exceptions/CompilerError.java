package com.sun.fortress.exceptions;

import com.sun.fortress.nodes_util.Span;

public class CompilerError extends RuntimeException {
	private Span span;
	
	public CompilerError(String message) {
		super(message);
	}
	
	public CompilerError(Span span, String message) {
		super(span.toString() + message);
		this.span = span;
	}

	public Span getSpan() {
		return span;
	}
}
