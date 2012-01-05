/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.glue.prim;

import com.naturalbridge.misc.Unsigned;
import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.interpreter.evaluator.types.FTypeObject;
import com.sun.fortress.interpreter.evaluator.values.*;
import com.sun.fortress.interpreter.glue.NativeMeth0;
import com.sun.fortress.interpreter.glue.NativeMeth1;
import com.sun.fortress.nodes.ObjectConstructor;

import java.lang.Long;
import java.util.List;


/**
 * Functions from NN64.
 */
public class UnsignedLong extends NativeConstructor {

    public UnsignedLong(Environment env, FTypeObject selfType, ObjectConstructor def) {
        super(env, selfType, def);
    }

    protected FNativeObject makeNativeObject(List<FValue> args, NativeConstructor con) {
        FNN64.setConstructor(this);
        return FNN64.ZERO;
    }

    static private abstract class UU2B extends NativeMeth1 {
        protected abstract boolean f(long x, long y);

        public final FValue applyMethod(FObject x, FValue y) {
            return FBool.make(f(x.getNN64(), y.getNN64()));
        }
    }

    static private abstract class U2U extends NativeMeth0 {
        protected abstract long f(long x);

        public final FValue applyMethod(FObject x) {
            return FNN64.make(f(x.getNN64()));
        }
    }

    static private abstract class U2L extends NativeMeth0 {
        protected abstract long f(long x);

        public final FValue applyMethod(FObject x) {
            return FLong.make(f(x.getNN64()));
        }
    }

    static private abstract class U2N extends NativeMeth0 {
        protected abstract int f(long x);

        public final FValue applyMethod(FObject x) {
            return FNN32.make(f(x.getNN64()));
        }
    }

    static private abstract class U2R extends NativeMeth0 {
        protected abstract double f(long x);

        public final FValue applyMethod(FObject x) {
            return FFloat.make(f(x.getNN64()));
        }
    }

    static private abstract class U2S extends NativeMeth0 {
        protected abstract java.lang.String f(long x);

        public final FValue applyMethod(FObject x) {
            return FString.make(f(x.getNN64()));
        }
    }

    static private abstract class UU2U extends NativeMeth1 {
        protected abstract long f(long x, long y);

        public final FValue applyMethod(FObject x, FValue y) {
            return FNN64.make(f(x.getNN64(), y.getNN64()));
        }
    }

    public static final class Negate extends U2U {
        protected long f(long x) {
            return Unsigned.subtract(0, x);
        }
    }

    public static final class Add extends UU2U {
        protected long f(long x, long y) {
            return Unsigned.add(x, y);
        }
    }

    public static final class Sub extends UU2U {
        protected long f(long x, long y) {
            return Unsigned.subtract(x, y);
        }
    }

    public static final class Mul extends UU2U {
        protected long f(long x, long y) {
            return Unsigned.multiplyToLong(x, y);
        }
    }

    public static final class Div extends UU2U {
        protected long f(long x, long y) {
            return Unsigned.divide(x, y);
        }
    }

    public static final class Rem extends UU2U {
        protected long f(long x, long y) {
            return Unsigned.remainder(x, y);
        }
    }

    public static final class Gcd extends UU2U {
        protected long f(long u, long v) {
            return gcd(u, v);
        }
    }

    public static final class Lcm extends UU2U {
        protected long f(long u, long v) {
            long g = gcd(u, v);
            return Unsigned.multiplyToLong(Unsigned.divide(u, g), v);
        }
    }

    public static final class Choose extends UU2U {
        protected long f(long u, long v) {
            return choose(u, v);
        }
    }

    public static final class Mod extends UU2U {
        protected long f(long u, long v) {
            /* MOD and REM are the same for natural numbers. */
            return Unsigned.remainder(u, v);
        }
    }

    public static final class BitAnd extends UU2U {
        protected long f(long u, long v) {
            return u & v;
        }
    }

    public static final class BitOr extends UU2U {
        protected long f(long u, long v) {
            return u | v;
        }
    }

