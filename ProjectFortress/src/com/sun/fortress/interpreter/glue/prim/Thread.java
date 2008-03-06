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
import com.sun.fortress.interpreter.evaluator.Evaluator;
import com.sun.fortress.interpreter.evaluator.tasks.BaseTask;
import com.sun.fortress.interpreter.evaluator.tasks.FortressTaskRunner;
import com.sun.fortress.interpreter.evaluator.tasks.FortressTaskRunnerGroup;
import com.sun.fortress.interpreter.evaluator.transactions.Transaction;
import com.sun.fortress.interpreter.evaluator.tasks.SpawnTask;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.evaluator.types.FTypeObject;
import com.sun.fortress.interpreter.evaluator.values.Constructor;
import com.sun.fortress.interpreter.evaluator.values.FBool;
import com.sun.fortress.interpreter.evaluator.values.FInt;
import com.sun.fortress.interpreter.evaluator.values.FObject;
import com.sun.fortress.interpreter.evaluator.values.FOrdinaryObject;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.interpreter.evaluator.values.FVoid;
import com.sun.fortress.interpreter.evaluator.values.SingleFcn;
import com.sun.fortress.interpreter.glue.NativeFn0;
import com.sun.fortress.interpreter.glue.NativeMeth0;
import com.sun.fortress.interpreter.glue.NativeMeth1;
import com.sun.fortress.interpreter.glue.NativeMeth2;
import com.sun.fortress.nodes.AbsDeclOrDecl;
import com.sun.fortress.nodes.Expr;
import com.sun.fortress.nodes.GenericWithParams;

import static com.sun.fortress.interpreter.evaluator.ProgramError.errorMsg;
import static com.sun.fortress.interpreter.evaluator.ProgramError.error;

public class Thread extends Constructor {

    public Thread(BetterEnv env, FTypeObject selfType, GenericWithParams def) {
        super(env, selfType, def);
    }

    protected FObject makeAnObject(BetterEnv lex_env, BetterEnv self_env) {
        return new Thread_prim(selfType, lex_env, self_env);
    }

    private static final class Thread_prim extends FOrdinaryObject {
        private final FortressTaskRunnerGroup group;
        private final SpawnTask st;

        public Thread_prim(FType selfType, BetterEnv lexical_env, BetterEnv self_dot_env) {
            super(selfType, lexical_env, self_dot_env);
            int numThreads = Runtime.getRuntime().availableProcessors();
            String numThreadsString = System.getenv("FORTRESS_THREADS");

            if (numThreadsString != null)
                numThreads = Integer.parseInt(numThreadsString);
            group = new FortressTaskRunnerGroup(numThreads);
            SingleFcn sf = (SingleFcn) self_dot_env.getValue("fcn");
            st = new SpawnTask(sf,new Evaluator(self_dot_env));
	    group.execute(st);
        }
    }

    public static final class val extends NativeMeth0 {
        public FValue act(FObject self) {
            Thread_prim tp = (Thread_prim) self;
            FValue r = tp.st.result();
            if (r==null) {
                error(errorMsg("Access to uninitialized spawned thread result "));
            }
            return r;
        }
    }

    public static final class wait extends NativeMeth0 {
        public FValue act(FObject self) {
            Thread_prim tp = (Thread_prim) self;
            tp.st.waitForResult();
            return FVoid.V;
        }
    }

    public static final class ready extends NativeMeth0 {
        public FValue act(FObject self) {
            Thread_prim tp = (Thread_prim) self;
            return FBool.make(tp.st.isDone());
        }
    }

    public static final class stop extends NativeMeth0 {
        public FValue act(FObject self) {
            Thread_prim tp = (Thread_prim) self;
            tp.st.cancel();
            return FVoid.V;
        }
    }

    public static final class abort extends NativeFn0 {
        protected FValue act() {
            Transaction current = BaseTask.getThreadState().transaction();
	    if (current != null)
		current.abort();
            return FVoid.V;
	}
    }

}
