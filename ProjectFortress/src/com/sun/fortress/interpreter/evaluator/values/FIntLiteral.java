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

import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.evaluator.types.FTypeIntLiteral;
import com.sun.fortress.interpreter.evaluator.ProgramError;

public class FIntLiteral extends FValue implements HasIntValue {

    public static final BigInteger INT_MIN =
        BigInteger.valueOf(java.lang.Integer.MIN_VALUE);

    public static final BigInteger INT_MAX =
        BigInteger.valueOf(java.lang.Integer.MAX_VALUE);

    public static final BigInteger LONG_MIN =
        BigInteger.valueOf(java.lang.Long.MIN_VALUE);

    public static final BigInteger LONG_MAX =
        BigInteger.valueOf(java.lang.Long.MAX_VALUE);

    private final BigInteger value;

    private FIntLiteral(BigInteger i) {
        value = i;
    }

    public static FValue make(BigInteger v) {
        if (v.compareTo(INT_MAX)>0) {
            if (v.compareTo(LONG_MAX)>0) {
                return new FIntLiteral(v);
            } else {
                return FLong.make(v.longValue());
            }
        } else if (v.compareTo(INT_MIN)<0) {
            if (v.compareTo(LONG_MIN)<0) {
                return new FIntLiteral(v);
            } else {
                return FLong.make(v.longValue());
            }
        } else {
            return FInt.make(v.intValue());
        }
    }

    public FType type() { return FTypeIntLiteral.T; }

    public String getString() { return value.toString(); } // TODO Sam left this undone, not sure if intentional

    public int getInt() {
        throw new ProgramError("Value "+value+" does not fit in ZZ32.");
    }
    public long getLong() {
        throw new ProgramError("Value "+value+" does not fit in ZZ64.");
    }
    public BigInteger getLit() { return value; }
    public double getFloat() { return value.doubleValue(); }
}
