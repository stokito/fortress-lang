/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.evaluator.transactions.manager;

import com.sun.fortress.interpreter.evaluator.transactions.ContentionManager;
import com.sun.fortress.interpreter.evaluator.transactions.Transaction;

import java.util.Collection;

/**
 * @author mph
 */
public class BaseManager implements ContentionManager {
    long priority;

    /**
     * Creates a new instance of BaseManager
     */
    public BaseManager() {
        priority = 0;
    }

    public void resolveConflict(Transaction me, Transaction other) {
    }

    public void resolveConflict(Transaction me, Collection<Transaction> other) {
    }

    public long getPriority() {
        return priority;
    }

    public void setPriority(long value) {
        priority = value;
    }

    public void openSucceeded() {
    }

    /**
     * Local-spin sleep method -- more accurate than Thread.sleep()
     * Difference discovered by V. Marathe.
     */
    protected void sleep(long ms) {
        long startTime = System.currentTimeMillis();
        long stopTime = 0;
        do {
            stopTime = System.currentTimeMillis();
        } while ((stopTime - startTime) < ms);
    }

    public void committed() {
    }

    public void waitToRestart() {
    }

}
