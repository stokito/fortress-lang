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
import static com.sun.fortress.exceptions.ProgramError.errorMsg;

import java.math.BigInteger;

import com.sun.fortress.exceptions.ProgramError;

public class FBigNum extends NativeConstructor.FNativeObject
        implements HasIntValue {

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

    public String getString() { return value.toString(); }

    public String toString() { return value.toString() + ":ZZ"; }

    public int getInt() {
        throw new ProgramError(errorMsg("Value ", value,
                                        " might not fit in ZZ32."));
    }
    public long getLong() {
        throw new ProgramError(errorMsg("Value ", value,
                                        " might not fit in ZZ64."));
    }
    public BigInteger getBigInteger() { return value; }

    public double getFloat() {
        return value.doubleValue();
    }

    public boolean seqv(FValue v) {
        if (!(v instanceof NativeConstructor.FNativeObject)) return false;
        if (v instanceof FBigNum)
            return value.equals(((FBigNum)v).value);
        if (v instanceof FFloat || v instanceof FFloatLiteral || v instanceof FRR32)
            return getFloat()==v.getFloat();
        return false;
    }


    public static void setConstructor(NativeConstructor con) {
        // WARNING!  In order to run the tests we must reset con for
        // each new test, so it's not OK to ignore setConstructor
        // attempts after the first one.
        if (con==null) return;
        FBigNum.con = con;
    }

    public NativeConstructor getConstructor() {
        return FBigNum.con;
    }

    public static void resetConstructor() {
        FBigNum.con = null;
    }
}
