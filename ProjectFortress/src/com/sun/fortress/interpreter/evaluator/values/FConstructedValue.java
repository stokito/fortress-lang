/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.evaluator.values;

import static com.sun.fortress.exceptions.InterpreterBug.bug;
import static com.sun.fortress.exceptions.ProgramError.errorMsg;
import com.sun.fortress.interpreter.evaluator.types.FType;

public abstract class FConstructedValue extends FValue {
    /**
     * Stores its ftype explicitly.
     */
    private final FType ftype;

    /**
     * Getter for ftype.  Should always be non-null, but right now
     * FGenericFunction never calls setFtype and this returns null in
     * that case.  This leads to extensive bugs particularly when a
     * generic function is overloaded along with non-generic siblings,
     * or when a generic function is passed as an argument to an
     * overloaded function without providing an explicit type
     * instantiation.  Delete "&& false" to enable checking if you're
     * trying to fix this bug.
     */
    public FType type() {
        return ftype;
    }

    protected FConstructedValue(FType type) {
        ftype = type;
    }

    public boolean seqv(FValue v) {
        bug(errorMsg("seqv of FConstructedValue ", this, " and ", v));
        return false;
    }

}
