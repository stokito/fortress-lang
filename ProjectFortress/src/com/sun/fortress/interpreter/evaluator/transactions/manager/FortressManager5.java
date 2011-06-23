/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
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
 * We prefer the lowest numbered transaction unless we've retried
 * too often, then we get mad (greedy).
 */

public class FortressManager5 extends BaseManager {

    private final static int MaxRetries = 5;

    public FortressManager5() {
    }

    private void sleepyTime() {
        FortressTaskRunner runner = (FortressTaskRunner) FortressTaskRunner.currentThread();
        double sleepTime = 0;

        sleepTime = Math.random() * Math.pow(2.0, (double) runner.retries());
        //long current = System.currentTimeMillis();
        sleep((long) sleepTime);
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

        Transaction mineLast = mine;
        Transaction yoursLast = yours;

        while (yours != mine) {
            mineLast = mine;
            yoursLast = yours;
            mine = mine.getParent();
            yours = yours.getParent();
        }

        mine = mineLast;
        yours = yoursLast;

        int myId = (int) mine.getID();
        int yourId = (int) yours.getID();

        if (myId < yourId) return me;
        else return other;
    }

    public void pickOne(Transaction me, Transaction other) {
        Transaction winner = lowerNumbered(me, other);
        Transaction loser;
        if (winner == me) {
            if (me.isActive()) {
                if (!other.abort()) {
                    me.abort();
                    sleepyTime();
                }
            }
        } else {
            if (other.isActive()) {
                me.abort();
                sleepyTime();
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
            for (Transaction t : others) {
                if (t != me) {
                    t.abort();
                }
            }
        } else {
            me.abort();
            sleepyTime();
        }
    }

    public void resolveConflict(Transaction me, Transaction other) {
        if (me == null || other == null || !me.isActive() || !other.isActive()) return;
        FortressTaskRunner runner = (FortressTaskRunner) FortressTaskRunner.currentThread();
        //int retries = runner.retries();
        if (runner.retries() > 5) {
            FortressTaskRunner.debugPrintln("Going GREEDY");
            other.abort();
        } else pickOne(me, other);
    }

    public void resolveConflict(Transaction me, Collection<Transaction> others) {
        if (me == null || !me.isActive()) return;
        FortressTaskRunner runner = (FortressTaskRunner) FortressTaskRunner.currentThread();
        //int retries = runner.retries();
        if (runner.retries() > 10) {
            FortressTaskRunner.debugPrintln("Going GREEDY");
            for (Transaction t : others) {
                t.abort();
            }
        } else pickOne(me, others);
    }
}
