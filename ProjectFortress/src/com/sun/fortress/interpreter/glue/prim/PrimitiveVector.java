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

import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.interpreter.evaluator.transactions.AtomicArray;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.evaluator.types.FTypeObject;
import com.sun.fortress.interpreter.evaluator.values.Constructor;
import com.sun.fortress.interpreter.evaluator.values.FInt;
import com.sun.fortress.interpreter.evaluator.values.FObject;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.interpreter.evaluator.values.FVoid;
import com.sun.fortress.interpreter.glue.NativeMeth1;
import com.sun.fortress.interpreter.glue.NativeMeth2;
import com.sun.fortress.nodes.AbsDeclOrDecl;
import com.sun.fortress.nodes.FnName;
import com.sun.fortress.nodes.GenericWithParams;

import static com.sun.fortress.interpreter.evaluator.ProgramError.errorMsg;
import static com.sun.fortress.interpreter.evaluator.ProgramError.error;

public class PrimitiveVector extends Constructor {

    public PrimitiveVector(BetterEnv env, FTypeObject selfType, GenericWithParams def) {
        super(env, selfType, def);
        // TODO Auto-generated constructor stub
    }

    protected FObject makeAnObject(BetterEnv lex_env, BetterEnv self_env) {
        return new Vec(selfType, lex_env, self_env);
    }

    private static final class Vec extends FObject {
        AtomicArray<FValue> a;

        public Vec(FType selfType, BetterEnv lexical_env, BetterEnv self_dot_env) {
            super(selfType, BetterEnv.empty(), self_dot_env);
            long n = self_dot_env.getValue("s0").getLong();
            this.a = new AtomicArray<FValue>(FValue.class, (int) n);
        }
    }

    public static final class get extends NativeMeth1 {
        public FValue act(FObject self, FValue ii) {
            // System.out.println(self+".get("+ii+")");
            Vec v = (Vec) self;
            int i = ii.getInt();
            FValue r = v.a.get(i);
            if (r==null) {
                error(errorMsg("Access to uninitialized element ",
                               i, " of array ", v));
            }
            return r;
        }
    }

    public static final class put extends NativeMeth2 {
        public FValue act(FObject self, FValue x, FValue ii) {
            // System.out.println(self+".put("+x+","+ii+")");
            Vec v = (Vec) self;
            int i = ii.getInt();
            v.a.set(i,x);
            return FVoid.V;
        }
    }

}
