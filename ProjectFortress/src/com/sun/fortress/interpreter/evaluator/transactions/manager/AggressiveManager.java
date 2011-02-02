/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.evaluator.transactions.manager;

import com.sun.fortress.interpreter.evaluator.transactions.Transaction;

/**
 * The Chuck Norris contention manager:  always abort other transaction.
 *
 * @author Maurice Herlihy
 */
public class AggressiveManager extends BaseManager {

    public AggressiveManager() {
    }

    public void resolveConflict(Transaction me, Transaction other) {
        other.abort();
    }

    public long getPriority() {
        throw new UnsupportedOperationException();
    }

    public void setPriority(long value) {
        throw new UnsupportedOperationException();
    }
}
