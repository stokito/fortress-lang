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

package com.sun.fortress.interpreter.glue.test;

import java.util.List;

import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.interpreter.evaluator.types.FTypeObject;
import com.sun.fortress.interpreter.evaluator.values.FObject;
import com.sun.fortress.interpreter.evaluator.values.FString;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.interpreter.evaluator.values.NativeConstructor;
import com.sun.fortress.interpreter.glue.NativeMeth0;
import com.sun.fortress.nodes.GenericWithParams;

public class TNFoo extends NativeConstructor {
    public TNFoo(Environment env, FTypeObject selfType, GenericWithParams def) {
        super(env, selfType, def);
        // TODO Auto-generated constructor stub
    }

    protected FNativeObject makeNativeObject(List<FValue> args, NativeConstructor con) {
        return new Obj(args.get(0).getString(),getSelfEnv().getValue("n").getInt(),con);
    }

    private static final class Obj extends FNativeObject {
        final FValue theString;

        final NativeConstructor con;

        private Obj(String s, int theCount, NativeConstructor con) {
            super(con);
            this.con = con;

            while (theCount > 0) {
                s = s + " " + s;
                theCount--;
            }

            theString = FString.make(s);

        }

        public NativeConstructor getConstructor() {
            return con;
        }

        public boolean seqv(FValue v) {
            if (v==this) return true;
            if (!(v instanceof Obj)) return false;
            Obj o = (Obj) v;
            if (con != o.getConstructor()) return false;
            return theString.equals(o.theString);
        }
    }

    public static final class bar extends NativeMeth0 {
        protected FValue act(FObject selfValue) {
            Obj tnf = (Obj) selfValue;
            return tnf.theString;
        }

    }

}
