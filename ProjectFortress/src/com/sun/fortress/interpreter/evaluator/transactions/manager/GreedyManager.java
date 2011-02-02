/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

/* Based on Backoff Manager */

package com.sun.fortress.interpreter.evaluator.transactions.manager;

import com.sun.fortress.interpreter.evaluator.transactions.Transaction;

import java.util.Collection;

/**
 * Greedy algorithm
 * There is an interesting interaction between work stealing and transactions.
 * Until we can figure out a way to boost the priority of the transaction keeping
 * everyone else waiting at a join point, this will have to do.
 */

public class GreedyManager extends BaseManager {

    public GreedyManager() {
    }

    public void pickOne(Transaction me, Transaction other) {
        other.abort();
    }

    public void pickOne(Transaction me, Collection<Transaction> others) {
        for (Transaction t : others) {
            t.abort();
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
