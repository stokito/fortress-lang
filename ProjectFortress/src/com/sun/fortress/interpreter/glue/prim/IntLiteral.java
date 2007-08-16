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

import java.math.BigInteger;

import com.sun.fortress.interpreter.evaluator.values.FBool;
import com.sun.fortress.interpreter.evaluator.values.FInt;
import com.sun.fortress.interpreter.evaluator.values.FIntLiteral;
import com.sun.fortress.interpreter.evaluator.values.FLong;
import com.sun.fortress.interpreter.evaluator.values.FFloat;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.interpreter.glue.NativeFn1;
import com.sun.fortress.interpreter.glue.NativeFn2;

import static com.sun.fortress.interpreter.evaluator.ProgramError.error;

/**
 * Functions from IntLiteral.
 */
public class IntLiteral {

private static BigInteger toB(FValue x) {
    if (x instanceof FIntLiteral) {
        return ((FIntLiteral)x).getLit();
    } else {
        return error("Non-IntLiteral for IntLiteral primitive");
    }
}

public static abstract class K2K extends NativeFn1 {
    protected abstract BigInteger f(BigInteger x);
    protected final FValue act(FValue x) {
        return FIntLiteral.make(f(toB(x)));
    }
}
public static abstract class KK2K extends NativeFn2 {
    protected abstract BigInteger f(BigInteger x, BigInteger y);
    protected final FValue act(FValue x, FValue y) {
        return FIntLiteral.make(f(toB(x),toB(y)));
    }
}
public static abstract class KK2B extends NativeFn2 {
    protected abstract boolean f(BigInteger x, BigInteger y);
    protected final FValue act(FValue x, FValue y) {
        return FBool.make(f(toB(x),toB(y)));
    }
}
public static abstract class KL2K extends NativeFn2 {
    protected abstract BigInteger f(BigInteger x, long y);
    protected final FValue act(FValue x, FValue y) {
        return FIntLiteral.make(f(toB(x),y.getLong()));
    }
}
public static abstract class KL2N extends NativeFn2 {
    protected abstract FValue f(BigInteger x, long y);
    protected final FValue act(FValue x, FValue y) {
        return f(toB(x),y.getLong());
    }
}

public static final class Negate extends K2K {
    protected BigInteger f(BigInteger x) { return x.negate(); }
}
public static final class Add extends KK2K {
    protected BigInteger f(BigInteger x, BigInteger y) { return x.add(y); }
}
public static final class Sub extends KK2K {
    protected BigInteger f(BigInteger x, BigInteger y) { return x.subtract(y); }
}
public static final class Mul extends KK2K {
    protected BigInteger f(BigInteger x, BigInteger y) { return x.multiply(y); }
}
public static final class Div extends KK2K {
    protected BigInteger f(BigInteger x, BigInteger y) { return x.divide(y); }
}
public static final class Rem extends KK2K {
    protected BigInteger f(BigInteger x, BigInteger y) { return x.remainder(y); }
}
public static final class Gcd extends KK2K {
    protected BigInteger f(BigInteger u, BigInteger v) {
        return u.gcd(v);
    }
}
public static final class Lcm extends KK2K {
    protected BigInteger f(BigInteger u, BigInteger v) {
        BigInteger g = u.gcd(v);
        /* Divide smaller by g, then multiply.  Quick whiteboard
         * computation says this will be less work. */
        if (u.compareTo(v) > 0) {
            /* Smaller into u. */
            BigInteger t = u; u = v; v = t;
        }
        return (u.divide(g).multiply(v));
    }
}
public static final class Choose extends KK2K {
    protected BigInteger f(BigInteger u, BigInteger v) {
        return choose(u,v);
    }
}
public static final class Mod extends KK2K {
    protected BigInteger f(BigInteger u, BigInteger v) {
        if (v.compareTo(BigInteger.ZERO) < 0) {
            return u.negate().mod(v.negate()).negate();
        } else {
            return u.mod(v);
        }
    }
}
public static final class BitAnd extends KK2K {
    protected BigInteger f(BigInteger u, BigInteger v) {
        return u.and(v);
    }
}
public static final class BitOr extends KK2K {
    protected BigInteger f(BigInteger u, BigInteger v) {
        return u.or(v);
    }
}
public static final class BitXor extends KK2K {
    protected BigInteger f(BigInteger u, BigInteger v) {
        return u.xor(v);
    }
}
public static final class LShift extends KL2K {
    protected BigInteger f(BigInteger u, long v) {
        return u.shiftLeft((int)v);
    }
}
public static final class RShift extends KL2K {
    protected BigInteger f(BigInteger u, long v) {
        return u.shiftRight((int)v);
    }
}
public static final class BitNot extends K2K {
    protected BigInteger f(BigInteger x) { return x.not(); }
}
public static final class Eq extends KK2B {
    protected boolean f(BigInteger x, BigInteger y) { return x.equals(y); }
}
public static final class LessEq extends KK2B {
    protected boolean f(BigInteger x, BigInteger y) { return x.compareTo(y)<=0; }
}
public static final class Pow extends KL2N {
    protected FValue f(BigInteger u, long v) {
        if (v < 0) {
            return FFloat.make(1.0 / u.pow((int)-v).doubleValue());
        } else {
            return FIntLiteral.make(u.pow((int)v));
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
    for (BigInteger j = BigInteger.ONE;
         j.compareTo(k)<=0;
         j=j.add(BigInteger.ONE)) {
        nmk = nmk.add(BigInteger.ONE);
        accum = accum.multiply(nmk).divide(j);
    }
    return accum;
}

}
