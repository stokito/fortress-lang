/*******************************************************************************
    Copyright 2009 Sun Microsystems, Inc.,
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
        super("\n" + loc.at() + "\n    " + message);
        this.loc = loc;
    }

    public HasAt getLoc() {
        return loc;
    }
}
