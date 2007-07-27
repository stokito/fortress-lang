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

import com.sun.fortress.interpreter.evaluator.transactions.Transaction;

/**
 * Tries to keep a maximal independent set running.
 * If prior transaction is
 *		waiting or lower priority, then abort it.
 *		otherwise, wait for it to commit, abort, or wait
 * Complete description in
 *		Rachid Guerraoui, Maurice Herlihy, and Sebastian Pochon, Toward a Theory of Transactional Contention Management,
 *		Proceedings of the Twenty-Fourth Annual Symposium on Principles of Distributed Computing (PODC).
 *		Las Vegas, NV July 2005.
 * @author Maurice Herlihy
 */
public class GreedyManager extends BaseManager {
  public void resolveConflict(Transaction me, Transaction other) {
    if (other.waiting || other.startTime < me.startTime) {
      other.abort();
    } else {
      me.waiting = true;		// I'm waiting
      other.waitWhileActiveNotWaiting();
      me.waiting = false;		// I'm no longer waiting
    }
  }
  /**
   * Reset priority only on commit. On abort, restart with previous priority.
   **/
  public void committed() {
    setPriority(0);
  }

  public void openSucceeded() {
    setPriority(getPriority() + 1);
  }
}
