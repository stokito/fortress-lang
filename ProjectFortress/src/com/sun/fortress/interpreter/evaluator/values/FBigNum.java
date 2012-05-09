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

public class FBigNum extends FNativeObject implements HasIntValue {

    private static volatile NativeConstructor con;

    public static final FBigNum ZERO = new FBigNum(BigInteger.ZERO);

    private final BigInteger value;

    private FBigNum(BigInteger i) {
        super(null);
        value = i;
    }

    public static FValue make(BigInteger v) {
        return new FBigNum(v);
    }

    public String getString() {
        return value.toString();
    }

    public String toString() {
        return value.toString() + ":ZZ";
    }

    public int getInt() {
        throw new ProgramError(errorMsg("Value ", value, " might not fit in ZZ32."));
    }

    public long getLong() {
        throw new ProgramError(errorMsg("Value ", value, " might not fit in ZZ64."));
    }

    public BigInteger getBigInteger() {
        return value;
    }

    public double getFloat() {
        return value.doubleValue();
    }

    public boolean seqv(FValue v) {
        if (!(v instanceof FNativeObject)) return false;
        if (v instanceof FBigNum) return value.equals(((FBigNum) v).value);
        if (v instanceof FFloat || v instanceof FFloatLiteral || v instanceof FRR32) return getFloat() == v.getFloat();
        return false;
    }


    public static void setConstructor(NativeConstructor con) {
        // WARNING!  In order to run the tests we must reset con for
        // each new test, so it's not OK to ignore setConstructor
        // attempts after the first one.
        if (con == null) return;
        FBigNum.con = con;
    }

    public NativeConstructor getConstructor() {
        return FBigNum.con;
    }

    public static void resetConstructor() {
        FBigNum.con = null;
    }
}
