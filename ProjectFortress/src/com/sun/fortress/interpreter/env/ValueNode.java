/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.env;

import com.sun.fortress.exceptions.transactions.AbortedException;
import com.sun.fortress.interpreter.evaluator.tasks.FortressTaskRunner;
import com.sun.fortress.interpreter.evaluator.transactions.ReadSet;
import com.sun.fortress.interpreter.evaluator.transactions.Transaction;
import com.sun.fortress.interpreter.evaluator.values.FValue;

import java.util.ArrayList;
import java.util.List;

public class ValueNode {
    ValueNode old;
    FValue value;
    Transaction writer;
    ReadSet readers;

    public ValueNode(FValue v, Transaction w, ReadSet r, ValueNode o) {
        old = o;
        value = v;
        writer = w;
        readers = new ReadSet(r);
    }

    ValueNode() {
        old = null;
        value = null;
        writer = null;
        readers = new ReadSet();
    }

    public static final ValueNode nullValueNode = new ValueNode();

    public String toString() {
        if (this == nullValueNode) return "nullValueNode";
        else return "ValueNode[" + value + ":" + writer + "::" + readers + "]";
    }

    public FValue getValue() {
        return value;
    }

    public Transaction getWriter() {
        return writer;
    }

    public ReadSet getReaders() {
        return readers;
    }

    public ValueNode getOld() {
        return old;
    }

    public void addReader() {
        Transaction me = FortressTaskRunner.getTransaction();
        if (!readers.add(me)) {
            me.abort();
            throw new AbortedException(me, "ReadSet Sealed : " + readers);
        }
    }

    public void abortAllReaders() {
        if (this == nullValueNode) throw new RuntimeException(
                Thread.currentThread().getName() + "Trying to abort all the readers of the null value node");

        readers.seal();
        for (Transaction r : readers) {
            r.abort();
        }
    }

    public void abortWriter() {
        if (writer != null) {
            writer.abort();
        }
    }

    public void abortAllReadersAndWriters() {
        abortAllReaders();
        abortWriter();
    }

    public void resolveReadConflicts() {
        Transaction me = FortressTaskRunner.getTransaction();
        List<Transaction> conflicts = new ArrayList<Transaction>();
        for (Transaction reader : readers) {
            if (reader.isActive() && !reader.isAncestorOf(me)) {
                conflicts.add(reader);
            }
        }
        if (!conflicts.isEmpty()) me.getContentionManager().resolveConflict(me, conflicts);
    }

    public void resolveWriteConflict() {
        Transaction me = FortressTaskRunner.getTransaction();
        if (writer != null && writer.isActive() && !writer.isAncestorOf(me)) {
            me.getContentionManager().resolveConflict(me, writer);
        }
    }

    public void resolveReadWriteConflicts() {
        resolveReadConflicts();
        resolveWriteConflict();
    }
}
