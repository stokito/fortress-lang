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

/* Based on Backoff Manager */

package com.sun.fortress.interpreter.evaluator.transactions.manager;

import com.sun.fortress.interpreter.evaluator.transactions.util.Random;
import com.sun.fortress.interpreter.evaluator.transactions.ContentionManager;
import com.sun.fortress.interpreter.evaluator.transactions.Transaction;
import java.util.Collection;

/**
 * Contention manager employing simple exponential backoff.
 * @author Maurice Herlihy
 */
public class FortressManager extends BaseManager {
  static final int MIN_LOG_BACKOFF = 4;
  static final int MAX_LOG_BACKOFF = 26;
  static final int MAX_RETRIES = 64;

  Random random;

  int currentAttempt = 0;

  public FortressManager() {
    random = new Random();
  }
  public void openSucceeded() {
    super.openSucceeded();
    currentAttempt = 0;
  }

  public void resolveConflict(Transaction me, Transaction other) {
      if (other.isActive() && other.startTime >  me.startTime) {
	  other.abort();
      }
  }

  public void resolveConflict(Transaction me, Collection<Transaction> others) {
    boolean beforeMe = false;
    Transaction min = me;
    for (Transaction other : others) {
	if (other.isActive() && other.startTime < min.startTime) min = other;
    }

    if (min != me) me.abort();
    for (Transaction other : others) {
	if (other.isActive() && other != min) other.abort();
    }
  }
}
