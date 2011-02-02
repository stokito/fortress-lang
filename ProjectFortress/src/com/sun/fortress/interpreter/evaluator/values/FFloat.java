/*******************************************************************************
 Copyright 2008,2010, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.evaluator.values;


public class FFloat extends FNativeObject {
    private final double val;
    private static volatile NativeConstructor con;

    private FFloat(double x) {
        super(null);
        val = x;
    }

    public double getFloat() {
        return val;
    }

    public String getString() {
        return Double.toString(val);
    }

    public String toString() {
        return (val + ":RR64");
    }

    static public final FFloat ZERO = new FFloat(0.0);

    static public FFloat make(double x) {
        return new FFloat(x);
    }

    public boolean seqv(FValue v) {
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
        FFloat.con = con;
    }

    public NativeConstructor getConstructor() {
        return FFloat.con;
    }

    public static void resetConstructor() {
        FFloat.con = null;
    }
}
