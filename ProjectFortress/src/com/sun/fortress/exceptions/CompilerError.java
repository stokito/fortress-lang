/*******************************************************************************
    Copyright 2008,2009, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.exceptions;

import com.sun.fortress.useful.HasAt;

public class CompilerError extends RuntimeException {

    /**
     * Make Eclipse happy
     */
    private static final long serialVersionUID = 5598379625320784306L;

    private HasAt loc;

    public CompilerError(String message) {
        super(message);
    }

    public CompilerError(String message, Exception e) {
        super(message, e);
    }

    public CompilerError(Exception e) {
        super(e);
    }

    public CompilerError(HasAt loc, String message) {
        super("\n" + loc.at() + ":\n    " + message);
        this.loc = loc;
    }

    public HasAt getLoc() {
        return loc;
    }
}
