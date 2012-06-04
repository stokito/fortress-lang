/*******************************************************************************
    Copyright 2008,2009, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.exceptions.shell;

/**
 * A ShellException should be thrown when any stage in the
 * shell finds itself in an inconsistent state, and wants to
 * provide feedback on the Fortress source program which will
 * enable the inconsistency to be debugged and/or worked around.
 */
public class ShellException extends RuntimeException {

    /**
     * Make Eclipse happy
     */
    private static final long serialVersionUID = 75009923473500186L;

    public ShellException(Exception e) {
        super(e);
    }
}
