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
import com.sun.fortress.numerics.DirectedRounding;

import java.util.List;

/**
 * Functions from RR64.
 */
public class Float extends NativeConstructor {

    public Float(Environment env, FTypeObject selfType, ObjectConstructor def) {
        super(env, selfType, def);
    }

    protected FNativeObject makeNativeObject(List<FValue> args, NativeConstructor con) {
        FFloat.setConstructor(this);
        return FFloat.ZERO;
    }

    static private abstract class R2F extends NativeMeth0 {
        protected abstract float f(double x);

        public final FValue applyMethod(FObject x) {
            return FRR32.make(f(x.getFloat()));
        }
    }

    static private abstract class R2B extends NativeMeth0 {
        protected abstract boolean f(double x);

        public final FValue applyMethod(FObject x) {
            return FBool.make(f(x.getFloat()));
        }
    }

    static private abstract class R2R extends NativeMeth0 {
        protected abstract double f(double x);

        public final FValue applyMethod(FObject x) {
            return FFloat.make(f(x.getFloat()));
        }
    }

    static private abstract class R2L extends NativeMeth0 {
        protected abstract long f(double x);

        public final FValue applyMethod(FObject x) {
            return FLong.make(f(x.getFloat()));
        }
    }

    static private abstract class R2S extends NativeMeth0 {
        protected abstract java.lang.String f(double x);

        public final FValue applyMethod(FObject x) {
            return FString.make(f(x.getFloat()));
        }
    }

    static private abstract class RR2B extends NativeMeth1 {
        protected abstract boolean f(double x, double y);

        public final FValue applyMethod(FObject x, FValue y) {
            return FBool.make(f(x.getFloat(), y.getFloat()));
        }
    }

    static private abstract class RR2R extends NativeMeth1 {
        protected abstract double f(double x, double y);

        public final FValue applyMethod(FObject x, FValue y) {
            return FFloat.make(f(x.getFloat(), y.getFloat()));
        }
    }

    public static final class Narrow extends R2F {
        protected float f(double x) {
            return (float) x;
        }
    }

    public static final class Negate extends R2R {
        protected double f(double x) {
            return -x;
        }
    }

    public static final class Add extends RR2R {
        protected double f(double x, double y) {
            return x + y;
        }
    }

    public static final class Sub extends RR2R {
        protected double f(double x, double y) {
            return x - y;
        }
    }

    public static final class Mul extends RR2R {
        protected double f(double x, double y) {
            return x * y;
        }
    }

    public static final class Div extends RR2R {
        protected double f(double x, double y) {
            return x / y;
        }
    }

    public static final class Sqrt extends R2R {
        protected double f(double x) {
            return Math.sqrt(x);
        }
    }

    public static final class AddUp extends RR2R {
        protected double f(double x, double y) {
            return DirectedRounding.addUp(x, y);
        }
    }

    public static final class SubUp extends RR2R {
        protected double f(double x, double y) {
            return DirectedRounding.subtractUp(x, y);
        }
    }

    public static final class MulUp extends RR2R {
        protected double f(double x, double y) {
            return DirectedRounding.multiplyUp(x, y);
        }
    }

    public static final class DivUp extends RR2R {
        protected double f(double x, double y) {
            return DirectedRounding.divideUp(x, y);
        }
    }

    public static final class SqrtUp extends R2R {
        protected double f(double x) {
            return DirectedRounding.sqrtUp(x);
        }
    }

    public static final class AddDown extends RR2R {
        protected double f(double x, double y) {
            return DirectedRounding.addDown(x, y);
        }
    }

    public static final class SubDown extends RR2R {
        protected double f(double x, double y) {
            return DirectedRounding.subtractDown(x, y);
        }
    }

    public static final class MulDown extends RR2R {
        protected double f(double x, double y) {
            return DirectedRounding.multiplyDown(x, y);
        }
    }

    public static final class DivDown extends RR2R {
        protected double f(double x, double y) {
            return DirectedRounding.divideDown(x, y);
        }
    }

    public static final class SqrtDown extends R2R {
        protected double f(double x) {
            return DirectedRounding.sqrtDown(x);
        }
    }

    public static final class AddUpNoNaN extends RR2R {
        protected double f(double x, double y) {
            return DirectedRounding.addUpNoNaN(x, y);
        }
    }

    public static final class SubUpNoNaN extends RR2R {
        protected double f(double x, double y) {
            return DirectedRounding.subtractUpNoNaN(x, y);
        }
    }

    public static final class MulUpNoNaN extends RR2R {
        protected double f(double x, double y) {
            return DirectedRounding.multiplyUpNoNaN(x, y);
        }
    }

