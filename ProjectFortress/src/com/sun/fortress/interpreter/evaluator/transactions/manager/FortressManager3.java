/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

/* Based on Backoff Manager */

package com.sun.fortress.interpreter.evaluator.transactions.manager;

import com.sun.fortress.interpreter.evaluator.tasks.FortressTaskRunner;
import com.sun.fortress.interpreter.evaluator.transactions.Transaction;

import java.util.Collection;

/**
 * Contention manager for Fortress should be called
 * the 800lb guerilla manager
 */
public class FortressManager3 extends BaseManager {

    public FortressManager3() {
    }

    // This is completely arbitrary.
    private void sleepFactor(int threadid) {
        int factor = threadid % 2048;
        sleep(factor);
    }

    public Transaction lowerNumbered(Transaction me, Transaction other) {
        Transaction mine = me;
        Transaction yours = other;

        while (yours.getNestingDepth() < mine.getNestingDepth()) {
            mine = mine.getParent();
        }

        while (mine.getNestingDepth() < yours.getNestingDepth()) {
            yours = yours.getParent();
        }

        int myId = (int) mine.getID();
        int yourId = (int) yours.getID();

        if (myId < yourId) return other;
        else return me;
    }

    public void pickOne(Transaction me, Transaction other) {
        Transaction winner = lowerNumbered(me, other);
        Transaction loser;
        if (winner == me) loser = other;
        else loser = me;

        if (winner.isActive()) {
            if (!loser.abort()) {
                // My competitor already committed.
                winner.abort();
            }
        }
    }

    public void pickOne(Transaction me, Collection<Transaction> others) {

        Transaction winner = me;
        for (Transaction t : others) {
            if (t.isActive()) {
                winner = lowerNumbered(winner, t);
            }
        }

        if (winner == me) {
            // It's possible that one of my competitors already committed.  We might want to be smarter later, but for now!
            boolean ilose = false;

            for (Transaction t : others) {
                if (t != me) {
                    t.abort();
                    if (!t.isAborted()) ilose = true;
                }
            }
            if (ilose) me.abort();
        } else {
            FortressTaskRunner.yield();
            me.abort();
        }
    }

    public void resolveConflict(Transaction me, Transaction other) {
        if (me == null || other == null || !me.isActive() || !other.isActive()) return;
        pickOne(me, other);
    }

    public void resolveConflict(Transaction me, Collection<Transaction> others) {
        if (me == null || !me.isActive()) return;
        pickOne(me, others);
    }
}
