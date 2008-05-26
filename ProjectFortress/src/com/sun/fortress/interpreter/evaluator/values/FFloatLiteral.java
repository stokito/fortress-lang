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

import java.lang.Double;

public class FFloatLiteral extends NativeConstructor.FNativeObject {
    private final String value;
    private static volatile NativeConstructor con;
    public static final FFloatLiteral ZERO = new FFloatLiteral("0.0");

    public FFloatLiteral(String s) {
        super(null);
        value = s;
    }

    public String getString() { return value; } // TODO Sam left this undone, not sure if intentional

    public double getFloat() { return Double.valueOf(value); }

    public boolean seqv(FValue v) {
        // HACK that's only sort-of correct.  If we care about all the digits,
        // there must be some sort of normalization.
        if (!(v instanceof NativeConstructor.FNativeObject)) return false;
        if (v instanceof FFloat || v instanceof FFloatLiteral) {
            return (Double.doubleToRawLongBits(getFloat()) ==
                    Double.doubleToRawLongBits(v.getFloat()));
        }
        if (v instanceof FInt   || v instanceof FIntLiteral   ||
            v instanceof FLong) {
            return (getFloat() == v.getFloat());
        }
        return false;
    }

    public static void setConstructor(NativeConstructor con) {
        // WARNING!  In order to run the tests we must reset con for
        // each new test, so it's not OK to ignore setConstructor
        // attempts after the first one.
        if (con==null) return;
        FFloatLiteral.con = con;
    }

    public NativeConstructor getConstructor() {
        return FFloatLiteral.con;
    }

}
