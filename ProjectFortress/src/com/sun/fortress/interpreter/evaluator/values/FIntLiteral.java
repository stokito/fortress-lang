/*******************************************************************************
 Copyright 2008,2010, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.evaluator.values;

import com.sun.fortress.exceptions.ProgramError;
import static com.sun.fortress.exceptions.ProgramError.errorMsg;

import java.math.BigInteger;

public class FIntLiteral extends FNativeObject implements HasIntValue {

    public static final BigInteger INT_MIN = BigInteger.valueOf(java.lang.Integer.MIN_VALUE);

    public static final BigInteger INT_MAX = BigInteger.valueOf(java.lang.Integer.MAX_VALUE);

    public static final BigInteger LONG_MIN = BigInteger.valueOf(java.lang.Long.MIN_VALUE);

    public static final BigInteger LONG_MAX = BigInteger.valueOf(java.lang.Long.MAX_VALUE);

    private static volatile NativeConstructor con;

    public static final FIntLiteral ZERO = new FIntLiteral(BigInteger.ZERO);

    private final BigInteger value;

    private FIntLiteral(BigInteger i) {
        super(null);
        value = i;
    }

    public static FValue make(BigInteger v) {
        if (v.compareTo(INT_MAX) > 0) {
            if (v.compareTo(LONG_MAX) > 0) {
                return FBigNum.make(v);
            } else {
                return FLong.make(v.longValue());
            }
        } else if (v.compareTo(INT_MIN) < 0) {
            if (v.compareTo(LONG_MIN) < 0) {
                return new FIntLiteral(v);
            } else {
                return FLong.make(v.longValue());
            }
        } else {
            return FInt.make(v.intValue());
        }
    }

    public String getString() {
        return (value.toString() + ":IntLiteral");
    }

    public int getInt() {
        throw new ProgramError(errorMsg("Value ", value, " does not fit in ZZ32."));
    }

    public long getLong() {
        throw new ProgramError(errorMsg("Value ", value, " does not fit in ZZ64."));
    }

    public BigInteger getLit() {
        return value;
    }

    public double getFloat() {
        return value.doubleValue();
    }

    public boolean seqv(FValue v) {
        if (!(v instanceof FNativeObject)) return false;
        if (v instanceof FIntLiteral) return value.equals(((FIntLiteral) v).value);
        if (v instanceof FFloat || v instanceof FFloatLiteral) return getFloat() == v.getFloat();
        return false;
    }


    public static void setConstructor(NativeConstructor con) {
        // WARNING!  In order to run the tests we must reset con for
        // each new test, so it's not OK to ignore setConstructor
        // attempts after the first one.
        if (con == null) return;
        FIntLiteral.con = con;
    }

    public NativeConstructor getConstructor() {
        return FIntLiteral.con;
    }

    public static void resetConstructor() {
        FIntLiteral.con = null;
        ;
    }
}
