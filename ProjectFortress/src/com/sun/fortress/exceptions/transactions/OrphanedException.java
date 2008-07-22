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

package com.sun.fortress.exceptions.transactions;

import com.sun.fortress.interpreter.evaluator.transactions.Transaction;
/**
 * Thrown by an attempt to open a <code>TMObject</code> to indicate
 * that the current transaction cannot commit.
 **/
public class OrphanedException extends java.lang.RuntimeException {

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
