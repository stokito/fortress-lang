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

package com.sun.fortress.exceptions.shell;

/**
 * These are exceptions when the user has asked
 * us to perform an illegal operation in the shell. 
 */
public class UserError extends Exception {
    /**
     * Make Eclipse happy
     */
    private static final long serialVersionUID = -6692455449244407238L;

    public UserError(String msg) { super(msg); }
}
