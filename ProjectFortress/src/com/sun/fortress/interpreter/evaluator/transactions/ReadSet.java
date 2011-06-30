/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.evaluator.transactions;

import com.sun.fortress.interpreter.evaluator.tasks.FortressTaskRunner;

import java.util.AbstractSet;
import java.util.concurrent.CopyOnWriteArrayList;

public class ReadSet extends AbstractSet<Transaction> {
    private CopyOnWriteArrayList<Transaction> elements;
    private Boolean sealed;

    public ReadSet() {
        sealed = false;
        elements = new CopyOnWriteArrayList<Transaction>();
    }


    public ReadSet(ReadSet r) {
        sealed = false;
        elements = new CopyOnWriteArrayList<Transaction>();
        // There is a race here.  If I'm trying to copy from r and r is modified
        // out from underneath me, I get a NoSuchElementException, so I'm writing
        // my own add function.
        if (r != null) myAdd(r);
    }

    private void myAdd(ReadSet r) {
        for (Transaction t : elements) {
            if (t.isActive()) elements.add(t);
        }
    }

    public String toString() {
        return "sealed = " + sealed + " elements = " + elements;
    }


    /**
     * Initialize one object from another.
     *
     * @param aSet Initialize from this other object.
     */
    public void copyFrom(ReadSet aSet) {
        elements.addAll(aSet.elements);
    }

    public void seal() {
        sealed = true;
    }

    /**
     * Add a new transaction to the set.
     *
     * @param t Transaction to add.
     * @return Whether this transaction was already present.
     */
    public boolean add(Transaction t) {
        cleanup();
        if (sealed) {
            FortressTaskRunner.debugPrintln(
                    "add of " + t + " to readset " + toString() + " failed because readset was sealed");
            return false;
        } else {
            elements.addIfAbsent(t);
            return true;
        }
    }

    public void cleanup() {
        for (Transaction t : elements) {
            if (!t.isActive()) remove(t);
        }
    }

    /**
     * remove transaction from the set.
     *
     * @param t Transaction to remove.
     * @return Whether this transaction was already present.
     */
    public boolean remove(Transaction t) {
        boolean res = elements.contains(t);
        if (res) elements.remove(t);
        return res;
    }

    /**
     * Discard all elements of this set.
     */
    public void clear() {
        elements.clear();
    }

    /**
     * How many transactions in the set?
     *
     * @return Number of transactions in the set.
     */
    public int size() {
        return elements.size();
    }

    /**
     * Iterate over transaction in the set.
     *
     * @return Iterator over transactions in the set.
     */
    public java.util.Iterator<Transaction> iterator() {
        return elements.iterator();
    }
}
