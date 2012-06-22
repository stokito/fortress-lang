/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

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
        return (t != null && t.isActive());
    }

    public void restored() {
        t = null;
    }
}
