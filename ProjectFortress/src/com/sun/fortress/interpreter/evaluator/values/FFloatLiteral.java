/*******************************************************************************
 Copyright 2008,2010, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.evaluator.values;

import java.math.BigInteger;

public class FFloatLiteral extends FNativeObject {
    private final String value;
    private static volatile NativeConstructor con;
    public static final FFloatLiteral ZERO = new FFloatLiteral("0.0");

    public FFloatLiteral(String s) {
        super(null);
        value = s;
    }

    public FFloatLiteral(String s, BigInteger intPart, BigInteger numerator, int denomBase, int denomPower) {
        super(null);
        if (denomBase == 10) value = s;
        else {
            double val = intPart.doubleValue() + (numerator.doubleValue() / Math.pow(denomBase, denomPower));
            value = "" + val;
        }
    }

    public String getString() {
        return value;
    } // TODO Sam left this undone, not sure if intentional

    public double getFloat() {
        return Double.valueOf(value);
    }

    public boolean seqv(FValue v) {
        // HACK that's only sort-of correct.  If we care about all the digits,
        // there must be some sort of normalization.
        if (!(v instanceof FNativeObject)) return false;
        if (v instanceof FFloat || v instanceof FFloatLiteral) {
            return (Double.doubleToRawLongBits(getFloat()) == Double.doubleToRawLongBits(v.getFloat()));
        }
        if (v instanceof FInt || v instanceof FIntLiteral || v instanceof FLong) {
            return (getFloat() == v.getFloat());
        }
        return false;
    }

    public static void setConstructor(NativeConstructor con) {
        // WARNING!  In order to run the tests we must reset con for
        // each new test, so it's not OK to ignore setConstructor
        // attempts after the first one.
        if (con == null) return;
        FFloatLiteral.con = con;
    }

    public NativeConstructor getConstructor() {
        return FFloatLiteral.con;
    }

    public static void resetConstructor() {
        FFloatLiteral.con = null;
    }

}
