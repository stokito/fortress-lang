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
 * Thrown by an attempt to open a <code>TMObject</code> to indicate
 * that the current transaction cannot commit.
 **/
public class AbortedException extends java.lang.RuntimeException {
  static final long serialVersionUID = 6572490566353395650L;

  /**
   * Creates a new <code>DeniedException</code> instance with no detail message.
   */
  public AbortedException() {
    super(Thread.currentThread().getName());
  }


  /**
   * Creates a new <code>Denied</code> instance with the specified detail message.
   * @param msg the detail message.
   */
  public AbortedException(String msg) {
    super(Thread.currentThread().getName() + " " + msg);
  }
}
