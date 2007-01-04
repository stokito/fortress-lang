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
import java.math.BigInteger;

import com.sun.fortress.interpreter.evaluator.types.FTypeIntLiteral;


public class FIntLiteral extends FValue implements HasIntValue {

    private BigInteger value;

    public String getString() { return value.toString(); } // TODO Sam left this undone, not sure if intentional

    public int getInt() { return value.intValue(); }
    public long getLong() { return value.longValue(); }
    public double getFloat() { return value.doubleValue(); }

    public FIntLiteral(BigInteger i) {
        value = i;
        setFtype(FTypeIntLiteral.T);
    }
}
