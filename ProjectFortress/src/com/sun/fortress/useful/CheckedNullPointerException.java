/*******************************************************************************
    Copyright 2007 Sun Microsystems, Inc.,
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

package com.sun.fortress.useful;

public class CheckedNullPointerException extends Exception {

    /**
     * 
     */
    private static final long serialVersionUID = 3938875379343754616L;

    public CheckedNullPointerException() {
        // TODO Auto-generated constructor stub
    }

    public CheckedNullPointerException(String message) {
        super(message);
        // TODO Auto-generated constructor stub
    }

    public CheckedNullPointerException(Throwable cause) {
        super(cause);
        // TODO Auto-generated constructor stub
    }

    public CheckedNullPointerException(String message, Throwable cause) {
        super(message, cause);
        // TODO Auto-generated constructor stub
    }

}
