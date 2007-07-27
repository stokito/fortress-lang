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
import java.util.Collection;

/**
 * Contention manager employing simple exponential backoff.
 * @author Maurice Herlihy
 */
public class BackoffManager extends BaseManager {
  static final int MIN_LOG_BACKOFF = 4;
  static final int MAX_LOG_BACKOFF = 26;
  static final int MAX_RETRIES = 22;

  Random random;

  int currentAttempt = 0;

  public BackoffManager() {
    random = new Random();
  }
  public void openSucceeded() {
    super.openSucceeded();
    currentAttempt = 0;
  }
  public void resolveConflict(Transaction me, Transaction other) {
    if (currentAttempt <= MAX_RETRIES) {
      if (!other.isActive()) {
        return;
      }
      int logBackoff = currentAttempt - 2 + MIN_LOG_BACKOFF;
      if (logBackoff > MAX_LOG_BACKOFF) {
        logBackoff = MAX_LOG_BACKOFF;
      }
      int sleep = random.nextInt(1 << logBackoff);
      try {
        Thread.sleep(sleep/1000000, sleep % 1000000);
      } catch (InterruptedException ex) {
      }
      currentAttempt++;
    } else {
      other.abort();
      currentAttempt = 0;
    }
  }
  public void resolveConflict(Transaction me, Collection<Transaction> others) {
    if (currentAttempt <= MAX_RETRIES) {
      int logBackoff = currentAttempt - 2 + MIN_LOG_BACKOFF;
      if (logBackoff > MAX_LOG_BACKOFF) {
        logBackoff = MAX_LOG_BACKOFF;
      }
      int sleep = random.nextInt(1 << logBackoff);
      try {
        Thread.sleep(sleep/1000000, sleep % 1000000);
      } catch (InterruptedException ex) {
      }
      currentAttempt++;
    } else {
      for (Transaction other : others) {
        if (other.isActive() && other != me) {
          other.abort();
        }
      }
      currentAttempt = 0;
    }
  }
}