    public static final class DivUpNoNaN extends RR2R {
        protected double f(double x, double y) {
            return DirectedRounding.divideUpNoNaN(x, y);
        }
    }

    public static final class AddDownNoNaN extends RR2R {
        protected double f(double x, double y) {
            return DirectedRounding.addDownNoNaN(x, y);
        }
    }

    public static final class SubDownNoNaN extends RR2R {
        protected double f(double x, double y) {
            return DirectedRounding.subtractDownNoNaN(x, y);
        }
    }

    public static final class MulDownNoNaN extends RR2R {
        protected double f(double x, double y) {
            return DirectedRounding.multiplyDownNoNaN(x, y);
        }
    }

    public static final class DivDownNoNaN extends RR2R {
        protected double f(double x, double y) {
            return DirectedRounding.divideDownNoNaN(x, y);
        }
    }

    public static final class Eq extends RR2B {
        protected boolean f(double x, double y) {
            return x == y;
        }
    }

    public static final class NEq extends RR2B {
        protected boolean f(double x, double y) {
            return x != y;
        }
    }

    public static final class Less extends RR2B {
        protected boolean f(double x, double y) {
            return x < y;
        }
    }

    public static final class LessEq extends RR2B {
        protected boolean f(double x, double y) {
            return x <= y;
        }
    }

    public static final class Greater extends RR2B {
        protected boolean f(double x, double y) {
            return x > y;
        }
    }

    public static final class GreaterEq extends RR2B {
        protected boolean f(double x, double y) {
            return x >= y;
        }
    }

    public static final class Min extends RR2R {
        protected double f(double x, double y) {
            return Math.min(x, y);
        }
    }

    public static final class Max extends RR2R {
        protected double f(double x, double y) {
            return Math.max(x, y);
        }
    }

    public static final class Pow extends RR2R {
        protected double f(double x, double y) {
            return Math.pow(x, y);
        }
    }

    public static final class Sin extends R2R {
        protected double f(double x) {
            return Math.sin(x);
        }
    }

    public static final class Cos extends R2R {
        protected double f(double x) {
            return Math.cos(x);
        }
    }

    public static final class Tan extends R2R {
        protected double f(double x) {
            return Math.tan(x);
        }
    }

    public static final class ASin extends R2R {
        protected double f(double x) {
            return Math.asin(x);
        }
    }

    public static final class ACos extends R2R {
        protected double f(double x) {
            return Math.acos(x);
        }
    }

    public static final class ATan extends R2R {
        protected double f(double x) {
            return Math.atan(x);
        }
    }

    public static final class ATan2 extends RR2R {
        protected double f(double y, double x) {
            return Math.atan2(y, x);
        }
    }

    public static final class Log extends R2R {
        protected double f(double x) {
            return Math.log(x);
        }
    }

    public static final class Exp extends R2R {
        protected double f(double x) {
            return Math.exp(x);
        }
    }

    public static final class Ceiling extends R2R {
        protected double f(double x) {
            return Math.ceil(x);
        }
    }

    public static final class Floor extends R2R {
        protected double f(double x) {
            return Math.floor(x);
        }
    }

    public static final class ICeiling extends R2L {
        protected long f(double x) {
            return (long) Math.ceil(x);
        }
    }

    public static final class IFloor extends R2L {
        protected long f(double x) {
            return (long) Math.floor(x);
        }
    }

    public static final class Truncate extends R2L {
        protected long f(double x) {
            return (long) x;
        }
    }

    public static final class Round extends R2L {
        protected long f(double x) {
            return Math.round(x);
        }
    }

    public static final class Abs extends R2R {
        protected double f(double x) {
            return Math.abs(x);
        }
    }

    public static final class NextUp extends R2R {
        protected double f(double x) {
            return DirectedRounding.nextUp(x);
        }
    }

    public static final class NextDown extends R2R {
        protected double f(double x) {
            return DirectedRounding.nextDown(x);
        }
    }

    public static final class RawBits extends R2L {
        protected long f(double x) {
            return Double.doubleToRawLongBits(x);
        }
    }

    public static final class FromRawBits extends Util.L2R {
        protected double f(long x) {
            return Double.longBitsToDouble(x);
        }
    }

    public static final class Random extends Util.R2R {
        protected double f(double scale) {
            return scale * Math.random();
        }
    }

    public static final class isInfinite extends R2B {
        protected boolean f(double x) {
            return Double.isInfinite(x);
        }
    }

    public static final class isNaN extends R2B {
        protected boolean f(double x) {
            return Double.isNaN(x);
        }
    }

    public static final class ToString extends R2S {
        protected java.lang.String f(double x) {
            return Double.toString(x);
        }
    }

    @Override
    protected void unregister() {
        FFloat.resetConstructor();

    }

}
