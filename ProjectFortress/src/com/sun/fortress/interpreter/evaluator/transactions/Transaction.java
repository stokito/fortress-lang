/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.evaluator.transactions;

import com.sun.fortress.exceptions.transactions.PanicException;
import com.sun.fortress.interpreter.env.ReferenceCell;
import com.sun.fortress.interpreter.evaluator.tasks.FortressTaskRunner;
import com.sun.fortress.interpreter.evaluator.values.FValue;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Transaction.java
 * Keeps a transaction's status and contention manager.
 */

public class Transaction {

    /**
     * Possible transaction status
     */
    private enum Status {
        ORPHANED, ABORTED, ACTIVE, COMMITTED
    }

    ;
    private Transaction parent;
    private volatile List<Transaction> children;
    /**
     * Updater for status
     */
    private volatile AtomicReference<Status> myStatus;
    private int nestingDepth;
    private int count;
    private static AtomicInteger counter = new AtomicInteger();

    public static final boolean debug = false;

    // Used for debugging
    private ConcurrentHashMap<ReferenceCell, ConcurrentHashMap<FortressTaskRunner, String>> updates;


    /**
     * Creates a new, active transaction.
     */
    public Transaction() {
        parent = null;
        children = new ArrayList<Transaction>();
        nestingDepth = 0;
        myStatus = new AtomicReference(Status.ACTIVE);
        count = counter.getAndIncrement();

        if (debug) {
            updates = new ConcurrentHashMap<ReferenceCell, ConcurrentHashMap<FortressTaskRunner, String>>();
        }
    }

    public Transaction(Transaction p) {
        parent = p;
        children = new ArrayList<Transaction>();
        count = counter.getAndIncrement();

        if (debug) {
            updates = new ConcurrentHashMap<ReferenceCell, ConcurrentHashMap<FortressTaskRunner, String>>();

        }

        if (p == null) {
            nestingDepth = 0;
            myStatus = new AtomicReference(Status.ACTIVE);
        } else if (p.isActive()) {
            nestingDepth = p.getNestingDepth() + 1;
            myStatus = new AtomicReference(Status.ACTIVE);
            p.addChild(this);
        } else {
            nestingDepth = p.getNestingDepth() + 1;
            myStatus = new AtomicReference(Status.ORPHANED);
        }

    }

    // Used for debugging, called from ReferenceCell
    public void addRead(ReferenceCell rc, FValue f) {
        if (debug) {
            FortressTaskRunner runner = (FortressTaskRunner) Thread.currentThread();
            updates.putIfAbsent(rc, new ConcurrentHashMap<FortressTaskRunner, String>());
            ConcurrentHashMap<FortressTaskRunner, String> m = updates.get(rc);
            if (!m.containsKey(runner)) {
                m.put(runner, "(read " + f + ")");
            } else {
                m.put(runner, m.get(runner) + "(read " + f + ")");
            }
        }
    }

    // Used for debugging, called from ReferenceCell
    public void addWrite(ReferenceCell rc, FValue f) {
        if (debug) {
            FortressTaskRunner runner = (FortressTaskRunner) Thread.currentThread();
            updates.putIfAbsent(rc, new ConcurrentHashMap<FortressTaskRunner, String>());
            ConcurrentHashMap<FortressTaskRunner, String> m = updates.get(rc);
            if (!m.containsKey(runner)) {
                m.put(runner, "( write " + f + ")");
            } else {
                String temp = m.get(runner) + "(write " + f + ")";
                m.put(runner, temp);
            }
        }
    }

    // Used for debugging, called from ReferenceCell
    public void mergeUpdates(String s, Transaction t, ReferenceCell rc) {
        if (debug) {
            FortressTaskRunner runner = (FortressTaskRunner) Thread.currentThread();
            updates.putIfAbsent(rc, new ConcurrentHashMap<FortressTaskRunner, String>());
            ConcurrentHashMap<FortressTaskRunner, String> m = updates.get(rc);
            if (!m.containsKey(runner)) m.put(runner, s);
            else m.put(runner, m.get(runner) + s);
        }
    }

    public int getNestingDepth() {
        return nestingDepth;
    }

    public long getID() {
        return count;
    }


    /* Access the transaction's current status.
     * @return current transaction status
     */
    private Status getStatus() {
        return myStatus.get();
    }

    /**
     * Tests whether transaction is active.
     *
     * @return whether transaction is active
     */
    public boolean isActive() {
        return getStatus() == Status.ACTIVE;
    }

