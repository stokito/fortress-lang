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

import static com.sun.fortress.exceptions.ProgramError.error;

import java.util.List;

import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.interpreter.evaluator.types.FTypeObject;
import com.sun.fortress.interpreter.evaluator.values.FBool;
import com.sun.fortress.interpreter.evaluator.values.FFloat;
import com.sun.fortress.interpreter.evaluator.values.FInt;
import com.sun.fortress.interpreter.evaluator.values.FNN32;
import com.sun.fortress.interpreter.evaluator.values.FLong;
import com.sun.fortress.interpreter.evaluator.values.FObject;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.interpreter.evaluator.values.NativeConstructor;
import com.sun.fortress.interpreter.glue.NativeMeth0;
import com.sun.fortress.interpreter.glue.NativeMeth1;
import com.sun.fortress.nodes.GenericWithParams;

/**
 * Functions from ZZ32.
 */
public class ZZ32 extends NativeConstructor {

public ZZ32(Environment env, FTypeObject selfType, GenericWithParams def) {
    super(env,selfType,def);
}

protected FNativeObject makeNativeObject(List<FValue> args,
                                         NativeConstructor con) {
    FInt.setConstructor(this);
    return FInt.ZERO;
}

static private abstract class ZZ2B extends NativeMeth1 {
    protected abstract boolean f(int x, int y);
    protected final FValue act(FObject x, FValue y) {
        return FBool.make(f(x.getInt(),y.getInt()));
    }
}

static private abstract class Z2Z extends NativeMeth0 {
    protected abstract int f(int x);
    protected final FValue act(FObject x) {
        return FInt.make(f(x.getInt()));
    }
}
static private abstract class Z2N extends NativeMeth0 {
    protected abstract int f(int x);
    protected final FValue act(FObject x) {
        return FNN32.make(f(x.getInt()));
    }
}
static private abstract class Z2L extends NativeMeth0 {
    protected abstract long f(int x);
    protected final FValue act(FObject x) {
        return FLong.make(f(x.getInt()));
    }
}
static private abstract class ZZ2Z extends NativeMeth1 {
    protected abstract int f(int x, int y);
    protected final FValue act(FObject x, FValue y) {
        return FInt.make(f(x.getInt(),y.getInt()));
    }
}
static private abstract class ZL2Z extends NativeMeth1 {
    protected abstract int f(int x, long y);
    protected final FValue act(FObject x, FValue y) {
        return FInt.make(f(x.getInt(),y.getLong()));
    }
}

public static final class Negate extends Z2Z {
    protected int f(int x) { return -x; }
}
public static final class Add extends ZZ2Z {
    protected int f(int x, int y) { return x + y; }
}
public static final class Sub extends ZZ2Z {
    protected int f(int x, int y) { return x - y; }
}
public static final class Mul extends ZZ2Z {
    protected int f(int x, int y) { return x * y; }
}
public static final class Div extends ZZ2Z {
    protected int f(int x, int y) { return x / y; }
}
public static final class Rem extends ZZ2Z {
    protected int f(int x, int y) { return x % y; }
}
public static final class Gcd extends ZZ2Z {
    protected int f(int u, int v) {
        return (int)Long.gcd(u,v);
    }
}
public static final class Lcm extends ZZ2Z {
    protected int f(int u, int v) {
        int g = (int)Long.gcd(u,v);
        return (u/g)*v;
    }
}
public static final class Choose extends ZZ2Z {
    protected int f(int u, int v) {
        return rc(Long.choose(u,v));
    }
}
public static final class Mod extends ZZ2Z {
    protected int f(int u, int v) {
        return (int)Long.mod(u,v);
    }
}
public static final class BitAnd extends ZZ2Z {
    protected int f(int u, int v) {
        return u & v;
    }
}
public static final class BitOr extends ZZ2Z {
    protected int f(int u, int v) {
        return u | v;
    }
}
public static final class BitXor extends ZZ2Z {
    protected int f(int u, int v) {
        return u ^ v;
    }
}
public static final class LShift extends ZL2Z {
    protected int f(int u, long v) {
        return ((v&~31)==0)?(u << (int)v):0;
    }
}
public static final class RShift extends ZL2Z {
    protected int f(int u, long v) {
        return ((v&~31)==0)?(u >> (int)v):(u>>31);
    }
}
public static final class BitNot extends Z2Z {
    protected int f(int x) { return ~x; }
}
public static final class Eq extends ZZ2B {
    protected boolean f(int x, int y) { return x==y; }
}
public static final class Less extends ZZ2B {
    protected boolean f(int x, int y) { return x<y; }
}
public static final class Partition extends Z2Z {
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
public static final class Pow extends NativeMeth1 {
    protected FValue act(FObject x, FValue y) {
        int base = x.getInt();
        long exp = y.getLong();
        if (exp < 0) {
            return FFloat.make(1.0 / (double)Long.pow(base,-exp));
        } else {
            return FInt.make(rc(Long.pow(base,exp)));
        }
    }
}
public static final class ToNN32 extends Z2N {
    protected int f(int x) { return x; }
}
public static final class ToLong extends Z2L {
    protected long f(int x) { return (long)x; }
}

public static int rc(long i) {
    int r = (int) i;
    if ((long) r != i) {
        error("Overflow of ZZ32 "+i);
    }
    return r;
}

@Override
protected void unregister() {
    FInt.resetConstructor();
}

}
