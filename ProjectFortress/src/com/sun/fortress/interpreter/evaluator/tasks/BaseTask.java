/********************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ********************************************************************************/

package com.sun.fortress.interpreter.evaluator.tasks;

import com.sun.fortress.exceptions.transactions.AbortedException;
import com.sun.fortress.exceptions.transactions.PanicException;
import com.sun.fortress.interpreter.evaluator.transactions.Transaction;
import jsr166y.RecursiveAction;

import java.util.concurrent.atomic.AtomicInteger;

public abstract class BaseTask extends RecursiveAction {
    Throwable err = null;
    Transaction transaction = null;
    final private int depth;
    final private BaseTask parent;

    // Debugging
    private static Boolean debug = false;
    private static AtomicInteger counter = new AtomicInteger();
    private int count;
    String name;

    public BaseTask(BaseTask p) {
        parent = p;
        transaction = p.transaction();
        depth = p.depth() + 1;
        if (debug) {
            count = counter.getAndIncrement();
            name = p.name() + "." + count;
        } else {
            name = "BaseTask";
            count = 0;
        }
    }

    public int depth() {
        return depth;
    }

    public int count() {
        return count;
    }

    public String name() {
        return name;
    }

    // For primordial evaluator task
    public BaseTask() {
        parent = null;
        transaction = null;
        depth = 0;
        count = 0;
        name = "0";
    }

    public BaseTask parent() {
        return parent;
    }

    public int transactionNesting() {
        if (transaction == null) return 0;
        else return transaction.getNestingDepth();
    }

    public Transaction transaction() {
        return transaction;
    }

    public Boolean inATransaction() {
        return (transaction != null);
    }

    /**
     * Can this transaction still commit?
     * This method may be called at any time, not just at transaction end,
     * so we do not clear the onValidateOnce table.
     *
     * @return true iff transaction might still commit
     */
    public boolean validate() {
        try {
            if (transaction == null) throw new PanicException(
                    Thread.currentThread().getName() + "Attempting to validate null transaction");
            return transaction.validate();
        }
        catch (AbortedException ex) {
            return false;
        }
    }

    /**
     * Starts a new transaction.
     */
    public void beginTransaction() {
        if (transaction == null) {
            transaction = new Transaction();
        } else if (!transaction.isActive()) throw new AbortedException(transaction,
                                                                       "Parent death detected in beginTransaction");
        else {
            Transaction parent = transaction;
            Transaction child = new Transaction(transaction);
            transaction = child;

            if (!parent.isActive()) {
                child.abort();
                transaction = parent;
                throw new AbortedException(parent, "Parent death detected in beginTransaction");
            }
        }
    }

    /**
     * Attempts to commit the current transaction of the invoking
     * <code>Thread</code>.
     *
     * @return whether commit succeeded.
     */
    public boolean commitTransaction() {
        if (validate() && transaction.commit()) {
            return true;
        }
        abortTransaction();
        return false;
    }

    /**
     * Aborts the current transaction of the invoking <code>Thread</code>.
     * Does not end transaction, but ensures it will never commit.
     */
    public void abortTransaction() {
        if (transaction != null && transaction.isActive()) {
            transaction.abort();
        }
    }

    public void giveUpTransaction() {
        if (transaction == null) {
            throw new PanicException(Thread.currentThread().getName() + "Giving up null transaction");
        }
        //    if (transaction.isActive()) {
        //        throw new PanicException(Thread.currentThread().getName() + "How did an active transaction escape?");
        //    }
        transaction = transaction.getParent();
    }

    public void recordException(Throwable t) {
        err = t;
    }

    public boolean causedException() {
        return err != null;
    }

    public Throwable taskException() {
        return err;
    }

    public abstract void print();

    public static void printTaskTrace() {
        BaseTask task = FortressTaskRunner.getTask();
        while (task != null) {
            task.print();
            task = task.parent();
        }
    }

    public String toString() {
        return "[BaseTask" + name() + ":" + transaction + "]";
    }

}
