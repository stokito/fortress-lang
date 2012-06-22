/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.glue.prim;

/* import java.lang.String; /* SPARE COPY  */

import com.sun.fortress.interpreter.evaluator.values.*;
import com.sun.fortress.interpreter.glue.NativeFn1;
import com.sun.fortress.interpreter.glue.NativeFn2;
import com.sun.fortress.interpreter.glue.NativeFn3;


/**
 * Utility classes to make it easier to define many simple primitives.
 */
public class Util {
    static public abstract class Z2o extends NativeFn1 {
        protected abstract FValue f(int x);

        protected final FValue applyToArgs(FValue x) {
            return f(x.getInt());
        }
    }

    static public abstract class Z2Z extends NativeFn1 {
        protected abstract int f(int x);

        protected final FValue applyToArgs(FValue x) {
            return FInt.make(f(x.getInt()));
        }
    }

    static public abstract class ZZ2o extends NativeFn2 {
        protected abstract FValue f(int x, int y);

        protected final FValue applyToArgs(FValue x, FValue y) {
            return f(x.getInt(), y.getInt());
        }
    }

    static public abstract class ZZ2Z extends NativeFn2 {
        protected abstract int f(int x, int y);

        protected final FValue applyToArgs(FValue x, FValue y) {
            return FInt.make(f(x.getInt(), y.getInt()));
        }
    }

    static public abstract class ZL2Z extends NativeFn2 {
        protected abstract int f(int x, long y);

        protected final FValue applyToArgs(FValue x, FValue y) {
            return FInt.make(f(x.getInt(), y.getLong()));
        }
    }

    static public abstract class ZZ2B extends NativeFn2 {
        protected abstract boolean f(int x, int y);

        protected final FValue applyToArgs(FValue x, FValue y) {
            return FBool.make(f(x.getInt(), y.getInt()));
        }
    }

    static public abstract class Z2L extends NativeFn1 {
        protected abstract long f(int x);

        protected final FValue applyToArgs(FValue x) {
            return FLong.make(f(x.getInt()));
        }
    }

    static public abstract class L2o extends NativeFn1 {
        protected abstract FValue f(long x);

        protected final FValue applyToArgs(FValue x) {
            return f(x.getLong());
        }
    }

    static public abstract class L2L extends NativeFn1 {
        protected abstract long f(long x);

        protected final FValue applyToArgs(FValue x) {
            return FLong.make(f(x.getLong()));
        }
    }

    static public abstract class L2R extends NativeFn1 {
        protected abstract double f(long x);

        protected final FValue applyToArgs(FValue x) {
            return FFloat.make(f(x.getLong()));
        }
    }

    static public abstract class LL2o extends NativeFn2 {
        protected abstract FValue f(long x, long y);

        protected final FValue applyToArgs(FValue x, FValue y) {
            return f(x.getLong(), y.getLong());
        }
    }

    static public abstract class LL2L extends NativeFn2 {
        protected abstract long f(long x, long y);

        protected final FValue applyToArgs(FValue x, FValue y) {
            return FLong.make(f(x.getLong(), y.getLong()));
        }
    }

    static public abstract class LL2B extends NativeFn2 {
        protected abstract boolean f(long x, long y);

        protected final FValue applyToArgs(FValue x, FValue y) {
            return FBool.make(f(x.getLong(), y.getLong()));
        }
    }

    static public abstract class L2Z extends NativeFn1 {
        protected abstract int f(long x);

        protected final FValue applyToArgs(FValue x) {
            return FInt.make(f(x.getLong()));
        }
    }

    static public abstract class I2F extends NativeFn1 {
        protected abstract float f(int x);

        protected final FValue applyToArgs(FValue x) {
            return FRR32.make(f(x.getInt()));
        }
    }

    static public abstract class F2F extends NativeFn1 {
        protected abstract float f(float x);

        protected final FValue applyToArgs(FValue x) {
            return FRR32.make(f(x.getRR32()));
        }
    }

    static public abstract class R2o extends NativeFn1 {
        protected abstract FValue f(double x);

        protected final FValue applyToArgs(FValue x) {
            return f(x.getFloat());
        }
    }

