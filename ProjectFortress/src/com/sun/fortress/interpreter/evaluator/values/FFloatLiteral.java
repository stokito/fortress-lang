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

package com.sun.fortress.interpreter.evaluator.values;
import com.sun.fortress.interpreter.evaluator.types.FBuiltinType;
import com.sun.fortress.interpreter.evaluator.types.FTypeFloatLiteral;

public class FFloatLiteral extends FBuiltinValue {
    private final String value;

    public FBuiltinType type() { return FTypeFloatLiteral.ONLY; }

    public String getString() { return value; } // TODO Sam left this undone, not sure if intentional

    public double getFloat() { return Double.valueOf(value); }

    public FFloatLiteral(String s) {
        value = s;
    }
}
