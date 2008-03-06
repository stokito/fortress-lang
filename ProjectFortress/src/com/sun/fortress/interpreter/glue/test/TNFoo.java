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

import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.evaluator.types.FTypeObject;
import com.sun.fortress.interpreter.evaluator.values.Constructor;
import com.sun.fortress.interpreter.evaluator.values.FObject;
import com.sun.fortress.interpreter.evaluator.values.FOrdinaryObject;
import com.sun.fortress.interpreter.evaluator.values.FString;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.interpreter.glue.NativeMeth0;
import com.sun.fortress.nodes.AbsDeclOrDecl;
import com.sun.fortress.nodes.SimpleName;
import com.sun.fortress.nodes.GenericWithParams;

public class TNFoo extends Constructor {

    public TNFoo(BetterEnv env, FTypeObject selfType, GenericWithParams def) {
        super(env, selfType, def);
        // TODO Auto-generated constructor stub
    }

    protected FObject makeAnObject(BetterEnv lex_env, BetterEnv self_env) {
        return new Obj(selfType, lex_env, self_env);
    }

    private static final class Obj extends FOrdinaryObject {
        public Obj(FType selfType, BetterEnv lexical_env, BetterEnv self_dot_env) {
            // might like to discard envs to perhaps save space,
            // but need self_dot_env for method invocation lookup
            super(selfType, lexical_env, self_dot_env);
            int theCount = self_dot_env.getValue("n").getInt();
            String s = self_dot_env.getValue("s").getString();

            while (theCount > 0) {
                s = s + " " + s;
                theCount--;
            }

            theString = FString.make(s);

        }

        FValue theString;

    }

    public static final class bar extends NativeMeth0 {
        protected FValue act(FObject selfValue) {
            Obj tnf = (Obj) selfValue;
            return tnf.theString;
        }

    }

}
