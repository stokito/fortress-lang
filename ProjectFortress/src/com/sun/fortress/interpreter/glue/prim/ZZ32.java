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

import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.interpreter.evaluator.values.NativeConstructor;
import com.sun.fortress.interpreter.evaluator.values.FBool;
import com.sun.fortress.interpreter.evaluator.values.FFloat;
import com.sun.fortress.interpreter.evaluator.values.FInt;
import com.sun.fortress.interpreter.evaluator.values.FIntLiteral;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.interpreter.evaluator.types.FTypeObject;
import com.sun.fortress.nodes.GenericWithParams;
import com.sun.fortress.interpreter.glue.NativeFn1;
import com.sun.fortress.interpreter.glue.NativeFn2;

import static com.sun.fortress.interpreter.evaluator.ProgramError.error;

/**
 * Functions from ZZ32.
 */
public class ZZ32 extends NativeConstructor {

public ZZ32(BetterEnv env, FTypeObject selfType, GenericWithParams def) {
    super(env,selfType,def);
}

protected FNativeObject makeNativeObject(List<FValue> args,
                                         NativeConstructor con) {
    FInt.setConstructor(this);
    return FInt.ZERO;
}

public static final class Negate extends Util.Z2Z {
    protected int f(int x) { return -x; }
}
public static final class Add extends Util.ZZ2Z {
    protected int f(int x, int y) { return x + y; }
}
public static final class Sub extends Util.ZZ2Z {
    protected int f(int x, int y) { return x - y; }
}
public static final class Mul extends Util.ZZ2Z {
    protected int f(int x, int y) { return x * y; }
}
public static final class Div extends Util.ZZ2Z {
    protected int f(int x, int y) { return x / y; }
}
public static final class Rem extends Util.ZZ2Z {
    protected int f(int x, int y) { return x % y; }
}
public static final class Gcd extends Util.ZZ2Z {
    protected int f(int u, int v) {
        return (int)gcd(u,v);
    }
}
public static final class Lcm extends Util.ZZ2Z {
    protected int f(int u, int v) {
        int g = (int)gcd(u,v);
        return (u/g)*v;
    }
}
public static final class Choose extends Util.ZZ2Z {
    protected int f(int u, int v) {
        return rc(choose(u,v));
    }
}
public static final class Mod extends Util.ZZ2Z {
    protected int f(int u, int v) {
        return (int)mod(u,v);
    }
}
public static final class BitAnd extends Util.ZZ2Z {
    protected int f(int u, int v) {
        return u & v;
    }
}
public static final class BitOr extends Util.ZZ2Z {
    protected int f(int u, int v) {
        return u | v;
    }
}
public static final class BitXor extends Util.ZZ2Z {
    protected int f(int u, int v) {
        return u ^ v;
    }
}
public static final class LShift extends Util.ZL2Z {
    protected int f(int u, long v) {
        return ((v&~31)==0)?(u << (int)v):0;
    }
}
public static final class RShift extends Util.ZL2Z {
    protected int f(int u, long v) {
        return ((v&~31)==0)?(u >> (int)v):(u>>31);
    }
}
public static final class BitNot extends Util.Z2Z {
    protected int f(int x) { return ~x; }
}
public static final class Eq extends Util.ZZ2B {
    protected boolean f(int x, int y) { return x==y; }
}
public static final class LessEq extends Util.ZZ2B {
    protected boolean f(int x, int y) { return x<=y; }
}
public static final class Partition extends Util.Z2Z {
    protected int f(int u) {
        int m = (u-1) >> 1;
        m |= m >> 1;
        m |= m >> 2;
        m |= m >> 4;
        m |= m >> 8;
        m |= m >> 16;
        return m+1;
    }
}
public static final class Pow extends NativeFn2 {
    protected FValue act(FValue x, FValue y) {
        int base = x.getInt();
        long exp = y.getLong();
        if (exp < 0) {
            return FFloat.make(1.0 / (double)pow(base,-exp));
        } else {
            return FInt.make(rc(pow(base,exp)));
        }
    }
}
public static final class FromLong extends Util.L2Z {
    protected int f(long x) { return ZZ32.rc(x); }
}

public static int rc(long i) {
    int r = (int) i;
    if ((long) r != i) {
        error("Overflow of ZZ32 "+i);
    }
    return r;
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
