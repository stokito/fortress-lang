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
import com.sun.fortress.interpreter.evaluator.transactions.exceptions.PanicException;
import com.sun.fortress.interpreter.evaluator.transactions.Transaction;

import java.util.concurrent.Callable;
import java.util.HashSet;
import java.util.Set;

public class ThreadState {
  // We increment depth when we enter a transaction.  We decrement depth when we end a transaction which happens after
  // commit.

    int depth = 0;
    final ContentionManager manager;

    long committed = 0;        // number of committed transactions
    long total = 0;            // total number of transactions
    long committedMemRefs = 0; // number of committed reads and writes
    long totalMemRefs = 0;     // total number of reads and writes

    Set<Callable<Boolean>> onValidate = new HashSet<Callable<Boolean>>();
    Set<Runnable>          onCommit   = new HashSet<Runnable>();
    Set<Runnable>          onBegin    = new HashSet<Runnable>();
    Set<Runnable>          onAbort    = new HashSet<Runnable>();
    Set<Callable<Boolean>> onValidateOnce = new HashSet<Callable<Boolean>>();
    Set<Runnable>          onBeginOnce    = new HashSet<Runnable>();
    Set<Runnable>          onCommitOnce   = new HashSet<Runnable>();
    Set<Runnable>          onAbortOnce    = new HashSet<Runnable>();

    Transaction transaction = null;

    private void incDepth() {
        depth++;
    }

    private void decDepth() {
        depth--;
    }

    /**
     * Creates new ThreadState
     */
    public ThreadState() {
        try {
            manager = (ContentionManager)FortressTaskRunner.contentionManagerClass.newInstance();
        } catch (NullPointerException e) {
            throw new PanicException("No default contention manager class set.");
        } catch (Exception e) {  // Some problem with instantiation
            throw new PanicException(e);
        }
    }

    /* Are we in a transaction? */
    public int transactionNesting() { return depth;}

    /**
     * Resets any metering information (commits/aborts, etc).
     */
    public void reset() {
        committed = 0;        // number of committed transactions
        total = 0;            // total number of transactions
        committedMemRefs = 0; // number of committed reads and writes
        totalMemRefs = 0;     // total number of reads and writes
    }

    public void incCommitted(int num) { committed += num;}
    public void incTotal(int num) {total+= num;}
    public void incCommittedMemRefs(int num) { committedMemRefs += num;}
    public void incTotalMemRefs(int num) {totalMemRefs += num;}

    /**
     * used for debugging
     * @return string representation of thread state
     */
    public String toString() {
        return
            "Thread" + Thread.currentThread().getName() + "["+
            "transaction:" + transaction + "," +
            "committed: " + committed + "," +
            "aborted: " + ( total -  committed) +
            "depth: " + depth +
            "]";
    }

    public Transaction transaction() { return transaction;}
    public ContentionManager manager() { return manager;}
    public void addToTotalMemRefs(long num) { totalMemRefs += num;}
    public long memRefs() { return totalMemRefs;}
    public void addToCommittedMemRefs(long num) { committedMemRefs += num;}
    public long commitedMemRefs() { return committedMemRefs;}
    public void incAttempts() { transaction.attempts++;}
    public long attempts() { return transaction.attempts;}

    /**
     * Can this transaction still commit?
     * This method may be called at any time, not just at transaction end,
     * so we do not clear the onValidateOnce table.
     * @return true iff transaction might still commit
     */
    public boolean validate() {
        try {
            // permanent
            for (Callable<Boolean> v : onValidate) {
                if (!v.call()) {
                    return false;
                }
            }
            // temporary
            for (Callable<Boolean> v : onValidateOnce) {
                if (!v.call()) {
                    return false;
                }
            }
            onValidateOnce.clear();
            return transaction.validate();
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * Call methods registered to be called on transaction begin.
     */
    public void runBeginHandlers() {
        try {
            // permanent
            for (Runnable r: onBegin) {
                r.run();
            }
            // temporary
            for (Runnable r: onBeginOnce) {
                r.run();
            }
            onBeginOnce.clear();
        } catch (Exception ex) {
            throw new PanicException(ex);
        }
    }
    /**
     * Call methods registered to be called on commit.
     */
    public void runCommitHandlers() {
        try {
            // permanent
            for (Runnable r: onCommit) {
                r.run();
            }
            // temporary
            for (Runnable r: onCommitOnce) {
                r.run();
            }
            onCommitOnce.clear();
        } catch (Exception ex) {
            throw new PanicException(ex);
        }
    }

    /**
     * Starts a new transaction.  Cannot nest transactions deeper than
     * <code>Thread.MAX_NESTING_DEPTH.</code> The contention manager of the
     * invoking thread is notified when a transaction is begun.
     */
    public void beginTransaction() {
        if (depth == 0) {
            transaction = new Transaction();
            total++;
        }
        incDepth();
    }

    /**
     * Attempts to commit the current transaction of the invoking
     * <code>Thread</code>.  Always succeeds for nested
     * transactions.  The contention manager of the invoking thread is
     * notified of the result.  If the transaction does not commit
     * because a <code>TMObject</code> opened for reading was
     * invalidated, the contention manager is also notified of the
     * inonValidate.
     *
     *
     * @return whether commit succeeded.
     */
    public boolean commitTransaction() {
        if (depth < 1) {
            throw new PanicException(Thread.currentThread().getName() + " commitTransaction invoked when no transaction active.");
        } else if (depth > 1) {
            return validate();
        } else  {
            if (validate() && transaction.commit()) {
                committed++;
                runCommitHandlers();
                transaction = null;
                return true;
            }
            abortTransaction();
            return false;
        }
    }

    /**
     * Aborts the current transaction of the invoking <code>Thread</code>.
     * Does not end transaction, but ensures it will never commit.
     */
    public void abortTransaction() {
        runAbortHandlers();
        transaction.abort();
    }

    public void endTransaction() {
        decDepth();
        if (depth == 0 && transaction != null) {
            if (transaction.isActive()) {
                abortTransaction();
                transaction = null;
            } else if (transaction.isAborted()) {
                transaction = null;
            }
        }
    }

    /**
     * Call methods registered to be called on commit.
     */
    public void runAbortHandlers() {
        try {
            // permanent
            for (Runnable r: onAbort) {
                r.run();
            }
            // temporary
            for (Runnable r: onAbortOnce) {
                r.run();
            }
            onAbortOnce.clear();
        } catch (Exception ex) {
            throw new PanicException(ex);
        }
    }
}



