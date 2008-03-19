/*******************************************************************************
    Copyright 2008 Sun Microsystems, Inc.,
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
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Contention manager for Fortress should be called
 * the 800lb guerilla manager
 */
public class FortressManager2 extends BaseManager {

    public FortressManager2() {
    }

    public void openSucceeded() {
	super.openSucceeded();
    }

    private void sleepFactor(int tid) {
	long factor = tid * tid * tid * tid * 17;
	sleep(factor);
    }

    public void pickOne(Transaction me, Transaction other) {
	int mine = (int) me.getThreadId();
	int yours = (int) other.getThreadId();
	
	if (mine < yours) {
	    if (me.isActive()) {
		other.abort();
	    }
	} else if (mine > yours) {
	    if (other.isActive()) {
		me.abort();
		sleepFactor(mine);
	    }
	} else  return;
    }

    public void pickOne(Transaction me, Collection<Transaction> others) {
	int mine = (int) me.getThreadId();
	int min = mine;
	Transaction win = me;

	for (Transaction t : others) {
	    if (t.isActive()) {
		int id = (int) t.getThreadId();
		if (id < min) {
		    min = id;
		    win = t;
		}
	    }
	}
	if (win == me) {
	    for (Transaction t : others) {
		if (t != me)
		    t.abort();
	    }
	} else {
	    me.abort();
	    sleepFactor(mine);
	}
    }

    public void resolveConflict(Transaction me, Transaction other) {
	if (me == null || other == null || !me.isActive() || !other.isActive()) return;
	pickOne(me, other);
    }

    public void resolveConflict(Transaction me, Collection<Transaction> others) {
	if (me == null || !me.isActive()) return;
	pickOne(me,others);
    }

    public void waitToRestart() {
	int waitTime = 100000;
	sleep(waitTime);
    }

    public void committed() {
    }
}
