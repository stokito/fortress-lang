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

import com.sun.fortress.interpreter.evaluator.transactions.util.Random;
import com.sun.fortress.interpreter.evaluator.transactions.ContentionManager;
import com.sun.fortress.interpreter.evaluator.transactions.Transaction;

/**
 * The Chuck Norris contention manager:  always abort other transaction.
 * @author Maurice Herlihy
 */
public class AggressiveManager extends BaseManager {

  public AggressiveManager() {
  }
  public void resolveConflict(Transaction me, Transaction other) {
      other.abort();
  }

  public long getPriority() {
    throw new UnsupportedOperationException();
  }

  public void setPriority(long value) {
    throw new UnsupportedOperationException();
  }
}
