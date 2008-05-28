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

package com.sun.fortress.interpreter.evaluator;

import com.sun.fortress.useful.HasAt;
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
