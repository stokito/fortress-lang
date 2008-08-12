/********************************************************************************
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
********************************************************************************/

package com.sun.fortress.interpreter.evaluator.tasks;

import com.sun.fortress.interpreter.evaluator.transactions.ContentionManager;
import com.sun.fortress.exceptions.transactions.AbortedException;
import com.sun.fortress.exceptions.transactions.PanicException;
import com.sun.fortress.interpreter.evaluator.transactions.Transaction;

import java.util.concurrent.Callable;
import java.util.HashSet;
import java.util.Set;

public class TaskState {

    // We increment depth when we enter a transaction.  We decrement depth when we end a transaction which happens after
    // commit, or a parent aborts.

    private Transaction transaction;

    public TaskState() {
        transaction = null;
    }

    public TaskState(TaskState ts) {
        transaction = ts.transaction();
    }

    public int transactionNesting() {
        if (transaction == null)
            return 0;
        else return transaction.getNestingDepth();
    }

    /**
     * used for debugging
     * @return string representation of thread state
     */
    public String toString() {
        return "{" + transaction + "}";
    }

    public Transaction transaction() { return transaction;}
    public Boolean inATransaction() { return (transaction != null);}

    /**
     * Can this transaction still commit?
     * This method may be called at any time, not just at transaction end,
     * so we do not clear the onValidateOnce table.
     * @return true iff transaction might still commit
     */
    public boolean validate() {
        try {
            if (transaction == null)
                throw new PanicException(Thread.currentThread().getName() + "Attempting to validate null transaction");
            return transaction.validate();
        } catch (AbortedException ex) {
            return false;
        }
    }

    /**
     * Starts a new transaction.
     */
    public void beginTransaction() {
        if (transaction == null) {
            transaction = new Transaction();
        } else if (!transaction.isActive())
            throw new AbortedException(transaction, "Parent death detected in beginTransaction");
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
}
