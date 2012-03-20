/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.glue.prim;

import com.naturalbridge.misc.Unsigned;
import static com.sun.fortress.exceptions.ProgramError.error;
import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.interpreter.evaluator.types.FTypeObject;
import com.sun.fortress.interpreter.evaluator.values.*;
import com.sun.fortress.interpreter.glue.NativeMeth0;
import com.sun.fortress.interpreter.glue.NativeMeth1;
import com.sun.fortress.nodes.ObjectConstructor;

import java.util.List;

/**
 * Functions from NN32.
 */
public class NN32 extends NativeConstructor {

    public NN32(Environment env, FTypeObject selfType, ObjectConstructor def) {
        super(env, selfType, def);
    }

    protected FNativeObject makeNativeObject(List<FValue> args, NativeConstructor con) {
        FNN32.setConstructor(this);
        return FNN32.ZERO;
    }

    static private abstract class NN2B extends NativeMeth1 {
        protected abstract boolean f(int x, int y);

        public final FValue applyMethod(FObject x, FValue y) {
            return FBool.make(f(x.getNN32(), y.getNN32()));
        }
    }

    static private abstract class N2N extends NativeMeth0 {
        protected abstract int f(int x);

        public final FValue applyMethod(FObject x) {
            return FNN32.make(f(x.getNN32()));
        }
    }

    static private abstract class N2Z extends NativeMeth0 {
        protected abstract int f(int x);

        public final FValue applyMethod(FObject x) {
            return FInt.make(f(x.getNN32()));
        }
    }

    static private abstract class N2U extends NativeMeth0 {
        protected abstract long f(int x);

        public final FValue applyMethod(FObject x) {
            return FNN64.make(f(x.getNN32()));
        }
    }

    static private abstract class N2R extends NativeMeth0 {
        protected abstract double f(int x);

        public final FValue applyMethod(FObject x) {
            return FFloat.make(f(x.getNN32()));
        }
    }

    static private abstract class N2S extends NativeMeth0 {
        protected abstract java.lang.String f(int x);

        public final FValue applyMethod(FObject x) {
            return FString.make(f(x.getNN32()));
        }
    }

    static private abstract class NN2N extends NativeMeth1 {
        protected abstract int f(int x, int y);

        public final FValue applyMethod(FObject x, FValue y) {
            return FNN32.make(f(x.getNN32(), y.getNN32()));
        }
    }

    static private abstract class NL2N extends NativeMeth1 {
        protected abstract int f(int x, long y);

        public final FValue applyMethod(FObject x, FValue y) {
            return FNN32.make(f(x.getNN32(), y.getLong()));
        }
    }

    public static final class Negate extends N2N {
        protected int f(int x) {
            return Unsigned.subtract(0, x);
        }
    }

    public static final class Add extends NN2N {
        protected int f(int x, int y) {
            return Unsigned.add(x, y);
        }
    }

    public static final class Sub extends NN2N {
        protected int f(int x, int y) {
            return Unsigned.subtract(x, y);
        }
    }

    public static final class Mul extends NN2N {
        protected int f(int x, int y) {
            return Unsigned.multiplyToInt(x, y);
        }
    }

    public static final class Div extends NN2N {
        protected int f(int x, int y) {
            return Unsigned.divide(x, y);
        }
    }

    public static final class Rem extends NN2N {
        protected int f(int x, int y) {
            return Unsigned.remainder(x, y);
        }
    }

    public static final class Gcd extends NN2N {
        protected int f(int u, int v) {
            return (int) UnsignedLong.gcd(Unsigned.toLong(u), Unsigned.toLong(v));
        }
    }

    public static final class Lcm extends NN2N {
        protected int f(int u, int v) {
            int g = (int) UnsignedLong.gcd(u, v);
            return Unsigned.multiplyToInt(Unsigned.divide(u, g), v);
        }
    }

    public static final class Choose extends NN2N {
        protected int f(int u, int v) {
            return rc(UnsignedLong.choose(u, v));
        }
    }

    public static final class Mod extends NN2N {
        protected int f(int u, int v) {
            /* MOD and REM are the same for natural numbers. */
            return Unsigned.remainder(u, v);
        }
    }

    public static final class BitAnd extends NN2N {
        protected int f(int u, int v) {
            return u & v;
        }
    }

    public static final class BitOr extends NN2N {
        protected int f(int u, int v) {
            return u | v;
        }
    }

    public static final class BitXor extends NN2N {
        protected int f(int u, int v) {
            return u ^ v;
        }
    }

    public static final class LShift extends NL2N {
        protected int f(int u, long v) {
            return ((v & ~31) == 0) ? (u << (int) v) : 0;
        }
    }

    public static final class RShift extends NL2N {
        protected int f(int u, long v) {
            return ((v & ~31) == 0) ? (u >>> (int) v) : 0;
        }
    }

    public static final class BitNot extends N2N {
        protected int f(int x) {
            return ~x;
        }
    }

    public static final class Eq extends NN2B {
        protected boolean f(int x, int y) {
            return x == y;
        }
    }

    public static final class Less extends NN2B {
        protected boolean f(int x, int y) {
            return Unsigned.lessThan(x, y);
        }
    }

    public static final class Partition extends N2N {
        protected int f(int u) {
            return Integer.highestOneBit(u - 1);
        }
    }

    public static final class Pow extends NativeMeth1 {
        public FValue applyMethod(FObject x, FValue y) {
            int base = x.getNN32();
            long exp = y.getLong();
            if (exp < 0) {
                return FFloat.make(1.0 / (double) UnsignedLong.pow(base, -exp));
            } else {
                return FNN32.make(rc(UnsignedLong.pow(base, exp)));
            }
        }
    }

    public static final class ToInt extends N2Z {
        protected int f(int x) {
            return x;
        }
    }

    public static final class ToUnsignedLong extends N2U {
        protected long f(int x) {
            return Unsigned.toLong(x);
        }
    }

    public static final class ToString extends N2S {
        protected java.lang.String f(int x) {
            return Unsigned.toString(x);
        }
    }

    public static final class AsFloat extends N2R {
        protected double f(int x) {
            return Unsigned.toDouble(x);
        }
    }

    public static int rc(long i) {
        if ((i >> 32) != 0) {
            error("Overflow of NN32 " + i);
        }
        return (int) i;
    }

    @Override
    protected void unregister() {
        FNN32.resetConstructor();

    }

}
