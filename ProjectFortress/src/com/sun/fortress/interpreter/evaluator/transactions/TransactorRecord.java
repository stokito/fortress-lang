/*******************************************************************************
    Copyright 2007 Sun Microsystems, Inc.,
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
import com.sun.fortress.interpreter.evaluator.transactions.exceptions.AbortedException;

abstract class TransactorRecord {
    protected volatile Transaction t = null;

    public Transaction getTransaction() {
        return t;
    }

    public void completed() {
        Transaction t = FortressTaskRunner.getTransaction();

        if (t==null) {
            this.t = Transaction.COMMITTED_TRANS;
        } else if (t.isActive()) {
            this.t = t;
        } else {
            throw new AbortedException();
        }
    }

    public boolean isActive() {
        if (t==null) {
            return true;
        } else {
            return t.isActive();
        }
    }
}
