/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.glue.prim;

import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.interpreter.evaluator.types.FTypeObject;
import com.sun.fortress.interpreter.evaluator.values.*;
import com.sun.fortress.interpreter.glue.NativeMeth0;
import com.sun.fortress.interpreter.glue.NativeMeth1;
import com.sun.fortress.nodes.ObjectConstructor;

import java.math.BigInteger;
import java.util.List;


public class BigNum extends NativeConstructor {

    public BigNum(Environment env, FTypeObject selfType, ObjectConstructor def) {
        super(env, selfType, def);
    }

    protected FNativeObject makeNativeObject(List<FValue> args, NativeConstructor con) {
        FBigNum.setConstructor(con);
        return FBigNum.ZERO;
    }

    @Override
    protected void unregister() {
        FBigNum.resetConstructor();
    }

    private static BigInteger toB(FValue x) {
        if (x instanceof FBigNum) {
            return ((FBigNum) x).getBigInteger();
        } else if (x instanceof FIntLiteral) {
            return ((FIntLiteral) x).getLit();
        } else {
            return BigInteger.valueOf(x.getLong());
        }
    }

    public static abstract class Z2Z extends NativeMeth0 {
        protected abstract BigInteger f(BigInteger x);

        public final FValue applyMethod(FObject x) {
            return FBigNum.make(f(toB(x)));
        }
    }

    static private abstract class Z2R extends NativeMeth0 {
        protected abstract double f(BigInteger x);

        public final FValue applyMethod(FObject x) {
            return FFloat.make(f(toB(x)));
        }
    }

    public static abstract class Z2S extends NativeMeth0 {
        protected abstract java.lang.String f(BigInteger x);

        public final FValue applyMethod(FObject x) {
            return FString.make(f(toB(x)));
        }
    }

    public static abstract class ZZ2Z extends NativeMeth1 {
        protected abstract BigInteger f(BigInteger x, BigInteger y);

        public final FValue applyMethod(FObject x, FValue y) {
            return FBigNum.make(f(toB(x), toB(y)));
        }
    }

    public static abstract class ZZ2B extends NativeMeth1 {
        protected abstract boolean f(BigInteger x, BigInteger y);

        public final FValue applyMethod(FObject x, FValue y) {
            return FBool.make(f(toB(x), toB(y)));
        }
    }

    public static abstract class ZZ2I extends NativeMeth1 {
        protected abstract int f(BigInteger x, BigInteger y);

        public final FValue applyMethod(FObject x, FValue y) {
            return FInt.make(f(toB(x), toB(y)));
        }
    }

    public static abstract class ZL2Z extends NativeMeth1 {
        protected abstract BigInteger f(BigInteger x, long y);

        public final FValue applyMethod(FObject x, FValue y) {
            return FBigNum.make(f(toB(x), y.getLong()));
        }
    }

    public static abstract class ZL2N extends NativeMeth1 {
        protected abstract FValue f(BigInteger x, long y);

        public final FValue applyMethod(FObject x, FValue y) {
            return f(toB(x), y.getLong());
        }
    }

    public static abstract class Z2I extends NativeMeth0 {
        protected abstract int f(BigInteger x);

        public final FValue applyMethod(FObject x) {
            return FInt.make(f(toB(x)));
        }
    }

    public static abstract class Z2L extends NativeMeth0 {
        protected abstract long f(BigInteger x);

        public final FValue applyMethod(FObject x) {
            return FLong.make(f(toB(x)));
        }
    }

    public static final class Negate extends Z2Z {
        protected BigInteger f(BigInteger x) {
            return x.negate();
        }
    }

    public static final class Add extends ZZ2Z {
        protected BigInteger f(BigInteger x, BigInteger y) {
            return x.add(y);
        }
    }

    public static final class Sub extends ZZ2Z {
        protected BigInteger f(BigInteger x, BigInteger y) {
            return x.subtract(y);
        }
    }

    public static final class Mul extends ZZ2Z {
        protected BigInteger f(BigInteger x, BigInteger y) {
            return x.multiply(y);
        }
    }

    public static final class Div extends ZZ2Z {
        protected BigInteger f(BigInteger x, BigInteger y) {
            return x.divide(y);
        }
    }

