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

package com.sun.fortress.interpreter.evaluator.transactions.manager;
import com.sun.fortress.interpreter.evaluator.transactions.ContentionManager;
import com.sun.fortress.interpreter.evaluator.transactions.Transaction;
import java.util.Collection;

/**
 *
 * @author mph
 */
public class BaseManager implements ContentionManager {
  long priority;

  /** Creates a new instance of BaseManager */
  public BaseManager() {
    priority = 0;
  }

  public void resolveConflict(Transaction me, Transaction other) {
  }

  public void resolveConflict(Transaction me, Collection<Transaction> other) {
  }

  public long getPriority() {
    return priority;
  }

  public void setPriority(long value) {
    priority = value;
  }

  public void openSucceeded() {
  }

  /**
   * Local-spin sleep method -- more accurate than Thread.sleep()
   * Difference discovered by V. Marathe.
   */
  protected void sleep(int ns) {
    long startTime = System.nanoTime();
    long stopTime = 0;
    do {
      stopTime = System.nanoTime();
    } while((stopTime - startTime) < ns);
  }

  public void committed() {
  }

  public void waitToRestart() {
  }

}