    public static final class BitXor extends UU2U {
        protected long f(long u, long v) {
            return u ^ v;
        }
    }

    public static final class LShift extends UU2U {
        protected long f(long u, long v) {
            return ((v & ~63) == 0) ? (u << (int) v) : 0;
        }
    }

    public static final class RShift extends UU2U {
        protected long f(long u, long v) {
            return ((v & ~63) == 0) ? (u >>> (int) v) : 0;
        }
    }

    public static final class BitNot extends U2U {
        protected long f(long x) {
            return ~x;
        }
    }

    public static final class Eq extends UU2B {
        protected boolean f(long x, long y) {
            return x == y;
        }
    }

    public static final class Less extends UU2B {
        protected boolean f(long x, long y) {
            return Unsigned.lessThan(x, y);
        }
    }

    public static final class Partition extends U2U {
        protected long f(long u) {
            return Long.highestOneBit(u - 1);
        }
    }

    public static final class Pow extends NativeMeth1 {
        public FValue applyMethod(FObject x, FValue y) {
            long base = x.getNN64();
            long exp = y.getLong();
            if (exp < 0) {
                return FFloat.make(1.0 / (double) pow(base, -exp));
            } else {
                return FNN64.make(pow(base, exp));
            }
        }
    }

    public static final class ToLong extends U2L {
        protected long f(long x) {
            return x;
        }
    }

    public static final class FromLong extends U2N {
        protected int f(long x) {
            return (int) (x);
        }
    }

    public static final class ToString extends U2S {
        protected java.lang.String f(long x) {
            return Unsigned.toString(x);
        }
    }

    public static final class AsFloat extends U2R {
        protected double f(long x) {
            return Unsigned.toDouble(x);
        }
    }

    public static long gcd(long u, long v) {
        /* Thank you, Wikipedia. */
        /* But is this faster than just dividing on a modern
     * architecture? -Jan */
        if (u == 0) return v;
        if (v == 0) return u;
        long k = 0;
        long uv = u | v;
        for (; (uv & 1) == 0; uv >>>= 1) { /* While both u and v are even */
            k++;  /* Store their common power of 2 */
        }
        /* At this point either u or v (or both) is odd */
        u >>>= k;
        for (; (u & 1) == 0; u >>>= 1) {
        } /* Divide u by 2 until odd. */
        v >>>= k;
        for (; (v & 1) == 0; v >>>= 1) {
        } /* Divide v by 2 until odd. */
        while (true) {
            /* u and v are both odd */
            if (Unsigned.greaterThanOrEqual(u, v)) {
                u = (u - v) >>> 1;
                if (u == 0) return (v << k);
                for (; (u & 1) == 0; u >>>= 1) {
                } /* Divide u by 2 until odd. */
            } else {
                /* u and v both odd, v > u */
                v = (v - u) >>> 1;
                for (; (v & 1) == 0; v >>>= 1) {
                } /* Divide v by 2 until odd. */
            }
        }
    }

    public static long choose(long n, long k) {
        if (Unsigned.greaterThan(k, Unsigned.divide(n, 2))) k = Unsigned.subtract(n, k);
        if (k == 0) return 1;
        if (k == 1) return n;
        // k <= n/2
        // Will multiply (k) terms, n-k+1 through n-k+k
        // will divide by k terms, 1 through k
        // Note that if we divide after multiplying, it will
        // always be evenly divisible.
        // Proof: when we divide by k, we've multiplied by k consecutive integers,
        // at least one of which will be a multiple of k.
        long accum = 1;
        for (long j = 1; j <= k; j++) {
            long m = n - k + j;
            accum = Unsigned.multiplyToLong(accum, m);
            accum = Unsigned.divide(accum, j);
        }
        return accum;
    }

    public static long pow(long x, long y) {
        long r = 1;
        while (y > 0) {
            if ((y & 1) != 0) {
                r *= x;
                y--;
            }
            x *= x;
            y >>>= 1;
        }
        return r;
    }

    @Override
    protected void unregister() {
        FNN64.resetConstructor();

    }

}
