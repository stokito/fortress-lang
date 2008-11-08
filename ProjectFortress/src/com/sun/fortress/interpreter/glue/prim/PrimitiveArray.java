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
import static com.sun.fortress.exceptions.ProgramError.errorMsg;

import java.util.List;

import com.sun.fortress.interpreter.evaluator.transactions.AtomicArray;
import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.interpreter.evaluator.types.FTypeObject;
import com.sun.fortress.interpreter.evaluator.values.FBool;
import com.sun.fortress.interpreter.evaluator.values.FObject;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.interpreter.evaluator.values.FVoid;
import com.sun.fortress.interpreter.evaluator.values.NativeConstructor;
import com.sun.fortress.interpreter.glue.NativeMeth1;
import com.sun.fortress.interpreter.glue.NativeMeth2;
import com.sun.fortress.nodes.GenericWithParams;

public class PrimitiveArray extends NativeConstructor {
    int s0;

    public PrimitiveArray(Environment env, FTypeObject selfType, GenericWithParams def) {
        super(env, selfType, def);
        // TODO Auto-generated constructor stub
    }

    protected FNativeObject makeNativeObject(List<FValue> args, NativeConstructor con) {
        return new AtomicArray(con,s0);
    }

    @Override
    protected void oneTimeInit(Environment self_env) {
        s0 = self_env.getLeafValue("s0").getInt();
    }

    private static abstract class vi2O extends NativeMeth1 {
        protected abstract FValue f(AtomicArray v, int i);
        public FValue act(FObject self, FValue ii) {
			AtomicArray v = (AtomicArray) self;
            int i = ii.getInt();
            return f(v,i);
        }
    }

    private static abstract class vio2V extends NativeMeth2 {
        protected abstract void f(AtomicArray v, int i, FValue x);
        public FValue act(FObject self, FValue ii, FValue x) {
			AtomicArray v = (AtomicArray) self;
            int i = ii.getInt();
            f(v,i,x);
            return FVoid.V;
        }
    }

    private static abstract class vio2B extends NativeMeth2 {
        protected abstract boolean f(AtomicArray v, int i, FValue x);
        public FValue act(FObject self, FValue ii, FValue x) {
			AtomicArray v = (AtomicArray) self;
            int i = ii.getInt();
            return FBool.make(f(v,i,x));
        }
    }

    public static final class get extends vi2O {
        protected FValue f(AtomicArray v, int i) {
            FValue r = (FValue) v.get(i);
            if (r==null) {
                error(errorMsg("Access to uninitialized element ",
                               i, " of array ", v));
            }
            return r;
        }
    }

    public static final class put extends vio2V {
        protected void f(AtomicArray v, int i, FValue x) {
            v.set(i,x);
        }
    }

    public static final class init0 extends vio2B {
        protected boolean f(AtomicArray v, int i, FValue x) {
            return v.init(i,x);
        }
    }

    @Override
    protected void unregister() {
        // Apparently not necessary for this class; on the other hand,
        // perhaps it's wrong to cache the "con" with each and every
        // allocated object.  See other PrimImmutableArray types, also.
    }

}
