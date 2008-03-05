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

    public void pickOne(Transaction me, Transaction other) {
	long mine = me.getThreadId();
	long yours = other.getThreadId();
	
	if (mine < yours) {
	    if (me.isActive()) other.abort();
	} else if (mine > yours) {
	    if (other.isActive()) me.abort();
	} else  return;

    }

    public void pickOne(Transaction me, Collection<Transaction> others) {
	long min = me.getThreadId();
	Transaction win = me;

	for (Transaction t : others) {
	    if (t.isActive()) {
		long id = t.getThreadId();
		if (id < min) {
		    min = id;
		    win = t;
		}
	    }
	}
	if (win == me) {
	    for (Transaction t : others) {
		t.abort();
	    }
	} else {
	    me.abort();
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
	int waitTime = (int) (Math.random() * 65536);
	sleep(waitTime);
    }

    public void committed() {
    }
}
