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
 * RepositoryErrors are either illegal Fortress programs or 
 * exceptional behavior in the interpreter that relate to
 * Components and APIs (see chapter 20 of the Fortress Language
 *  Specification). 
 */
public class RepositoryError extends RuntimeException {
    public RepositoryError(String _message) {
        super(_message);
    }

}
