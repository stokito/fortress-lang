/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.glue.test;

import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.interpreter.evaluator.types.FTypeObject;
import com.sun.fortress.interpreter.evaluator.values.FNativeObject;
import com.sun.fortress.interpreter.evaluator.values.FObject;
import com.sun.fortress.interpreter.evaluator.values.FString;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.interpreter.evaluator.values.NativeConstructor;
import com.sun.fortress.interpreter.glue.NativeMeth0;
import com.sun.fortress.nodes.ObjectConstructor;

import java.util.List;

public class TNFoo extends NativeConstructor {
    public TNFoo(Environment env, FTypeObject selfType, ObjectConstructor def) {
        super(env, selfType, def);
        // TODO Auto-generated constructor stub
    }

    protected FNativeObject makeNativeObject(List<FValue> args, NativeConstructor con) {
        return new Obj(args.get(0).getString(), getSelfEnv().getLeafValue("n").getInt(), con);
    }

    private static final class Obj extends FNativeObject {
        final FValue theString;

        final NativeConstructor con;

        private Obj(String s, int theCount, NativeConstructor con) {
            super(con);
            this.con = con;

            StringBuilder buf = new StringBuilder();
            buf.append(s);
            while (theCount > 0) {
                buf.append(" " + s);
                s = buf.toString();
                theCount--;
            }

            theString = FString.make(s);

        }

        public NativeConstructor getConstructor() {
            return con;
        }

        public boolean seqv(FValue v) {
            if (v == this) return true;
            if (!(v instanceof Obj)) return false;
            Obj o = (Obj) v;
            if (con != o.getConstructor()) return false;
            return theString.equals(o.theString);
        }
    }

    public static final class bar extends NativeMeth0 {
        public final FValue applyMethod(FObject selfValue) {
            Obj tnf = (Obj) selfValue;
            return tnf.theString;
        }

    }

    @Override
    protected void unregister() {
        // not needed for this class
    }

}
