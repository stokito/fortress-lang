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
 * Functions from RR32.
 */
public class RR32 extends NativeConstructor {

    public RR32(Environment env, FTypeObject selfType, ObjectConstructor def) {
        super(env, selfType, def);
    }

    protected FNativeObject makeNativeObject(List<FValue> args, NativeConstructor con) {
        FRR32.setConstructor(this);
        return FRR32.ZERO;
    }

    static private abstract class F2B extends NativeMeth0 {
        protected abstract boolean f(float x);

        public final FValue applyMethod(FObject x) {
            return FBool.make(f(x.getRR32()));
        }
    }

    static private abstract class F2F extends NativeMeth0 {
        protected abstract float f(float x);

        public final FValue applyMethod(FObject x) {
            return FRR32.make(f(x.getRR32()));
        }
    }

    static private abstract class F2R extends NativeMeth0 {
        protected abstract double f(float x);

        public final FValue applyMethod(FObject x) {
            return FFloat.make(f(x.getRR32()));
        }
    }

    static private abstract class F2L extends NativeMeth0 {
        protected abstract long f(float x);

        public final FValue applyMethod(FObject x) {
            return FLong.make(f(x.getRR32()));
        }
    }

    static private abstract class F2I extends NativeMeth0 {
        protected abstract int f(float x);

        public final FValue applyMethod(FObject x) {
            return FInt.make(f(x.getRR32()));
        }
    }

    static private abstract class F2S extends NativeMeth0 {
        protected abstract java.lang.String f(float x);

        public final FValue applyMethod(FObject x) {
            return FString.make(f(x.getRR32()));
        }
    }

    static private abstract class FF2B extends NativeMeth1 {
        protected abstract boolean f(float x, float y);

        public final FValue applyMethod(FObject x, FValue y) {
            return FBool.make(f(x.getRR32(), y.getRR32()));
        }
    }

    static private abstract class FF2F extends NativeMeth1 {
        protected abstract float f(float x, float y);

        public final FValue applyMethod(FObject x, FValue y) {
            return FRR32.make(f(x.getRR32(), y.getRR32()));
        }
    }

    public static final class Negate extends F2F {
        protected float f(float x) {
            return -x;
        }
    }

    public static final class Add extends FF2F {
        protected float f(float x, float y) {
            return x + y;
        }
    }

    public static final class Sub extends FF2F {
        protected float f(float x, float y) {
            return x - y;
        }
    }

    public static final class Mul extends FF2F {
        protected float f(float x, float y) {
            return x * y;
        }
    }

    public static final class Div extends FF2F {
        protected float f(float x, float y) {
            return x / y;
        }
    }

    public static final class Sqrt extends F2F {
        protected float f(float x) {
            return (float) Math.sqrt(x);
        }
    }

    public static final class AddUp extends FF2F {
        protected float f(float x, float y) {
            return DirectedRounding.addUp(x, y);
        }
    }

    public static final class SubUp extends FF2F {
        protected float f(float x, float y) {
            return DirectedRounding.subtractUp(x, y);
        }
    }

    public static final class MulUp extends FF2F {
        protected float f(float x, float y) {
            return DirectedRounding.multiplyUp(x, y);
        }
    }

    public static final class DivUp extends FF2F {
        protected float f(float x, float y) {
            return DirectedRounding.divideUp(x, y);
        }
    }

    public static final class SqrtUp extends F2F {
        protected float f(float x) {
            return DirectedRounding.sqrtUp(x);
        }
    }

    public static final class AddDown extends FF2F {
        protected float f(float x, float y) {
            return DirectedRounding.addDown(x, y);
        }
    }

    public static final class SubDown extends FF2F {
        protected float f(float x, float y) {
            return DirectedRounding.subtractDown(x, y);
        }
    }

    public static final class MulDown extends FF2F {
        protected float f(float x, float y) {
            return DirectedRounding.multiplyDown(x, y);
        }
    }

    public static final class DivDown extends FF2F {
        protected float f(float x, float y) {
            return DirectedRounding.divideDown(x, y);
        }
    }

    public static final class SqrtDown extends F2F {
        protected float f(float x) {
            return DirectedRounding.sqrtDown(x);
        }
    }

    public static final class AddUpNoNaN extends FF2F {
        protected float f(float x, float y) {
            return DirectedRounding.addUpNoNaN(x, y);
        }
    }

