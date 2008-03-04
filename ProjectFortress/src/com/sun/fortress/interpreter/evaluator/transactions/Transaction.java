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

  // generate unique ids
  private static AtomicInteger unique = new AtomicInteger(100);

  private long threadID;

  /** Updater for status */
  private static final
      AtomicReferenceFieldUpdater<Transaction, Status>
      statusUpdater = AtomicReferenceFieldUpdater.newUpdater
      (Transaction.class, Status.class, "status");

  private volatile Status status;

  private long id;

  private ContentionManager manager;

  /**
   * Creates a new, active transaction.
   */
  public Transaction() {
    status = Status.ACTIVE;
    id = this.startTime = System.nanoTime();
    manager = FortressTaskRunner.getContentionManager();
    threadID = Thread.currentThread().getId();
  }

  public long getThreadId() { return threadID;}
  /**
   * Creates a new transaction with given status.
   * @param myStatus active, committed, or aborted
   */
  private Transaction(Transaction.Status myStatus) {
    this.status = myStatus;
    this.startTime = 0;
  }

  /**
   * Access the transaction's current status.
   * @return current transaction status
   */
  public Status getStatus() {
    return status;
  }

  /**
   * Tests whether transaction is active.
   * @return whether transaction is active
   */
  public boolean isActive() {
    return this.getStatus() == Status.ACTIVE;
  }

  /**
   * Tests whether transaction is aborted.
   * @return whether transaction is aborted
   */
  public boolean isAborted() {
      return this.getStatus() == Status.ABORTED;
  }

  /**
   * Tests whether transaction is committed.
   * @return whether transaction is committed
   */
  public boolean isCommitted() {
    return (this.getStatus() == Status.COMMITTED);
  }

  /**
   * Tests whether transaction is committed or active.
   * @return whether transaction is committed or active
   */
  public boolean validate() {
    Status st = this.getStatus();
    switch (st) {
      case COMMITTED:
        throw new PanicException("committed transaction still running");
      case ACTIVE:
        return true;
      case ABORTED:
        return false;
      default:
        throw new PanicException("unexpected transaction state: " + status);
    }
  }

  /**
   * Tries to commit transaction
   * @return whether transaction was committed
   */
  public boolean commit() {
    try {
      while (this.getStatus() == Status.ACTIVE) {
        if (statusUpdater.compareAndSet(this,
            Status.ACTIVE,
            Status.COMMITTED)) {
          return true;
        }
      }
      return false;
    } finally {
      wakeUp();
    }
  }

  /**
   * Tries to abort transaction
   * @return whether transaction was aborted (not necessarily by this call)
   */
  public boolean abort() {
    try {
      while (this.getStatus() == Status.ACTIVE) {
        if (statusUpdater.compareAndSet(this, Status.ACTIVE, Status.ABORTED)) {
          return true;
        }
      }
      return this.getStatus() == Status.ABORTED;
    } finally {
      wakeUp();
    }
  }

  /**
   * Returns a string representation of this transaction
   * @return the string representcodes[ation
   */
  public String toString() {
    switch (this.status) {
      case COMMITTED:
        return "Transaction" + this.startTime + "[committed]";
      case ABORTED:
        return "Transaction" + this.startTime + "[aborted]";
      case ACTIVE:
        return "Transaction" + this.startTime + "[active]";
      default:
        return "Transaction" + this.startTime + "[???]";
    }
  }

  /**
   * Block caller while transaction is active.
   */
  public synchronized void waitWhileActive() {
    while (this.getStatus() == Status.ACTIVE) {
      try {
        wait();
      } catch (InterruptedException ex) {}
    }
  }
  /**
   * Block caller while transaction is active.
   */
  public synchronized void waitWhileActiveNotWaiting() {
    while (getStatus() == Status.ACTIVE && !waiting) {
      try {
        wait();
      } catch (InterruptedException ex) {}
    }
  }

  /**
   * Wake up any transactions waiting for this one to finish.
   */
  public synchronized void wakeUp() {
    notifyAll();
  }

  /**
   * This transaction's contention manager
   * @return the manager
   */
  public ContentionManager getContentionManager() {
    return manager;
  }
}
