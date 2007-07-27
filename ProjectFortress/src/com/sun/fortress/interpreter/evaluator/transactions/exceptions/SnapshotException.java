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

package com.sun.fortress.interpreter.evaluator.transactions.exceptions;

/**
 * Thrown if shapshot validation fails. Will abort transaction if not
 * caught by the application.
 *
 * @author Maurice Herlihy
 */
public class SnapshotException extends java.lang.RuntimeException {

  /**
   * Creates a new <code>SnapshotException</code> instance with no detail message.
   */
  public SnapshotException() {
    super();
  }


  /**
   * Creates a new <code>SnapshotException</code> instance with the specified detail message.
   * @param msg the detail message.
   */
  public SnapshotException(String msg) {
    super(msg);
  }
}
