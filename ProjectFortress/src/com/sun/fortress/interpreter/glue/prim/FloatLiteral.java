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
import com.sun.fortress.nodes.ObjectConstructor;

import java.util.List;

public class FloatLiteral extends NativeConstructor {

    public FloatLiteral(Environment env, FTypeObject selfType, ObjectConstructor def) {
        super(env, selfType, def);
    }

    protected FNativeObject makeNativeObject(List<FValue> args, NativeConstructor con) {
        FFloatLiteral.setConstructor(this);
        return FFloatLiteral.ZERO;
    }

    static private abstract class Rlit2S extends NativeMeth0 {
        protected abstract java.lang.String f(java.lang.String x);

        public final FValue applyMethod(FObject x) {
            return FString.make(f(x.getString()));
        }
    }

    static private abstract class Rlit2R extends NativeMeth0 {
        protected abstract double f(double x);

        public final FFloat applyMethod(FObject x) {
            return FFloat.make(f(x.getFloat()));
        }
    }

    public static final class ToString extends Rlit2S {
        protected java.lang.String f(java.lang.String x) {
            return x;
        }
    }

    public static final class AsFloat extends Rlit2R {
        protected double f(double x) {
            return x;
        }
    }

    @Override
    protected void unregister() {
        FFloatLiteral.resetConstructor();

    }

}