    static public abstract class R2R extends NativeFn1 {
        protected abstract double f(double x);

        protected final FValue applyToArgs(FValue x) {
            return FFloat.make(f(x.getFloat()));
        }
    }

    static public abstract class RR2o extends NativeFn2 {
        protected abstract FValue f(double x, double y);

        protected final FValue applyToArgs(FValue x, FValue y) {
            return f(x.getFloat(), y.getFloat());
        }
    }

    static public abstract class RR2R extends NativeFn2 {
        protected abstract double f(double x, double y);

        protected final FValue applyToArgs(FValue x, FValue y) {
            return FFloat.make(f(x.getFloat(), y.getFloat()));
        }
    }

    static public abstract class RR2B extends NativeFn2 {
        protected abstract boolean f(double x, double y);

        protected final FValue applyToArgs(FValue x, FValue y) {
            return FBool.make(f(x.getFloat(), y.getFloat()));
        }
    }

    static public abstract class R2L extends NativeFn1 {
        protected abstract long f(double x);

        protected final FValue applyToArgs(FValue x) {
            return FLong.make(f(x.getFloat()));
        }
    }

    static public abstract class B2o extends NativeFn1 {
        protected abstract FValue f(boolean x);

        protected final FValue applyToArgs(FValue x) {
            return f(x == FBool.TRUE);
        }
    }

    static public abstract class B2B extends NativeFn1 {
        protected abstract boolean f(boolean x);

        protected final FValue applyToArgs(FValue x) {
            return FBool.make(f(x == FBool.TRUE));
        }
    }

    static public abstract class BB2o extends NativeFn2 {
        protected abstract FValue f(boolean x, boolean y);

        protected final FValue applyToArgs(FValue x, FValue y) {
            return f(x == FBool.TRUE, y == FBool.TRUE);
        }
    }

    static public abstract class BB2B extends NativeFn2 {
        protected abstract boolean f(boolean x, boolean y);

        protected final FValue applyToArgs(FValue x, FValue y) {
            return FBool.make(f(x == FBool.TRUE, y == FBool.TRUE));
        }
    }

    static public abstract class SS2o extends NativeFn2 {
        protected abstract FValue f(String x, String y);

        protected final FValue applyToArgs(FValue x, FValue y) {
            return f(x.getString(), y.getString());
        }
    }

    static public abstract class SS2S extends NativeFn2 {
        protected abstract String f(String x, String y);

        protected final FValue applyToArgs(FValue x, FValue y) {
            return FString.make(f(x.getString(), y.getString()));
        }
    }

    static public abstract class SS2B extends NativeFn2 {
        protected abstract boolean f(String x, String y);

        protected final FValue applyToArgs(FValue x, FValue y) {
            return FBool.make(f(x.getString(), y.getString()));
        }
    }

    static public abstract class SS2Z extends NativeFn2 {
        protected abstract int f(String x, String y);

        protected final FValue applyToArgs(FValue x, FValue y) {
            return FInt.make(f(x.getString(), y.getString()));
        }
    }

    static public abstract class SZZ2S extends NativeFn3 {
        protected abstract String f(String x, int y, int z);

        protected final FValue applyToArgs(FValue x, FValue y, FValue z) {
            return FString.make(f(x.getString(), y.getInt(), z.getInt()));
        }
    }

    static public abstract class S2Z extends NativeFn1 {
        protected abstract int f(String x);

        protected final FValue applyToArgs(FValue x) {
            return FInt.make(f(x.getString()));
        }
    }

    static public abstract class S2V extends NativeFn1 {
        protected abstract void f(String x);

        protected final FValue applyToArgs(FValue x) {
            f(x.getString());
            return FVoid.V;
        }
    }

    static public abstract class Z2V extends NativeFn1 {
        protected abstract void f(int x);

        protected final FValue applyToArgs(FValue x) {
            f(x.getInt());
            return FVoid.V;
        }
    }

    static public abstract class o2V extends NativeFn1 {
        protected abstract void f(FValue x);

        protected final FValue applyToArgs(FValue x) {
            f(x);
            return FVoid.V;
        }
    }

}
