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

public class FRR32 extends NativeConstructor.FNativeObject {
    private final float val;
    private static volatile NativeConstructor con;

    private FRR32(float x) {
        super(null);
        val = x;
    }

    public double getFloat() {return val;}
    public float getRR32() {return val;}
    public String getString() {return Float.toString(val);}
    public String toString() {
        return (val+":RR32");
    }
    static public final FRR32 ZERO = new FRR32((float)0.0);
    static public FRR32 make(float x) {
        return new FRR32(x);
    }
    public boolean seqv(FValue v) {
        if (!(v instanceof NativeConstructor.FNativeObject)) return false;
        if (v instanceof FRR32) {
            return (Float.floatToRawIntBits(getRR32()) ==
                    Float.floatToRawIntBits(v.getRR32()));
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
        FRR32.con = con;
    }

    public NativeConstructor getConstructor() {
        return FRR32.con;
    }
}
