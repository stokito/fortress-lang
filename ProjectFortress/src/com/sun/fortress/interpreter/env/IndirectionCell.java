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

package com.sun.fortress.interpreter.env;

import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.interpreter.evaluator.types.FType;

import static com.sun.fortress.interpreter.evaluator.InterpreterBug.bug;
import static com.sun.fortress.interpreter.evaluator.ProgramError.error;

public class IndirectionCell extends FValue {
    protected volatile FValue theValue;
    public String toString() {
        if (theValue==null) return "null";
        if (theValue instanceof IndirectionCell)
            return "Uninitialized " + theValue.getClass().getSimpleName();
        return theValue.toString();
    }

    public IndirectionCell() {
    }

    public void storeValue(FValue f2) {
        if (theValue != null)
            bug("Internal error, second store of indirection cell");
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

    public FType type() {
        if (theValue == null) {
            error("Attempt to find type of uninitialized variable");
        }
        return theValue.type();
    }
}
