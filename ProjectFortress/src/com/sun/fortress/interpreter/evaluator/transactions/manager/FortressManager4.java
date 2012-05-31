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
 * Similar to FortressManager3, but with better exponential backoff.
 */

public class FortressManager4 extends BaseManager {

    public FortressManager4() {
    }

    private void sleepyTime() {
        FortressTaskRunner runner = (FortressTaskRunner) FortressTaskRunner.currentThread();
        int retries = runner.retries();
        double sleepTime = 0;

        if (retries < 10) {
            sleepTime = Math.random() * Math.pow(2.0, (double) runner.retries()) * 100;
            FortressTaskRunner.debugPrintln("Sleeping: retries = " + retries);
            long current = System.currentTimeMillis();
            sleep((long) sleepTime);
            long stime = System.currentTimeMillis() - current;
            long diff = stime - (long) sleepTime;
            FortressTaskRunner.debugPrintln(
                    "Sleeping done: time = expected = " + sleepTime + "ms real = " + stime + "ms diff = " + diff);
        } else {
            sleepTime = Math.random() * 1024;
            FortressTaskRunner.debugPrintln("Sleeping for real: retries = " + retries);
            long current = System.currentTimeMillis();
            try {
                Thread.sleep((long) sleepTime);
            }
            catch (java.lang.InterruptedException ie) {
            }
            long stime = System.currentTimeMillis() - current;
            long diff = stime - (long) sleepTime;
            FortressTaskRunner.debugPrintln(
                    "Sleeping done expected = " + sleepTime + "ms real = " + stime + "ms diff = " + diff);
        }
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

        //      FortressTaskRunner.debugPrintln("lowerNumbered me = " + me + " you = " + other + " mine = " + mine + " yours = " + yours + " mineLast = " + mineLast + " yoursLast = " + yoursLast);

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
                    FortressTaskRunner.debugPrintln("Resolving conflict between me = " + me + " and other = " + other +
                                                    " by special aborting me");
                    sleepyTime();
                } else {
                    FortressTaskRunner.debugPrintln(
                            "Resolving conflict between me = " + me + " and other = " + other + " by aborting other");
                }
            }
        } else {
            if (other.isActive()) {
                FortressTaskRunner.debugPrintln(
                        "Resolving conflict between me = " + me + " and other = " + other + " by aborting me");
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
            // It's possible that one of my competitors already committed.  We might want to be smarter later, but for now!
            boolean ilose = false;
            FortressTaskRunner.debugPrintln(
                    "Resolving conflict between me = " + me + " and others = " + others + " by aborting them");

            for (Transaction t : others) {
                if (t != me) {
                    t.abort();
                    if (!t.isAborted() || !t.isOrphaned()) ilose = true;
                }
            }
            //          if (ilose) {
            //              me.abort();
            //              FortressTaskRunner.debugPrintln("ReResolving conflict between me = " + me + " and other = " + others + " by aborting me");
            //              sleepyTime();
            //          }
        } else {
            me.abort();
            FortressTaskRunner.debugPrintln(
                    "Resolving conflict between me = " + me + " and other = " + others + " by aborting me");
            sleepyTime();
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
