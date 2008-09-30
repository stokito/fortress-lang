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

package com.sun.fortress.interpreter.glue.prim;

import java.math.BigInteger;
import java.util.List;

import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.interpreter.evaluator.types.FTypeObject;
import com.sun.fortress.interpreter.evaluator.values.FBigNum;
import com.sun.fortress.interpreter.evaluator.values.FBool;
import com.sun.fortress.interpreter.evaluator.values.FFloat;
import com.sun.fortress.interpreter.evaluator.values.FInt;
import com.sun.fortress.interpreter.evaluator.values.FLong;
import com.sun.fortress.interpreter.evaluator.values.FNN64;
import com.sun.fortress.interpreter.evaluator.values.FObject;
import com.sun.fortress.interpreter.evaluator.values.FString;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.interpreter.evaluator.values.NativeConstructor;
import com.sun.fortress.interpreter.glue.NativeFn0;
import com.sun.fortress.interpreter.glue.NativeMeth0;
import com.sun.fortress.interpreter.glue.NativeMeth1;
import com.sun.fortress.nodes.GenericWithParams;


/**
 * Functions from ZZ64.
 */
public class Long extends NativeConstructor {

public Long(Environment env, FTypeObject selfType, GenericWithParams def) {
    super(env, selfType, def);
}

protected FNativeObject makeNativeObject(List<FValue> args,
                                         NativeConstructor con) {
    FLong.setConstructor(this);
    return FLong.ZERO;
}

static private abstract class LL2B extends NativeMeth1 {
    protected abstract boolean f(long x, long y);
    protected final FValue act(FObject x, FValue y) {
        return FBool.make(f(x.getLong(),y.getLong()));
    }
}
static private abstract class L2L extends NativeMeth0 {
    protected abstract long f(long x);
    protected final FValue act(FObject x) {
        return FLong.make(f(x.getLong()));
    }
}

static private abstract class L2U extends NativeMeth0 {
    protected abstract long f(long x);
    protected final FValue act(FObject x) {
        return FNN64.make(f(x.getLong()));
    }
}

static private abstract class L2I extends NativeMeth0 {
    protected abstract int f(long x);
    protected final FValue act(FObject x) {
        return FInt.make(f(x.getLong()));
    }
}
static private abstract class L2Z extends NativeMeth0 {
    protected abstract BigInteger f(long x);
    protected final FValue act(FObject x) {
        return FBigNum.make(f(x.getLong()));
    }
}
static private abstract class L2R extends NativeMeth0 {
    protected abstract double f(long x);
    protected final FValue act(FObject x) {
        return FFloat.make(f(x.getLong()));
    }
}
static private abstract class L2S extends NativeMeth0 {
    protected abstract java.lang.String f(long x);
    protected final FValue act(FObject x) {
        return FString.make(f(x.getLong()));
    }
}
static private abstract class LL2L extends NativeMeth1 {
    protected abstract long f(long x, long y);
    protected final FValue act(FObject x, FValue y) {
        return FLong.make(f(x.getLong(),y.getLong()));
    }
}

public static final class Negate extends L2L {
    protected long f(long x) { return -x; }
}
public static final class Add extends LL2L {
    protected long f(long x, long y) { return x + y; }
}
public static final class Sub extends LL2L {
    protected long f(long x, long y) { return x - y; }
}
public static final class Mul extends LL2L {
    protected long f(long x, long y) { return x * y; }
}
public static final class Div extends LL2L {
    protected long f(long x, long y) { return x / y; }
}
public static final class Rem extends LL2L {
    protected long f(long x, long y) { return x % y; }
}
public static final class Gcd extends LL2L {
    protected long f(long u, long v) {
        return Int.gcd(u,v);
    }
}
public static final class Lcm extends LL2L {
    protected long f(long u, long v) {
        long g = Int.gcd(u,v);
        return (u/g)*v;
    }
}
public static final class Choose extends LL2L {
    protected long f(long u, long v) {
        return Int.choose(u,v);
    }
}
public static final class Mod extends LL2L {
    protected long f(long u, long v) {
        return Int.mod(u,v);
    }
}
public static final class BitAnd extends LL2L {
    protected long f(long u, long v) {
        return u & v;
    }
}
public static final class BitOr extends LL2L {
    protected long f(long u, long v) {
        return u | v;
    }
}
public static final class BitXor extends LL2L {
    protected long f(long u, long v) {
        return u ^ v;
    }
}
public static final class LShift extends LL2L {
    protected long f(long u, long v) {
        return ((v&~63)==0)?(u << (int)v):0;
    }
}
public static final class RShift extends LL2L {
    protected long f(long u, long v) {
        return ((v&~63)==0)?(u >> (int)v):(u>>63);
    }
}
public static final class BitNot extends L2L {
    protected long f(long x) { return ~x; }
}
public static final class Eq extends LL2B {
    protected boolean f(long x, long y) { return x==y; }
}
public static final class Less extends LL2B {
    protected boolean f(long x, long y) { return x<y; }
}
public static final class Partition extends L2L {
    protected long f(long u) {
        long m = (u-1) >> 1;
        m |= m >> 1;
        m |= m >> 2;
        m |= m >> 4;
        m |= m >> 8;
        m |= m >> 16;
        m |= m >> 32;
        return m+1;
    }
}
public static final class Pow extends NativeMeth1 {
    protected FValue act(FObject x, FValue y) {
        long base = x.getLong();
        long exp = y.getLong();
        if (exp < 0) {
            return FFloat.make(1.0 / (double)Int.pow(base,-exp));
        } else {
            return FLong.make(Int.pow(base,exp));
        }
    }
}

public static final class ToUnsignedLong extends L2U {
    protected long f(long x) { return x; }
}
public static final class FromLong extends L2I {
    protected int f(long x) { return Int.rc(x); }
}

public static final class ToBigNum extends L2Z {
    protected BigInteger f(long x) { return BigInteger.valueOf(x); }
}

public static final class ToString extends L2S {
    protected java.lang.String f(long x) { return java.lang.Long.toString(x); }
}
public static final class AsFloat extends L2R {
    protected double f(long x) { return (double)x; }
}

public static final class NanoTime extends NativeFn0 {
    protected FValue act() {
        long res = System.nanoTime();
        return FLong.make(res);
    }
}

public static long gcd(long u, long v) {
    /* Thank you, Wikipedia. */
    /* But is this faster than just dividing on a modern
     * architecture? -Jan */
    long k = 0;
    if (u <= 0) {
        if (u == 0)
            return v;
        u = -u;
    }
    if (v <= 0) {
        if (v == 0)
            return u;
        v = -v;
    }
    long uv = u|v;
    for (;(uv & 1) == 0; uv>>>=1) { /* While both u and v are even */
        k++;  /* Store their common power of 2 */
    }
    /* At this point either u or v (or both) is odd */
    u >>>= k;
    for (;(u & 1)==0; u >>>= 1) {} /* Divide u by 2 until odd. */
    v >>>= k;
    for (;(v & 1)==0; v >>>= 1) {} /* Divide v by 2 until odd. */
    while (true) {
        /* u and v are both odd */
        if (u >= v) {
            u = (u - v) >>> 1;
            if (u == 0) return (v<<k);
            for (;(u & 1)==0; u >>>= 1) {} /* Divide u by 2 until odd. */
        } else {
            /* u and v both odd, v > u */
            v = (v - u) >>> 1;
            for (;(v & 1)==0; v >>>= 1) {} /* Divide v by 2 until odd. */
        }
    }
}

public static long choose(long n, long k) {
    if (k > n/2) k = n - k;
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
    for (long j= 1; j <= k; j++) {
        long m = n-k+j;
        accum = accum * m;
        accum = accum / j;
    }
    return accum;
}

public static long mod(long xi, long yi) {
    long ri = xi % yi;
    if (ri == 0)
        return 0;

    if (yi > 0) {
        if (xi >= 0)
            return ri;
        return ri + yi;
    } else {
        if (xi >= 0)
            return ri + yi;
        return ri;
    }
}

public static long pow(long x, long y) {
    long r = 1;
    while (y > 0) {
        if ((y & 1)!=0) {
            r *= x;
            y--;
        }
        x *= x;
        y >>>= 1;
    }
    return r;
}

}
