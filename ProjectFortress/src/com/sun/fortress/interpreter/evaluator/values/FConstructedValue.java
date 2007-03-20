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

package com.sun.fortress.interpreter.evaluator.values;
import java.util.ArrayList;
import java.util.List;

import com.sun.fortress.interpreter.evaluator.InterpreterError;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.useful.EquivalenceClass;
import com.sun.fortress.interpreter.useful.Fn;
import com.sun.fortress.interpreter.useful.Useful;


public abstract class FConstructedValue extends FValue {
    /**
     * Stores its ftype explicitly.
     */
    private FType ftype;

    public FType type() { return ftype; }

    /**
     * @param ftype The ftype to set.
     */
    public void setFtype(FType ftype) {
        if (this.ftype != null)
            throw new IllegalStateException("Cannot set twice");
        this.ftype = ftype;
    }

    public void setFtypeUnconditionally(FType ftype) {
        this.ftype = ftype;
    }

}
