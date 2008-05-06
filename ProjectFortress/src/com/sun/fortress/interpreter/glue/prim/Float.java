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

import java.util.List;

import com.sun.fortress.numerics.DirectedRounding;

import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.interpreter.evaluator.values.NativeConstructor;
import com.sun.fortress.interpreter.evaluator.values.FFloat;
import com.sun.fortress.interpreter.evaluator.values.FBool;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.interpreter.evaluator.values.FObject;
import com.sun.fortress.interpreter.evaluator.types.FTypeObject;
import com.sun.fortress.interpreter.glue.NativeMeth1;
import com.sun.fortress.nodes.GenericWithParams;

/**
 * Functions from RR64.
 */
public class Float extends NativeConstructor {

public Float(BetterEnv env, FTypeObject selfType, GenericWithParams def) {
    super(env, selfType, def);
}

protected FNativeObject makeNativeObject(List<FValue> args,
                                         NativeConstructor con) {
    FFloat.setConstructor(this);
    return FFloat.ZERO;
}

static private abstract class RR2B extends NativeMeth1 {
    protected abstract boolean f(double x, double y);
    protected final FValue act(FObject x, FValue y) {
        return FBool.make(f(x.getFloat(),y.getFloat()));
    }
}
static private abstract class RR2R extends NativeMeth1 {
    protected abstract double f(double x, double y);
    protected final FValue act(FObject x, FValue y) {
        return FFloat.make(f(x.getFloat(),y.getFloat()));
    }
}

public static final class Negate extends Util.R2R {
    protected double f(double x) { return -x; }
}
public static final class Add extends Util.RR2R {
    protected double f(double x, double y) { return x + y; }
}
public static final class Sub extends Util.RR2R {
    protected double f(double x, double y) { return x - y; }
}
public static final class Mul extends Util.RR2R {
    protected double f(double x, double y) { return x * y; }
}
public static final class Div extends Util.RR2R {
    protected double f(double x, double y) { return x / y; }
}
public static final class Sqrt extends Util.R2R {
    protected double f(double x) { return Math.sqrt(x); }
}
public static final class AddUp extends Util.RR2R {
    protected double f(double x, double y) { return DirectedRounding.addUp(x,y); }
}
public static final class SubUp extends Util.RR2R {
    protected double f(double x, double y) { return DirectedRounding.subtractUp(x,y); }
}
public static final class MulUp extends Util.RR2R {
    protected double f(double x, double y) { return DirectedRounding.multiplyUp(x,y); }
}
public static final class DivUp extends Util.RR2R {
    protected double f(double x, double y) { return DirectedRounding.divideUp(x,y); }
}
public static final class SqrtUp extends Util.R2R {
    protected double f(double x) { return DirectedRounding.sqrtUp(x); }
}
public static final class AddDown extends Util.RR2R {
    protected double f(double x, double y) { return DirectedRounding.addDown(x,y); }
}
public static final class SubDown extends Util.RR2R {
    protected double f(double x, double y) { return DirectedRounding.subtractDown(x,y);}
}
public static final class MulDown extends Util.RR2R {
    protected double f(double x, double y) { return DirectedRounding.multiplyDown(x,y);}
}
public static final class DivDown extends Util.RR2R {
    protected double f(double x, double y) { return DirectedRounding.divideDown(x,y); }
}
public static final class SqrtDown extends Util.R2R {
    protected double f(double x) { return DirectedRounding.sqrtDown(x); }
}
public static final class Eq extends RR2B {
    protected boolean f(double x, double y) { return x==y; }
}
public static final class NEq extends RR2B {
    protected boolean f(double x, double y) { return x!=y; }
}
public static final class Less extends RR2B {
    protected boolean f(double x, double y) { return x<y; }
}
public static final class LessEq extends RR2B {
    protected boolean f(double x, double y) { return x<=y; }
}
public static final class Greater extends RR2B {
    protected boolean f(double x, double y) { return x>y; }
}
public static final class GreaterEq extends RR2B {
    protected boolean f(double x, double y) { return x>=y; }
}
public static final class Min extends RR2R {
    protected double f(double x, double y) { return Math.min(x,y); }
}
public static final class Max extends RR2R {
    protected double f(double x, double y) { return Math.max(x,y); }
}
public static final class Pow extends Util.RR2R {
    protected double f(double x, double y) {
        return Math.pow(x,y);
    }
}
public static final class Sin extends Util.R2R {
    protected double f(double x) { return Math.sin(x); }
}
public static final class Cos extends Util.R2R {
    protected double f(double x) { return Math.cos(x); }
}
public static final class Tan extends Util.R2R {
    protected double f(double x) { return Math.tan(x); }
}
public static final class ASin extends Util.R2R {
    protected double f(double x) { return Math.asin(x); }
}
public static final class ACos extends Util.R2R {
    protected double f(double x) { return Math.acos(x); }
}
public static final class ATan extends Util.R2R {
    protected double f(double x) { return Math.atan(x); }
}
public static final class ATan2 extends Util.RR2R {
    protected double f(double y, double x) { return Math.atan2(y,x); }
}
public static final class Log extends Util.R2R {
    protected double f(double x) { return Math.log(x); }
}
public static final class Exp extends Util.R2R {
    protected double f(double x) { return Math.exp(x); }
}
public static final class Ceiling extends Util.R2R {
    protected double f(double x) { return Math.ceil(x); }
}
public static final class Floor extends Util.R2R {
    protected double f(double x) { return Math.floor(x); }
}
public static final class ICeiling extends Util.R2L {
    protected long f(double x) { return (long)Math.ceil(x); }
}
public static final class IFloor extends Util.R2L {
    protected long f(double x) { return (long)Math.floor(x); }
}
public static final class Truncate extends Util.R2L {
    protected long f(double x) { return (long)x; }
}
public static final class Abs extends Util.R2R {
    protected double f(double x) { return Math.abs(x); }
}
public static final class NextUp extends Util.R2R {
    protected double f(double x) { return DirectedRounding.nextUp(x); }
}
public static final class NextDown extends Util.R2R {
    protected double f(double x) { return DirectedRounding.nextDown(x); }
}
public static final class RawBits extends Util.R2L {
    protected long f(double x) { return Double.doubleToLongBits(x); }
}
public static final class FromRawBits extends Util.L2R {
    protected double f(long x) { return Double.longBitsToDouble(x); }
}

public static final class Random extends Util.R2R {
    protected double f(double scale) { return scale*Math.random(); }
}

}
