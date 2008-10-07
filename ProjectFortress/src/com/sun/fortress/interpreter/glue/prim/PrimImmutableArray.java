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
import static com.sun.fortress.exceptions.InterpreterBug.bug;

import java.util.List;

import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.interpreter.evaluator.types.FTypeObject;
import com.sun.fortress.interpreter.evaluator.values.FBool;
import com.sun.fortress.interpreter.evaluator.values.FObject;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.interpreter.evaluator.values.NativeConstructor;
import com.sun.fortress.interpreter.glue.NativeMeth1;
import com.sun.fortress.interpreter.glue.NativeMeth2;
import com.sun.fortress.nodes.GenericWithParams;

public class PrimImmutableArray extends NativeConstructor {
    int s0;

    public PrimImmutableArray(Environment env,
                              FTypeObject selfType,
                              GenericWithParams def) {
        super(env, selfType, def);
        // TODO Auto-generated constructor stub
    }

    protected FNativeObject makeNativeObject(List<FValue> args, NativeConstructor con) {
        return new PrimImmutableArrayObject(con,s0);
    }

    protected void oneTimeInit(Environment self_env) {
        s0 = self_env.getValue("s0").getInt();
    }

    public static final class PrimImmutableArrayObject extends FNativeObject {
        protected final FValue[] contents;
        protected final NativeConstructor con;

        public PrimImmutableArrayObject(NativeConstructor con, int capacity) {
            super(con);
            this.con = con;
            this.contents = new FValue[capacity];
        }

        public NativeConstructor getConstructor() {
            return con;
        }

        public boolean seqv(FValue other) {
            return this==other;
        }

        public FValue get(int i) {
            try {
                return contents[i];
            } catch (ArrayIndexOutOfBoundsException e) {
                return bug(errorMsg("Array index ",i," out of bounds, length=",contents.length),e);
            }
        }

        public boolean init(int i, FValue v) {
            try {
                FValue old = contents[i];
                contents[i] = v;
                return (old==null);
            } catch (ArrayIndexOutOfBoundsException e) {
                bug(errorMsg("Array index ",i," out of bounds, length=",contents.length),e);
                return false;
            }
        }
    }

    private static abstract class vi2O extends NativeMeth1 {
        protected abstract FValue f(PrimImmutableArrayObject v, int i);
        public FValue act(FObject self, FValue ii) {
            PrimImmutableArrayObject v = (PrimImmutableArrayObject) self;
            int i = ii.getInt();
            return f(v,i);
        }
    }

    private static abstract class vio2B extends NativeMeth2 {
        protected abstract boolean f(PrimImmutableArrayObject v, int i, FValue x);
        public FValue act(FObject self, FValue ii, FValue x) {
            PrimImmutableArrayObject v = (PrimImmutableArrayObject) self;
            int i = ii.getInt();
            return FBool.make(f(v,i,x));
        }
    }

    public static final class get extends vi2O {
        protected FValue f(PrimImmutableArrayObject v, int i) {
            FValue r = v.get(i);
            if (r==null) {
                return error(errorMsg("Access to uninitialized element ",
                                      i, " of array ", v));
            }
            return r;
        }
    }

    public static final class init0 extends vio2B {
        protected boolean f(PrimImmutableArrayObject v, int i, FValue x) {
            return v.init(i,x);
        }
    }

}
