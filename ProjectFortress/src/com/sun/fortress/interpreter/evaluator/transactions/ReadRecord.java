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

import com.sun.fortress.interpreter.evaluator.transactions.AtomicFTypeArray;

final class ReadRecord extends TransactorRecord {
    private volatile ReadRecord next;

    public ReadRecord(ReadRecord next) {
        super();
        this.next = next;
    }

    public ReadRecord getNext() {
        return next;
    }

    public ReadRecord clean() {
        ReadRecord it = this;
        int n=0;
        while (it != null && !it.isActive()) {
            n++;
            it = it.getNext();
        }
        if (AtomicFTypeArray.TRACE_ARRAY && n > 0)
            System.out.println("Cleaned "+n);
        return it;
    }
}
