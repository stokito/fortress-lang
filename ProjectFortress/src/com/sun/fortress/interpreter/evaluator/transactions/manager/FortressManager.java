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

import com.sun.fortress.interpreter.evaluator.transactions.ContentionManager;
import com.sun.fortress.interpreter.evaluator.transactions.Transaction;
import java.util.Collection;

/**
 * Contention manager employing simple exponential backoff.
 * @author Maurice Herlihy
 */
public class FortressManager extends BaseManager {

  public FortressManager() { 
  }

  public void openSucceeded() {
    super.openSucceeded();
  }

  public void resolveConflict(Transaction me, Transaction other) {
      if (other != me) {
	  if (Math.random() > 0.5)
	      other.abort();
	  else me.abort();
      }
  }

  public void resolveConflict(Transaction me, Collection<Transaction> others) {
      me.abort();
  }

  public void waitToRestart() {
    int waitTime = (int) (Math.random() * 65536);
    sleep(waitTime);   
  }

  public void committed() {
  }
}
