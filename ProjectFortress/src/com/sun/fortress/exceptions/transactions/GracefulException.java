/*******************************************************************************
    Copyright 2008, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.exceptions.transactions;

/**
 * Thrown by the BaseContentionManager when the benchmark time has elapsed.
 **/
public class GracefulException extends java.lang.RuntimeException {
  static final long serialVersionUID = 6572490566353395650L;

  /**
   * Creates a new <code>GracefulException</code> instance with no detail message.
   */
  public GracefulException() {
    super();
  }

}
