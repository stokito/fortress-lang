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

package com.sun.fortress.interpreter.evaluator.transactions;

import java.util.Collection;
/**
 * Interface satisfied by all contention managers
 */
public interface ContentionManager {
  /**
   * Either give the writer a chance to finish it, abort it, or both.
   * @param me Calling transaction.
   * @param other Transaction that's in my way.
   */
  void resolveConflict(Transaction me, Transaction other);

  /**
   * Either give the writer a chance to finish it, abort it, or both.
   * @param me Calling transaction.
   * @param other set of transactions in my way
   */
  void resolveConflict(Transaction me, Collection<Transaction> other);

  /**
   * Assign a priority to caller. Not all managers assign meaningful priorities.
   * @return Priority of conflicting transaction.
   */
  long getPriority();

  /**
   * Change this manager's priority.
   * @param value new priority value
   */
  void setPriority(long value);

  /**
   * Notify manager that object was opened.
   */
  void openSucceeded();

  /**
   * Notify manager that transaction committed.
   */
  void committed();

  /**
   * Wait before trying again
   */

  void waitToRestart();

};
