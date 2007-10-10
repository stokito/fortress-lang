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

import com.sun.fortress.interpreter.evaluator.values.FValue;

final class WriteRecord extends TransactorRecord {
    private FValue oldValue;

    public WriteRecord() {
        super();
    }

    public void setOldValue(FValue oldValue) {
        this.oldValue = oldValue;
    }

    public FValue getOldValue() {
        return oldValue;
    }

    public boolean mustRestore() {
        return (t != null && t.getStatus()==Transaction.Status.ABORTED);
    }

    public void restored() {
        t = Transaction.COMMITTED_TRANS;
    }
}
