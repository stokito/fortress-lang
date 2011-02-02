/*******************************************************************************
    Copyright 2008, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

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
