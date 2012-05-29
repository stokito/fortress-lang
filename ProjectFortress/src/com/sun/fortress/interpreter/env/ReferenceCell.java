/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.env;

import static com.sun.fortress.exceptions.InterpreterBug.bug;
import com.sun.fortress.exceptions.transactions.AbortedException;
import com.sun.fortress.exceptions.transactions.OrphanedException;
import com.sun.fortress.interpreter.evaluator.tasks.FortressTaskRunner;
import com.sun.fortress.interpreter.evaluator.transactions.Transaction;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.evaluator.values.FValue;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * What the interpreter stores mutable things (fields, variables)
 * in.  It will eventually acquire transactional semantics.
 */

public class ReferenceCell extends IndirectionCell {
    private FType theType;
    ValueNode node;

    // for debugging
    static final AtomicInteger counter = new AtomicInteger(0);
    final int id;

    public ReferenceCell() {
        super();
        node = ValueNode.nullValueNode;
        id = counter.getAndIncrement();
    }

    public ReferenceCell(FType t) {
        super();
        theType = t;
        node = ValueNode.nullValueNode;
        id = counter.getAndIncrement();
    }

    public ReferenceCell(FType t, FValue v) {
        super();
        theType = t;
        node = new ValueNode(v, FortressTaskRunner.getTransaction(), null, ValueNode.nullValueNode);
        id = counter.getAndIncrement();
    }

    public FType getType() {
        return theType;
    }

    public String toString() {
        return "ReferenceCell" + id;
    }

    private boolean transactionIsCommitted(Transaction w) {
        if (w == null) return true;
        else if (w.isCommitted()) return true;
        return false;
    }

    private boolean transactionIsAbortedOrOrphaned(Transaction w) {
        if (w == null) return false;
        else if (w.isAborted()) return true;
        else if (w.isOrphaned()) return true;
        else return false;
    }

    private boolean transactionIsNotActive(Transaction w) {
        if (w == null) return false;
        else if (w.isAborted()) return true;
        else if (w.isOrphaned()) return true;
        else if (w.isCommitted()) return true;
        else return false;
    }


    private synchronized void cleanup() {
        Transaction w = node.getWriter();
        if (Transaction.debug) FortressTaskRunner.debugPrintln("Cleanup:" + this + " start with node = " + node);
        while (w != null && transactionIsNotActive(w)) {
            if (transactionIsAbortedOrOrphaned(w)) {
                node = node.getOld();
            } else {
                assert (transactionIsCommitted(w));
                Transaction p = w.getParent();
                ValueNode old = node.getOld();
                if (p == null && old == null) {
                    // Committed to top level, with no previous value.
                    node = new ValueNode(node.getValue(), null, null, null);
                } else if (p == null) {
                    // Committed to top level.  Any readers of the committed transaction
                    // must be preserved.
                    node = new ValueNode(node.getValue(), null, old.getReaders(), null);
                } else if (old != null) {
                    // Committed to a parent transaction, must preserve readers and
                    // old transaction.
                    node = new ValueNode(node.getValue(), p, old.getReaders(), old);
                } else {
                    node = new ValueNode(node.getValue(), p, null, null);
                }
            }
            if (node != null) {
                w = node.getWriter();
            } else {
                node = ValueNode.nullValueNode;
                if (Transaction.debug) FortressTaskRunner.debugPrintln("Cleanup:" + this + " end with " + node);
                return;
            }
        }
        if (Transaction.debug) FortressTaskRunner.debugPrintln("Cleanup:" + this + " end with " + node);
    }

    public synchronized void assignValue(FValue f2) {
        Transaction me = FortressTaskRunner.getTransaction();
        cleanup();
        if (Transaction.debug) FortressTaskRunner.debugPrintln(
                this + " assignValue start = " + node + " value = " + f2);

        if (node == ValueNode.nullValueNode) {
            node = new ValueNode(f2, me, null, null);
        }

        // writer is either active, or null
        if (me == null) { // top level assignment
            if (node == ValueNode.nullValueNode) node = new ValueNode(f2, me, null, null);
            else {
                ValueNode temp = node;
                node = new ValueNode(f2, null, null, null);
                temp.abortAllReadersAndWriters();
            }
        } else if (!me.isActive()) {
            throw new AbortedException(me, "Somebody killed me ");
        } else {
            node.resolveReadWriteConflicts();
            cleanup();
            if (!me.isActive()) throw new AbortedException(me, "Somebody killed me ");

            Transaction w = node.getWriter();
            //ValueNode old = node.getOld();

            if (w == null || w.isAncestorOf(me)) {
                node = new ValueNode(f2, me, node.getReaders(), node);
                if (Transaction.debug) me.addWrite(this, f2);

            } else if (w.isActive()) {
                // Cleanup got us to a parent node with an active writer
                assignValue(f2);
            }
        }
        if (Transaction.debug) FortressTaskRunner.debugPrintln(this + " assignValue finish = " + node);
    }

    public synchronized FValue getValue() {
        Transaction me = FortressTaskRunner.getTransaction();
        // Top Level transaction
        if (me == null) {
            while (node.getWriter() != null) {
                node.abortWriter();
                cleanup();
            }
            return node.getValue();
        }

        if (!me.isActive()) throw new AbortedException(me, "Somebody killed me");

        cleanup();

        Transaction w = node.getWriter();
        if (w == me) {
            return node.getValue();
        } else if (w == null || w.isAncestorOf(me)) {
            node.addReader();
            if (Transaction.debug) me.addRead(this, node.getValue());
            return node.getValue();
        } else if (w.isActive()) {
            node.resolveWriteConflict();
        }
        return getValue();
    }

    public FValue getValueNull() {
        FValue res = getValue();
        if (res == null) {
            FortressTaskRunner runner = (FortressTaskRunner) Thread.currentThread();
            Transaction t = runner.getTransaction();
            if (t.isAborted()) throw new AbortedException(t, "UhOh Someone aborted me");
            else if (t.isOrphaned()) throw new OrphanedException(t, "UhOh I'm an orphan");
            else throw new RuntimeException(
                        runner.getName() + " getValueNull is about to return a null value ReferenceCell = " + this +
                        " node = " + node + " task = " + runner.getTask());
        }
        return res;
    }


    public void storeValue(FValue f2) {
        if (node.getValue() != null) bug("Internal error, second store of indirection cell");
        assignValue(f2);
    }

    public void storeType(FType f2) {
        if (theType != null) bug("Internal error, second store of type");
        theType = f2;
    }

    public boolean isInitialized() {
        return getValue() != null;
    }

}
