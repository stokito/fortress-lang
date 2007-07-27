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

/**
 * Older transaction always wins.
 * @author Maurice Herlihy
 */
public class PriorityManager extends BaseManager {

  public PriorityManager() {
  }
  public void resolveConflict(Transaction me, Transaction other) {
    if (me.startTime <= other.startTime) {
      other.abort();
    } else {
      other.waitWhileActive();
    }
  }

  public long getPriority() {
    throw new UnsupportedOperationException();
  }

  public void setPriority(long value) {
    throw new UnsupportedOperationException();
  }
}
