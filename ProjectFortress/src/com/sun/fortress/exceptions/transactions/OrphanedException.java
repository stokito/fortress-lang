/*******************************************************************************
    Copyright 2008, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

******************************************************************************/

package com.sun.fortress.exceptions.transactions;

import com.sun.fortress.interpreter.evaluator.transactions.Transaction;
/**
 * Thrown by an attempt to open a <code>TMObject</code> to indicate
 * that the current transaction cannot commit.
 **/
public class OrphanedException extends java.lang.RuntimeException {

    /**
     * Make Eclipse happy
     */
    private static final long serialVersionUID = 8015908377885056915L;

    Transaction t;
    String threadName;


    public OrphanedException(Transaction trans) {
		super(Thread.currentThread().getName() + " Orphaned Transaction " + trans);
		threadName = Thread.currentThread().getName();
		t = trans;
    }


    public OrphanedException(Transaction trans, String msg) {
		super(Thread.currentThread().getName() + " Orphaned Transaction " + trans + " for reason " + msg);
		threadName = Thread.currentThread().getName();
		t = trans;
	}

    public Transaction getTransaction() { return t;}
    public String getThreadName() { return threadName;}
}
