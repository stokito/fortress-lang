/*******************************************************************************
    Copyright 2008, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.exceptions;

import com.sun.fortress.nodes_util.Span;

public class DesugarerError extends CompilerError {

    /**
     * Make Eclipse happy
     */
    private static final long serialVersionUID = 9175573225013299496L;

    public DesugarerError(String msg) {
		super(msg);
	}

	public DesugarerError(Span span, String msg) {
		super(span, msg);
	}

}
