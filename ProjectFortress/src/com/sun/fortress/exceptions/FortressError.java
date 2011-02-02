/*******************************************************************************
    Copyright 2008, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.exceptions;

import com.sun.fortress.useful.HasAt;
import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.interpreter.evaluator.values.FObject;

public class FortressError extends FortressException {

    /**
     * Make Eclipse happy
     */
	private static final long serialVersionUID = 241932492083578237L;
	FObject exc;

    public FortressError(HasAt loc, Environment env, FObject e) {
        super(loc,env,e.type().toString());
        exc = e;
    }

    public FortressError(FObject e) {
        super(e.type().toString());
        exc = e;
    }

    public String toString() {
        return "FortressException: " + exc.toString();
    }

    public FObject getException() {return exc;}
    public String getName() {return exc.toString();}
}
