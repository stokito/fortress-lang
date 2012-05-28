/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.evaluator.transactions.manager;

import com.sun.fortress.interpreter.evaluator.transactions.ContentionManager;
import com.sun.fortress.interpreter.evaluator.transactions.Transaction;

/**
 * Resolves conflicts by increasing pressure on the transactions that
 * a blocked transaction is waiting on, eventually causing them to
 * erupt through to completion. The way this works is that each time a
 * block is successfully opened, the transaction gains one point of
 * "momentum". When a transaction finds itself blocked by another of
 * higher priority, it adds its momentum (priority) to the other
 * transaction and then waits for the other transaction to complete.
 * Like the Karma manager, Eruption will only wait around so long
 * before clobbering the other transaction and going on anyway; the
 * maximum wait is proportional to the square of the difference in
 * priorities between the two transactions. Of course, at contention
 * time, if the other transaction has a lower priority, we just erupt
 * past it.
 * <p/>
 * The reasoning behind this management scheme is that if a particular
 * transaction is blocking resources critical to many other
 * transactions, it will gain all of their priority in addition to its
 * own and thus be much more likely to finish quickly and get out of
 * the way of all the others.
 * <p/>
 * Note that while a transaction is blocked, other transactions can
 * pile up behind it and increase its priority enough to outweigh the
 * transaction it's blocked behind.
 *
 * @author Bill Scherer
 */

public class EruptionManager extends BaseManager {
    static final int SLEEP_PERIOD = 1000;

    /**
     * Creates a new instance of EruptionManager
     */
    public EruptionManager() {
        priority = 0;
    }

    public void resolveConflict(Transaction me, Transaction other) {
        long transferred = 0;
        ContentionManager otherManager = other.getContentionManager();
        for (int attempts = 0; ; attempts++) {
            long otherPriority = otherManager.getPriority();
            long delta = otherPriority - priority;
            if (delta < 0 || attempts > delta * delta) {
                transferred = 0;
                other.abort();
                return;
            }
            // Unsafe increment, but too expensive to synchronize.
            if (priority > transferred) {
                otherManager.setPriority(otherPriority + priority - transferred);
                transferred = priority;
            }
            if (attempts < delta) {
                sleep(SLEEP_PERIOD);
            }
        }
    }

    public void openSucceeded() {
        priority++;
    }

}