    public static final class Rem extends ZZ2Z {
        protected BigInteger f(BigInteger x, BigInteger y) {
            return x.remainder(y);
        }
    }

    public static final class Gcd extends ZZ2Z {
        protected BigInteger f(BigInteger u, BigInteger v) {
            return u.gcd(v);
        }
    }

    public static final class Lcm extends ZZ2Z {
        protected BigInteger f(BigInteger u, BigInteger v) {
            BigInteger g = u.gcd(v);
            /* Divide smaller by g, then multiply.  Quick whiteboard
    * computation says this will be less work. */
            if (u.compareTo(v) > 0) {
                /* Smaller into u. */
                BigInteger t = u;
                u = v;
                v = t;
            }
            return (u.divide(g).multiply(v));
        }
    }

    public static final class Choose extends ZZ2Z {
        protected BigInteger f(BigInteger u, BigInteger v) {
            return choose(u, v);
        }
    }

    public static final class Mod extends ZZ2Z {
        protected BigInteger f(BigInteger u, BigInteger v) {
            if (v.compareTo(BigInteger.ZERO) < 0) {
                return u.negate().mod(v.negate()).negate();
            } else {
                return u.mod(v);
            }
        }
    }

    public static final class BitAnd extends ZZ2Z {
        protected BigInteger f(BigInteger u, BigInteger v) {
            return u.and(v);
        }
    }

    public static final class BitOr extends ZZ2Z {
        protected BigInteger f(BigInteger u, BigInteger v) {
            return u.or(v);
        }
    }

    public static final class BitXor extends ZZ2Z {
        protected BigInteger f(BigInteger u, BigInteger v) {
            return u.xor(v);
        }
    }

    public static final class LShift extends ZL2Z {
        protected BigInteger f(BigInteger u, long v) {
            return u.shiftLeft((int) v);
        }
    }

    public static final class RShift extends ZL2Z {
        protected BigInteger f(BigInteger u, long v) {
            return u.shiftRight((int) v);
        }
    }

    public static final class BitNot extends Z2Z {
        protected BigInteger f(BigInteger x) {
            return x.not();
        }
    }

    public static final class Eq extends ZZ2B {
        protected boolean f(BigInteger x, BigInteger y) {
            return x.equals(y);
        }
    }

    public static final class Cmp extends ZZ2I {
        protected int f(BigInteger x, BigInteger y) {
            return x.compareTo(y);
        }
    }

    public static final class ToString extends Z2S {
        protected java.lang.String f(BigInteger x) {
            return x.toString();
        }
    }

    public static final class AsFloat extends Z2R {
        protected double f(BigInteger x) {
            return x.doubleValue();
        }
    }

    public static final class ToZZ32 extends Z2I {
        protected int f(BigInteger x) {
            return x.intValue();
        }
    }

    public static final class ToZZ64 extends Z2L {
        protected long f(BigInteger x) {
            return x.longValue();
        }
    }

    public static final class Pow extends ZL2N {
        protected FValue f(BigInteger u, long v) {
            if (v < 0) {
                return FFloat.make(1.0 / u.pow((int) -v).doubleValue());
            } else {
                return FBigNum.make(u.pow((int) v));
            }
        }
    }

    private static BigInteger choose(BigInteger n, BigInteger k) {
        BigInteger nmk = n.subtract(k);
        if (nmk.compareTo(k) < 0) {
            BigInteger t = k;
            k = nmk;
            nmk = t;
        }
        // Will multiply (k) terms, n-k+1 through n-k+k
        // will divide by k terms, 1 through k
        // Note that if we divide after multiplying, it will
        // always be evenly divisible.
        // Proof: when we divide by k, we've multiplied by k consecutive integers,
        // at least one of which will be a multiple of k.
        BigInteger accum = BigInteger.ONE;
        for (BigInteger j = BigInteger.ONE; j.compareTo(k) <= 0; j = j.add(BigInteger.ONE)) {
            nmk = nmk.add(BigInteger.ONE);
            accum = accum.multiply(nmk).divide(j);
        }
        return accum;
    }

}
