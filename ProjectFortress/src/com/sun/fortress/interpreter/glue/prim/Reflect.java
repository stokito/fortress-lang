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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.evaluator.types.FTypeObject;
import com.sun.fortress.interpreter.evaluator.values.Constructor;
import com.sun.fortress.interpreter.evaluator.values.GenericConstructor;
import com.sun.fortress.interpreter.evaluator.values.Simple_fcn;
import com.sun.fortress.interpreter.evaluator.values.FObject;
import com.sun.fortress.interpreter.evaluator.values.FOrdinaryObject;
import com.sun.fortress.interpreter.evaluator.values.FString;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.interpreter.evaluator.values.FVoid;
import com.sun.fortress.interpreter.glue.NativeMeth0;
import com.sun.fortress.interpreter.glue.NativeApp;
import com.sun.fortress.nodes.AbsDeclOrDecl;
import com.sun.fortress.nodes.SimpleName;
import com.sun.fortress.nodes.GenericWithParams;
import com.sun.fortress.useful.HasAt;
import com.sun.fortress.useful.Useful;

import static com.sun.fortress.interpreter.evaluator.ProgramError.errorMsg;
import static com.sun.fortress.interpreter.evaluator.ProgramError.error;

public class Reflect extends Constructor {
    static GenericConstructor gcon = null;

    volatile ReflectedType it;

    public Reflect(BetterEnv env, FTypeObject selfType, GenericWithParams def) {
        super(env, selfType, def);
        gcon = (GenericConstructor)env.getValue("Reflect");
    }

    protected FObject makeAnObject(BetterEnv lex_env, BetterEnv self_env) {
        FType t = self_env.getType("T");
        if (it==null) {
            synchronized(this) {
                if (it==null) {
                    it = new ReflectedType(selfType, lex_env, self_env);
                }
            }
        }
        return it;
    }

    private static final class ReflectedType extends FOrdinaryObject {
        public ReflectedType(FType selfType, BetterEnv lex_env, BetterEnv self_dot_env) {
            super(selfType, lex_env, self_dot_env);
        }

        FType getTy() {
            return getSelfEnv().getType("T");
        }
    }

    public static final ReflectedType makeReflectedType(FType t) {
        if (gcon==null) {
            return error("Cannot make Reflect[\\"+t+
                         "\\]; constructor not invoked from Fortress yet.");
        }
        Simple_fcn con = gcon.typeApply(gcon,Useful.list(t));
        return (ReflectedType)con.apply(Collections.<FValue>emptyList(), gcon,
                                        BetterEnv.blessedEmpty());
    }

    public static final class Join extends NativeApp {
        public final int getArity() { return 2; }
        public FValue applyToArgs(List<FValue> reflecteds) {
            List<FType> tys = new ArrayList<FType>(reflecteds.size());
            for (FValue v: reflecteds) {
                tys.add(((ReflectedType)v).getTy());
            }
            Set<FType> join = FType.joinTypes(tys);
            /* For now, just choose a type at random. */
            for (FType ty : join) {
                return makeReflectedType(ty);
            }
            /* Should be unreachable. */
            return null;
        }
    }

    public static final class ToString extends NativeMeth0 {
        protected FValue act(FObject selfValue) {
            FType ty = ((ReflectedType)selfValue).getTy();
            return FString.make("Reflect[\\"+ty.toString()+"\\]");
        }
    }
}
