/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.env;

import static com.sun.fortress.exceptions.InterpreterBug.bug;
import static com.sun.fortress.exceptions.ProgramError.error;
import static com.sun.fortress.exceptions.ProgramError.errorMsg;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.evaluator.values.FValue;

public class IndirectionCell extends FValue {
    protected volatile FValue theValue;

    public String toString() {
        if (theValue == null) return "null";
        if (theValue instanceof IndirectionCell) return "Uninitialized " + theValue.getClass().getSimpleName();
        return theValue.toString();
    }

    public IndirectionCell() {
    }

    public void storeValue(FValue f2) {
        if (theValue != null) bug("Internal error, second store of indirection cell");
        theValue = f2;
    }

    public boolean isInitialized() {
        return theValue != null;
    }

    public FValue getValue() {
        if (theValue == null) {
            error("Attempt to read uninitialized variable");
        }
        return theValue;
    }

    public FValue getValueNull() {
        return theValue;
    }

    public FType type() {
        if (theValue == null) {
            error("Attempt to find type of uninitialized variable: " + this);
        }
        return theValue.type();
    }

    public boolean seqv(FValue v) {
        bug(errorMsg("seqv on IndirectionCell ", this, " and ", v));
        return false;
    }

}
