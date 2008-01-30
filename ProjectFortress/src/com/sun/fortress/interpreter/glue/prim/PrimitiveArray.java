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

import java.util.List;

import com.sun.fortress.interpreter.env.BetterEnv;
// import com.sun.fortress.interpreter.evaluator.transactions.AtomicArray;
import com.sun.fortress.interpreter.evaluator.transactions.AtomicFTypeArray;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.evaluator.types.FTypeObject;
import com.sun.fortress.interpreter.evaluator.values.Constructor;
import com.sun.fortress.interpreter.evaluator.values.FBool;
import com.sun.fortress.interpreter.evaluator.values.FInt;
import com.sun.fortress.interpreter.evaluator.values.FObject;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.interpreter.evaluator.values.FVoid;
import com.sun.fortress.interpreter.glue.NativeMeth1;
import com.sun.fortress.interpreter.glue.NativeMeth2;
import com.sun.fortress.nodes.AbsDeclOrDecl;
import com.sun.fortress.nodes.SimpleName;
import com.sun.fortress.nodes.GenericWithParams;

import static com.sun.fortress.interpreter.evaluator.ProgramError.errorMsg;
import static com.sun.fortress.interpreter.evaluator.ProgramError.error;

public class PrimitiveArray extends Constructor {

    public PrimitiveArray(BetterEnv env, FTypeObject selfType, GenericWithParams def) {
        super(env, selfType, def);
        // TODO Auto-generated constructor stub
    }

    protected FObject makeAnObject(BetterEnv lex_env, BetterEnv self_env) {
        return new AtomicFTypeArray(selfType, self_env);
    }

    public static final class get extends NativeMeth1 {
        public FValue act(FObject self, FValue ii) {
            // System.out.println(self+".get("+ii+")");
            AtomicFTypeArray v = (AtomicFTypeArray) self;
            // AtomicArray<FValue> v = ((Vec)self).a;
            int i = ii.getInt();
            FValue r = v.get(i);
            if (r==null) {
                error(errorMsg("Access to uninitialized element ",
                               i, " of array ", v));
            }
            return r;
        }
    }

    public static final class put extends NativeMeth2 {
        public FValue act(FObject self, FValue ii, FValue x) {
            // System.out.println(self+".put("+ii+","+x+")");
            AtomicFTypeArray v = (AtomicFTypeArray) self;
            // AtomicArray<FValue> v = ((Vec)self).a;
            int i = ii.getInt();
            v.set(i,x);
            return FVoid.V;
        }
    }

    public static final class init0 extends NativeMeth2 {
        public FValue act(FObject self, FValue ii, FValue x) {
            // System.out.println(self+".init0("+ii+","+x+")");
            AtomicFTypeArray v = (AtomicFTypeArray) self;
            // AtomicArray<FValue> v = ((Vec)self).a;
            int i = ii.getInt();
            return FBool.make(v.init(i,x));
        }
    }

}
