/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.evaluator.transactions.manager;

import com.sun.fortress.interpreter.evaluator.transactions.Transaction;
import com.sun.fortress.interpreter.evaluator.transactions.util.FortressRandom;

import java.util.Collection;

/**
 * Contention manager employing simple exponential backoff.
 *
 * @author Maurice Herlihy
 */
public class BackoffManager extends BaseManager {
    static final int MIN_LOG_BACKOFF = 4;
    static final int MAX_LOG_BACKOFF = 26;
    static final int MAX_RETRIES = 22;

    FortressRandom random;

    int currentAttempt = 0;

    public BackoffManager() {
        random = new FortressRandom();
    }

    public void openSucceeded() {
        super.openSucceeded();
        currentAttempt = 0;
    }

    public void resolveConflict(Transaction me, Transaction other) {
        if (currentAttempt <= MAX_RETRIES) {
            if (!other.isActive()) {
                return;
            }
            int logBackoff = currentAttempt - 2 + MIN_LOG_BACKOFF;
            if (logBackoff > MAX_LOG_BACKOFF) {
                logBackoff = MAX_LOG_BACKOFF;
            }
            int sleep = random.nextInt(1 << logBackoff);
            try {
                Thread.sleep(sleep / 1000000, sleep % 1000000);
            }
            catch (InterruptedException ex) {
            }
            currentAttempt++;
        } else {
            other.abort();
            currentAttempt = 0;
        }
    }

    public void resolveConflict(Transaction me, Collection<Transaction> others) {
        if (currentAttempt <= MAX_RETRIES) {
            int logBackoff = currentAttempt - 2 + MIN_LOG_BACKOFF;
            if (logBackoff > MAX_LOG_BACKOFF) {
                logBackoff = MAX_LOG_BACKOFF;
            }
            int sleep = random.nextInt(1 << logBackoff);
            try {
                Thread.sleep(sleep / 1000000, sleep % 1000000);
            }
            catch (InterruptedException ex) {
            }
            currentAttempt++;
        } else {
            for (Transaction other : others) {
                if (other.isActive() && other != me) {
                    other.abort();
                }
            }
            currentAttempt = 0;
        }
    }
}
