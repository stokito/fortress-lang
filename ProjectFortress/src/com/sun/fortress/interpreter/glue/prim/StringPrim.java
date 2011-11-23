/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.glue.prim;

import com.sun.fortress.compiler.WellKnownNames;
import static com.sun.fortress.exceptions.InterpreterBug.bug;
import static com.sun.fortress.exceptions.ProgramError.error;
import com.sun.fortress.interpreter.Driver;
import com.sun.fortress.interpreter.evaluator.tasks.BaseTask;
import com.sun.fortress.interpreter.evaluator.tasks.FortressTaskRunner;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.evaluator.values.FString;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.interpreter.evaluator.values.FVoid;
import com.sun.fortress.interpreter.evaluator.values.Simple_fcn;
import com.sun.fortress.interpreter.glue.Glue;
import com.sun.fortress.interpreter.glue.NativeFn0;

import java.util.List;

/**
 * Functions from String.
 */
public class StringPrim {

    /* These should no longer be necessary,
    * but have been reinstated for compiler bootstrapping.
    */
    public static final class Print extends Util.S2V {
        protected void f(String x) {
            System.out.print(x);
        }
    }

    public static final class Println extends Util.S2V {
        protected void f(String x) {
            System.out.println(x);
        }
    }


    public static final class Match extends Util.SS2B {
        protected boolean f(String regex, String some) {
            return some.matches(regex);
        }
    }

    public static final class PrintTaskTrace extends NativeFn0 {
        protected FValue applyToArgs() {
            BaseTask.printTaskTrace();
            return FVoid.V;
        }
    }

    public static final class PrintThreadInfo extends Util.S2V {
        protected void f(String x) {
            System.out.println(" Thread " + java.lang.Thread.currentThread().getName() + " operating on value " + x);
        }
    }

    public static final class PrintWithThread extends Util.S2V {
        protected void f(String x) {
            System.out.print(" Thread " + java.lang.Thread.currentThread().getId() + ": " + x);
        }
    }


    public static final class PrintlnWithThread extends Util.S2V {
        protected void f(String x) {
            System.out.println(" Thread " + java.lang.Thread.currentThread().getId() + ": " + x);
        }
    }

    public static final class ThrowError extends Util.S2V {
        protected void f(String x) {
            String msg = " Thread " + java.lang.Thread.currentThread().getName() + " got error " + x;
            error(msg);
        }
    }

    public static final class PrintTransactionInfo extends Util.S2V {
        protected void f(String x) {
            FortressTaskRunner.debugPrintln(x);
        }
    }

    public static final class GetProperty extends Util.SS2S {
        protected String f(String name, String defaultValue) {
            return com.sun.fortress.repository.ProjectProperties.get(name, defaultValue);
        }
    }

    public static final class GetEnvironment extends Util.SS2S {
        protected String f(String name, String defaultValue) {
            String result = System.getenv(name);
            if (result == null) {
                return defaultValue;
            }
            return result;
        }
    }


    public static final class GetProgramArgs extends NativeFn0 {
        static volatile List<String> args = null;
        // Must be volatile.  We're initing in one thread, running in another.
        // Also we're now double-checked since we must initialize lazily after
        // the universe has started up and is in a happy state.
        static volatile FValue convertedArgs = null;

        // See also NonPrimitive.buildEnvFromParams.
        public static void registerProgramArgs(List<String> args) {
            GetProgramArgs.convertedArgs = null;
            GetProgramArgs.args = args;
        }

        private static FValue grindProgramArgs() {
            FType stringType = FString.EMPTY.type();
            int[] natParams = new int[1];
            natParams[0] = args.size();
            Simple_fcn mkArr = Glue.instantiateGenericConstructor(Driver.getFortressLibrary(),
                                                                  WellKnownNames.varargsFactoryName,
                                                                  stringType,
                                                                  natParams);
            FValue result = mkArr.applyToArgs();
            if (!(result instanceof PrimImmutableArray.PrimImmutableArrayObject)) {
                return bug("registerProgramArgs: unexpected varargs array type " + result);
            }
            PrimImmutableArray.PrimImmutableArrayObject arr = (PrimImmutableArray.PrimImmutableArrayObject) result;
            for (int i = 0; i < args.size(); i++) {
                arr.init(i, FString.make(args.get(i)));
            }
            return result;
        }

        protected FValue applyToArgs() {
            if (convertedArgs == null) {
                if (args == null) {
                    return bug("No program args were registered by calling " +
                               "StringPrim.GetProgramArgs.registerProgramArgs");
                }
                synchronized (args) {
                    if (convertedArgs == null) {
                        convertedArgs = grindProgramArgs();
                    }
                }
            }
            return convertedArgs;
        }
    }

}
