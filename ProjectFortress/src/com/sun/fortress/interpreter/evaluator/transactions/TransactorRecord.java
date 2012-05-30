/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.evaluator.transactions;

import com.sun.fortress.exceptions.transactions.AbortedException;
import com.sun.fortress.interpreter.evaluator.tasks.FortressTaskRunner;

abstract class TransactorRecord {
    protected volatile Transaction t = null;

    // TODO: Document why we need a notion of completed() here.
    // Essentially data protected by the TransactorRecord is
    // locked in the interval in which the record is not yet
    // complete; others can't tell who they're conflicting
    // with and must assume the worst.  But is it necessary?
    // It may be due to the fact that value restoration uses helping.
    // But is it required for reads?  If so, isn't read record reuse
    // wrong?  If not, shouldn't we move this junk down a level and
    // just have the isActive and getTransaction interfaces here?

    public Transaction getTransaction() {
        return t;
    }

    public void completed() {
        Transaction t = FortressTaskRunner.getTransaction();

        if (t == null) {
            this.t = null;
        } else if (t.isActive()) {
            this.t = t;
        } else {
            throw new AbortedException(t);
        }
    }

    public boolean isActive() {
        if (t == null) {
            return true;
        } else {
            return t.isActive();
        }
    }
}
