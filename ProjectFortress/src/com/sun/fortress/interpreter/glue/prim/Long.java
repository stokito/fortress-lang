/*******************************************************************************
    Copyright 2007 Sun Microsystems, Inc.,
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

import com.sun.fortress.interpreter.evaluator.values.FFloat;
import com.sun.fortress.interpreter.evaluator.values.FLong;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.interpreter.glue.NativeFn0;
import com.sun.fortress.interpreter.glue.NativeFn2;


/**
 * Functions from ZZ64.
 */
public class Long {

public static final class Negate extends Util.L2L {
    protected long f(long x) { return -x; }
}
public static final class Add extends Util.LL2L {
    protected long f(long x, long y) { return x + y; }
}
public static final class Sub extends Util.LL2L {
    protected long f(long x, long y) { return x - y; }
}
public static final class Mul extends Util.LL2L {
    protected long f(long x, long y) { return x * y; }
}
public static final class Div extends Util.LL2L {
    protected long f(long x, long y) { return x / y; }
}
public static final class Rem extends Util.LL2L {
    protected long f(long x, long y) { return x % y; }
}
public static final class Gcd extends Util.LL2L {
    protected long f(long u, long v) {
        return Int.gcd(u,v);
    }
}
public static final class Lcm extends Util.LL2L {
    protected long f(long u, long v) {
        long g = Int.gcd(u,v);
        return (u/g)*v;
    }
}
public static final class Choose extends Util.LL2L {
    protected long f(long u, long v) {
        return Int.choose(u,v);
    }
}
public static final class Mod extends Util.LL2L {
    protected long f(long u, long v) {
        return Int.mod(u,v);
    }
}
public static final class BitAnd extends Util.LL2L {
    protected long f(long u, long v) {
        return u & v;
    }
}
public static final class BitOr extends Util.LL2L {
    protected long f(long u, long v) {
        return u | v;
    }
}
public static final class BitXor extends Util.LL2L {
    protected long f(long u, long v) {
        return u ^ v;
    }
}
public static final class LShift extends Util.LL2L {
    protected long f(long u, long v) {
        return ((v&~63)==0)?(u << (int)v):0;
    }
}
public static final class RShift extends Util.LL2L {
    protected long f(long u, long v) {
        return ((v&~63)==0)?(u >> (int)v):(u>>63);
    }
}
public static final class BitNot extends Util.L2L {
    protected long f(long x) { return ~x; }
}
public static final class Eq extends Util.LL2B {
    protected boolean f(long x, long y) { return x==y; }
}
public static final class LessEq extends Util.LL2B {
    protected boolean f(long x, long y) { return x<=y; }
}
public static final class Pow extends NativeFn2 {
    protected FValue act(FValue x, FValue y) {
        long base = x.getLong();
        long exp = y.getLong();
        if (exp < 0) {
            return FFloat.make(1.0 / (double)Int.pow(base,-exp));
        } else {
            return FLong.make(Int.pow(base,exp));
        }
    }
}
public static final class ToLong extends Util.Z2L {
    protected long f(int x) { return (long)x; }
}

public static final class NanoTime extends NativeFn0 {
    protected FValue act() {
        long res = System.nanoTime();
        return FLong.make(res);
    }
}

}
