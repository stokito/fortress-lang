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
public class FortressManager2 extends BaseManager {

    public FortressManager2() {
    }

    public void openSucceeded() {
        super.openSucceeded();
    }

    // This is completely arbitrary.
    private void sleepFactor(int threadid) {
        int factor = threadid % 2048;
        sleep(factor);
    }

    public void pickOne(Transaction me, Transaction other) {
        int mine = (int) me.getID();
        int yours = (int) other.getID();

        if (mine < yours) {
            if (me.isActive()) {
                FortressTaskRunner.debugPrintln(
                        "Resolve Conflict between " + me + " and " + other + " by killing " + other);
                if (!other.abort())
                    // My competitor already committed.  We might want to be smarter later, but for now!
                    me.abort();
            }
        } else if (mine > yours) {
            if (other.isActive()) {
                FortressTaskRunner.debugPrintln(
                        "Resolve Conflict between " + me + " and " + other + " by killing " + me);
                me.abort();
                sleepFactor(mine);
            }
        } else return;
    }

    public void pickOne(Transaction me, Collection<Transaction> others) {
        int mine = (int) me.getID();
        int min = mine;
        Transaction win = me;

        for (Transaction t : others) {
            if (t.isActive()) {
                int id = (int) t.getID();
                if (id < min) {
                    min = id;
                    win = t;
                }
            }
        }
        if (win == me) {
            FortressTaskRunner.debugPrintln("Resolve Conflict between " + me + " and others by killing them");
            // It's possible that one of my competitors already committed.  We might want to be smarter later, but for now!
            boolean ilose = false;

            for (Transaction t : others) {
                if (t != me) {
                    FortressTaskRunner.debugPrintln(
                            "Resolve Conflict between " + me + " and others by killing them including " + t);
                    t.abort();
                    if (!t.isAborted()) ilose = true;
                }
            }
            if (ilose) me.abort();
        } else {
            FortressTaskRunner.debugPrintln("Resolve Conflict between " + me + " and others by killing me ");
            for (Transaction t : others) {
                FortressTaskRunner.debugPrintln("Others still active include " + t);
            }
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
        pickOne(me, others);
    }

    public void waitToRestart() {
        int waitTime = 100000;
        sleep(waitTime);
    }

    public void committed() {
    }
}
