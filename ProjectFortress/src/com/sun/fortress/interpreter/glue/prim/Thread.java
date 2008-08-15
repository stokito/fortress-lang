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

import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.interpreter.evaluator.Evaluator;
import com.sun.fortress.interpreter.evaluator.tasks.FortressTaskRunner;
import com.sun.fortress.interpreter.evaluator.tasks.FortressTaskRunnerGroup;
import com.sun.fortress.interpreter.evaluator.tasks.SpawnTask;
import com.sun.fortress.interpreter.evaluator.transactions.Transaction;
import com.sun.fortress.interpreter.evaluator.types.FTypeObject;
import com.sun.fortress.interpreter.evaluator.values.FBool;
import com.sun.fortress.interpreter.evaluator.values.FObject;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.interpreter.evaluator.values.FVoid;
import com.sun.fortress.interpreter.evaluator.values.NativeConstructor;
import com.sun.fortress.interpreter.evaluator.values.SingleFcn;
import com.sun.fortress.interpreter.glue.NativeFn0;
import com.sun.fortress.interpreter.glue.NativeMeth0;
import com.sun.fortress.nodes.GenericWithParams;

public class Thread extends NativeConstructor {

    public Thread(Environment env, FTypeObject selfType, GenericWithParams def) {
        super(env, selfType, def);
    }

    protected FNativeObject makeNativeObject(List<FValue> args,
                                             NativeConstructor con) {
        return new Thread_prim((SingleFcn)args.get(0), con);
    }

    private static final class Thread_prim extends FNativeObject {
        private final NativeConstructor con;
        private final FortressTaskRunnerGroup group;
        private final SpawnTask st;

        public Thread_prim(SingleFcn sf, NativeConstructor con) {
            super(con);
            this.con = con;
            // For Now we are limiting spawn to creating only 1 thread
			//	    int numThreads = Runtime.getRuntime().availableProcessors();
			//            String numThreadsString = System.getenv("FORTRESS_THREADS");
			
			//            if (numThreadsString != null)
			//                numThreads = Integer.parseInt(numThreadsString);
			int numThreads = 1;
			group = new FortressTaskRunnerGroup(numThreads);
			st = new SpawnTask(sf,new Evaluator(getSelfEnv()));
			group.execute(st);
        }

        public NativeConstructor getConstructor() { return con; }

        public boolean seqv(FValue v) {
            return (this==v);
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
            tp.group.shutdownNow();
            //            tp.st.cancel();
            return FVoid.V;
        }
    }

    public static final class abort extends NativeFn0 {
        protected FValue act() {
            Transaction current = FortressTaskRunner.getTransaction();
            if (current != null)
                current.abort();
            return FVoid.V;
        }
    }

}
