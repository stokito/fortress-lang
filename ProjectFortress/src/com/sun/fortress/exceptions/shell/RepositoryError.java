/*******************************************************************************
    Copyright 2008, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.exceptions.shell;

/**
 * RepositoryErrors are either illegal Fortress programs or 
 * exceptional behavior in the interpreter that relate to
 * Components and APIs (see chapter 20 of the Fortress Language
 *  Specification). 
 */
public class RepositoryError extends RuntimeException {

    /**
     * Make Eclipse happy
     */
    private static final long serialVersionUID = 3012759991453588461L;

    public RepositoryError(String _message) {
        super(_message);
    }

}
