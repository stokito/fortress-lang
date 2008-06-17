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
 * A ShellException should be thrown when any stage in the
 * shell finds itself in an inconsistent state, and wants to
 * provide feedback on the Fortress source program which will
 * enable the inconsistency to be debugged and/or worked around. 
 */
public class ShellException extends RuntimeException {
   public ShellException(Exception e) {
      super(e.getMessage());
   }
}