    /**
     * Tests whether transaction is aborted.
     *
     * @return whether transaction is aborted
     */
    public boolean isAborted() {
        return getStatus() == Status.ABORTED;
    }

    /**
     * Tests whether transaction is committed.
     *
     * @return whether transaction is committed
     */
    public boolean isCommitted() {
        return getStatus() == Status.COMMITTED;
    }

    /**
     * Tests whether transaction is abandoned
     *
     * @return whether transaction is abandoned
     */

    public boolean isOrphaned() {
        return getStatus() == Status.ORPHANED;
    }

    public boolean addChild(Transaction c) {
        if (isActive()) {
            synchronized (children) {
                children.add(c);
            }
            return true;
        } else return false;
    }

    public Transaction getParent() {
        return parent;
    }

    /**
     * Tests whether transaction is committed or active.
     *
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
            case ORPHANED:
                return false;
            default:
                throw new PanicException("unexpected transaction state: " + getStatus());
        }
    }

    /**
     * Tries to commit transaction
     *
     * @return whether transaction was committed
     */
    public boolean commit() {
        while (myStatus.get() == Status.ACTIVE) {
            if (myStatus.compareAndSet(Status.ACTIVE, Status.COMMITTED)) {
                if (debug) {
                    if (parent == null) {
                        Enumeration<ReferenceCell> temp = updates.keys();
                        while (temp.hasMoreElements()) {
                            ReferenceCell key = temp.nextElement();
                            ConcurrentHashMap<FortressTaskRunner, String> ups = updates.get(key);
                            FortressTaskRunner runner = (FortressTaskRunner) Thread.currentThread();
                            String mine = ups.get(runner);
                        }
                    } else {
                        Enumeration<ReferenceCell> temp = updates.keys();
                        while (temp.hasMoreElements()) {
                            ReferenceCell key = temp.nextElement();
                            ConcurrentHashMap<FortressTaskRunner, String> ups = updates.get(key);
                            FortressTaskRunner runner = (FortressTaskRunner) Thread.currentThread();
                            String mine = ups.get(runner);
                            parent.mergeUpdates(mine, this, key);
                        }
                    }
                }

                synchronized (children) {
                    children.clear();
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Tries to abort transaction
     *
     * @return whether transaction was aborted (not necessarily by this call)
     */
    public boolean abort() {
        while (myStatus.get() == Status.ACTIVE) {
            if (myStatus.compareAndSet(Status.ACTIVE, Status.ABORTED)) {
                synchronized (children) {
                    for (Transaction child : children) {
                        child.orphan();
                    }
                    children.clear();
                }
                return true;
            }
        }
        if (isActive()) throw new RuntimeException("Transaction " + this + " is active and didn't get aborted ?");
        return false;
    }

    public boolean orphan() {
        if (myStatus.compareAndSet(Status.ACTIVE, Status.ORPHANED)) {
            synchronized (children) {
                for (Transaction child : children) {
                    child.orphan();
                }
                children.clear();
            }
            return true;
        } else if (myStatus.compareAndSet(Status.ABORTED, Status.ORPHANED)) {
            synchronized (children) {
                for (Transaction child : children) {
                    child.orphan();
                }
            }
            return false;
        } else if (myStatus.compareAndSet(Status.COMMITTED, Status.ORPHANED)) {
            synchronized (children) {
                for (Transaction child : children) {
                    child.orphan();
                }
            }
            return false;
        } else return false;
    }


    /**
     * Returns a string representation of this transaction
     *
     * @return the string representcodes[ation
     */
    public String toString() {
        switch (getStatus()) {
            case COMMITTED:
                return "[T" + count + ":committed, p=" + getParent() + "=>" + getNestingDepth() + "]";
            case ABORTED:
                return "[T" + count + ":aborted,p=" + getParent() + "=>" + getNestingDepth() + "]";
            case ACTIVE:
                return "[T" + count + ":active,p=" + getParent() + "=>" + getNestingDepth() + "]";
            case ORPHANED:
                return "[T" + count + ":orphaned ]";
            default:
                return "[T" + count + "[???]]";
        }
    }

    /**
     * This transaction's contention manager
     *
     * @return the manager
     */
    public ContentionManager getContentionManager() {
        return FortressTaskRunner.getContentionManager();
    }

    public boolean isAncestorOf(Transaction t) {
        Transaction current = t;
        while (current != null && current != this) {
            current = current.getParent();
        }
        if (current == this) {
            return true;
        } else {
            return false;
        }
    }
}
