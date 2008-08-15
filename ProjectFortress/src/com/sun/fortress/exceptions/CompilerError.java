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

import com.sun.fortress.nodes_util.Span;

public class CompilerError extends RuntimeException {

    /**
     * Make Eclipse happy
     */
    private static final long serialVersionUID = 5598379625320784306L;

    private Span span;

	public CompilerError(String message) {
		super(message);
	}
	
        public CompilerError(String message, Exception e) {
		super(message, e);
	}
        
        public CompilerError(Exception e) {
		super(e);
	}

	public CompilerError(Span span, String message) {
		super(span.toString() + message);
		this.span = span;
	}

	public Span getSpan() {
		return span;
	}
}
