/*******************************************************************************
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
******************************************************************************/

package com.sun.fortress.interpreter.evaluator.transactions;

import com.sun.fortress.interpreter.evaluator.tasks.FortressTaskRunner;
import com.sun.fortress.interpreter.evaluator.transactions.exceptions.PanicException;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/**
 * Transaction.java
 * Keeps a transaction's status and contention manager.
 */

public class Transaction {

    /**
     * Possible transaction status
     **/
    public enum Status {ABORTED, ACTIVE, COMMITTED};

    /**
     * Predefined committed transaction
     */
    public static Transaction COMMITTED_TRANS = new Transaction(Status.COMMITTED);
    /**
     * Predefined orted transaction
     */
    public static Transaction ABORTED_TRANS   = new Transaction(Status.ABORTED);

    /**
     * Is transaction waiting for another?
     */
    public boolean waiting = false;

    /** Number of times this transaction tried
     */
    public int attempts = 0;

    /**
     * Number of unique memory references so far.
     */
    public int memRefs = 0;

    /**

    /**
    * Time in nanos when transaction started
    */
    public long startTime = 0;
    /**
     * Time in nanos when transaction committed or aborted
     */
    public long stopTime = 0;

    private long threadID;

    /** Updater for status */
    private AtomicReference<Status> myStatus;

    private ContentionManager manager;

    /**
     * Creates a new, active transaction.
     */
    public Transaction() {
	myStatus = new AtomicReference(Status.ACTIVE);
	long id = this.startTime = System.nanoTime();
	manager = FortressTaskRunner.getContentionManager();
	int numThreads = Runtime.getRuntime().availableProcessors();
	String numThreadsString = System.getenv("FORTRESS_THREADS");
	if (numThreadsString != null)
	    numThreads = Integer.parseInt(numThreadsString);

	threadID = Thread.currentThread().getId() % numThreads;
    }

    public long getThreadId() { return threadID;}
    /**
     * Creates a new transaction with given status.
     * @param myStatus active, committed, or aborted
     */
    private Transaction(Transaction.Status s) {
	myStatus = new AtomicReference(s);
	startTime = 0;
    }

    /**
     * Access the transaction's current status.
     * @return current transaction status
     */
    public Status getStatus() {
	return myStatus.get();
    }

    /**
     * Tests whether transaction is active.
     * @return whether transaction is active
     */
    public boolean isActive() {
	return getStatus() == Status.ACTIVE;
    }

    /**
     * Tests whether transaction is aborted.
     * @return whether transaction is aborted
     */
    public boolean isAborted() {
	return getStatus() == Status.ABORTED;
    }

    /**
     * Tests whether transaction is committed.
     * @return whether transaction is committed
     */
    public boolean isCommitted() {
	return getStatus() == Status.COMMITTED;
    }

    /**
     * Tests whether transaction is committed or active.
     * @return whether transaction is committed or active
     */
    public boolean validate() {
	Status st = getStatus();
	switch (st) {
	case COMMITTED:
	    throw new PanicException("committed transaction still running");
	case ACTIVE:
	    return true;
	case ABORTED:
	    return false;
	default:
	    throw new PanicException("unexpected transaction state: " + getStatus());
	}
    }

    /**
     * Tries to commit transaction
     * @return whether transaction was committed
     */
    public boolean commit() {
	if (myStatus.compareAndSet(Status.ACTIVE, Status.COMMITTED)) 
		       return true;
	else return false;
    }

    /**
     * Tries to abort transaction
     * @return whether transaction was aborted (not necessarily by this call)
     */
    public boolean abort() {
	if (myStatus.compareAndSet(Status.ACTIVE, Status.ABORTED))
		       return true;
	else return false;
    }

    /**
     * Returns a string representation of this transaction
     * @return the string representcodes[ation
     */
    public String toString() {
	switch (getStatus()) {
	case COMMITTED:
	    return "Transaction" + startTime + "[committed]";
	case ABORTED:
	    return "Transaction" + startTime + "[aborted]";
	case ACTIVE:
	    return "Transaction" + startTime + "[active]";
	default:
	    return "Transaction" + startTime + "[???]";
	}
    }

    /**
     * This transaction's contention manager
     * @return the manager
     */
    public ContentionManager getContentionManager() {
	return manager;
    }
}