    public static final class SubUpNoNaN extends FF2F {
        protected float f(float x, float y) {
            return DirectedRounding.subtractUpNoNaN(x, y);
        }
    }

    public static final class MulUpNoNaN extends FF2F {
        protected float f(float x, float y) {
            return DirectedRounding.multiplyUpNoNaN(x, y);
        }
    }

    public static final class DivUpNoNaN extends FF2F {
        protected float f(float x, float y) {
            return DirectedRounding.divideUpNoNaN(x, y);
        }
    }

    public static final class AddDownNoNaN extends FF2F {
        protected float f(float x, float y) {
            return DirectedRounding.addDownNoNaN(x, y);
        }
    }

    public static final class SubDownNoNaN extends FF2F {
        protected float f(float x, float y) {
            return DirectedRounding.subtractDownNoNaN(x, y);
        }
    }

    public static final class MulDownNoNaN extends FF2F {
        protected float f(float x, float y) {
            return DirectedRounding.multiplyDownNoNaN(x, y);
        }
    }

    public static final class DivDownNoNaN extends FF2F {
        protected float f(float x, float y) {
            return DirectedRounding.divideDownNoNaN(x, y);
        }
    }

    public static final class Eq extends FF2B {
        protected boolean f(float x, float y) {
            return x == y;
        }
    }

    public static final class NEq extends FF2B {
        protected boolean f(float x, float y) {
            return x != y;
        }
    }

    public static final class Less extends FF2B {
        protected boolean f(float x, float y) {
            return x < y;
        }
    }

    public static final class LessEq extends FF2B {
        protected boolean f(float x, float y) {
            return x <= y;
        }
    }

    public static final class Greater extends FF2B {
        protected boolean f(float x, float y) {
            return x > y;
        }
    }

    public static final class GreaterEq extends FF2B {
        protected boolean f(float x, float y) {
            return x >= y;
        }
    }

    public static final class Min extends FF2F {
        protected float f(float x, float y) {
            return Math.min(x, y);
        }
    }

    public static final class Max extends FF2F {
        protected float f(float x, float y) {
            return Math.max(x, y);
        }
    }

    public static final class Pow extends FF2F {
        protected float f(float x, float y) {
            return (float) Math.pow(x, y);
        }
    }

    public static final class Ceiling extends F2F {
        protected float f(float x) {
            return (float) Math.ceil(x);
        }
    }

    public static final class Floor extends F2F {
        protected float f(float x) {
            return (float) Math.floor(x);
        }
    }

    public static final class ICeiling extends F2L {
        protected long f(float x) {
            return (long) Math.ceil(x);
        }
    }

    public static final class IFloor extends F2L {
        protected long f(float x) {
            return (long) Math.floor(x);
        }
    }

    public static final class Round extends F2L {
        protected long f(float x) {
            return (long) Math.round((double) x);
        }
    }

    public static final class Truncate extends F2L {
        protected long f(float x) {
            return (long) x;
        }
    }

    public static final class Abs extends F2F {
        protected float f(float x) {
            return Math.abs(x);
        }
    }

    public static final class NextUp extends F2F {
        protected float f(float x) {
            return DirectedRounding.nextUp(x);
        }
    }

    public static final class NextDown extends F2F {
        protected float f(float x) {
            return DirectedRounding.nextDown(x);
        }
    }

    public static final class RawBits extends F2I {
        protected int f(float x) {
            return java.lang.Float.floatToRawIntBits(x);
        }
    }

    public static final class FromRawBits extends Util.I2F {
        protected float f(int x) {
            return java.lang.Float.intBitsToFloat(x);
        }
    }

    public static final class Random extends Util.F2F {
        protected float f(float scale) {
            return (float) (scale * Math.random());
        }
    }

    public static final class isInfinite extends F2B {
        protected boolean f(float x) {
            return java.lang.Float.isInfinite(x);
        }
    }

    public static final class isNaN extends F2B {
        protected boolean f(float x) {
            return java.lang.Float.isNaN(x);
        }
    }

    public static final class ToString extends F2S {
        protected java.lang.String f(float x) {
            return java.lang.Float.toString(x);
        }
    }

    public static final class AsFloat extends F2R {
        protected double f(float x) {
            return (double) x;
        }
    }

    @Override
    protected void unregister() {
        FRR32.resetConstructor();

    }

}
