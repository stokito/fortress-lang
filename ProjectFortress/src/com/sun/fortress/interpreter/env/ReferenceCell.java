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

package com.sun.fortress.interpreter.env;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.evaluator.values.FValue;


/**
 * What the interpreter stores mutable things (fields, variables)
 * in.  It will eventually acquire transactional semantics.
 */
public class ReferenceCell extends IndirectionCell {
    private FType theType;
    ReferenceCell(FType t, FValue v) {
        super();
        theType = t;
        theValue = v;
    }

    ReferenceCell(FType t) {
        super();
        theType = t;
    }

    public void assignValue(FValue f2) {
        theValue = f2;
    }

    public FType getType() {
        return theType;
    }

    public boolean casValue(FValue old_value, FValue new_value) {
        return valueUpdater.compareAndSet(this, old_value, new_value);
    }

    static final AtomicReferenceFieldUpdater<IndirectionCell, FValue> valueUpdater =
        AtomicReferenceFieldUpdater.newUpdater(IndirectionCell.class, FValue.class, "theValue");

}
