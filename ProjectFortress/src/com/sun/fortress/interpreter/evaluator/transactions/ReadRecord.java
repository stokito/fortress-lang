/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.evaluator.transactions;

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
        int n = 0;
        while (it != null && !it.isActive()) {
            n++;
            it = it.getNext();
        }
        if (AtomicFTypeArray.TRACE_ARRAY && n > 0) System.out.println("Cleaned " + n);
        return it;
    }
}
