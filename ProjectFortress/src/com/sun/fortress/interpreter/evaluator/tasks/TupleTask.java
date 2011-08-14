/********************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ********************************************************************************/

package com.sun.fortress.interpreter.evaluator.tasks;

import static com.sun.fortress.exceptions.ProgramError.error;
import static com.sun.fortress.exceptions.ProgramError.errorMsg;
import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.interpreter.evaluator.Evaluator;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.nodes.Expr;
import jsr166y.ForkJoinTask;

public class TupleTask extends BaseTask {
    Evaluator eval;
    Expr expr;
    FValue res;

    public TupleTask(Expr ex, Evaluator ev) {
        super(FortressTaskRunner.getTask());
        expr = ex;
        eval = ev;
    }

    public void compute() {
        FortressTaskRunner.setCurrentTask(this);
        try {
            Environment inner = eval.e.extendAt(expr);
            Evaluator e = new Evaluator(inner);
            res = expr.accept(e);
        }
        catch (NullPointerException e) {
            throw e;
        }
        catch (Exception e) {
            recordException(e);
        }
        finally {
            /* Null out fields so they are not retained by GC after termination. */
            eval = null;
            expr = null;
            transaction = null;
        }
    }

    public void print() {
        System.out.println(
                "Tuple Task: eval = " + eval + "\n\t Expr = " + expr + "\n\t Res = " + res + "\n\t Thread = " +
                Thread.currentThread());
    }

    public String toString() {
        return "[TupleTask" + name() + ":" + transaction() + "]";
    }

    public FValue getRes() {
        return res;
    }

    public Expr getExpr() {
        return expr;
    }

    public FValue getResOrException() {
        if (causedException()) {
            Throwable t = taskException();
            if (t instanceof Error) {
                throw (Error) t;
            } else if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            } else {
                error(getExpr(), errorMsg("Wrapped Exception ", t));
            }
        }
        return res;
    }

    // Commented out because not supported in new jsr166y library.

    // This is a static method of jsr166y.forkjoin.RecursiveAction,
    // but this has better static type information and exploits it to
    // avoid an interface call as suggested by Doug Lea.
    //
    // Actual code cribbed from jsr166y.forkjoin.ForkJoinTask.
    // However we rely on the invariant that the TupleTasks are all non-null.
    //     public static void forkJoin(TupleTask []tasks) {
    //         if (true) { // switch to false for standard version.
    //             int last = tasks.length - 1;
    //             Throwable ex = null;
    //             for (int i = last; i > 0; --i) {
    //                 tasks[i].fork();
    //             }
    //             ex = tasks[0].exec();
    //             for (int i = 1; i <= last; ++i) {
    //                 TupleTask t = tasks[i];
    //                 boolean pop = ForkJoinWorkerThread.removeIfNextLocalTask(t);
    //                 if (ex != null)
    //                     t.cancel();
    //                 else if (!pop)
    //                     ex = t.quietlyJoin();
    //                 else
    //                     ex = t.exec();
    //             }
    //             if (ex != null)
    //                 bug(ex);
    //         } else {
    //             BaseTask.forkJoin(tasks);
    //         }
    //     }

    public static boolean worthSpawning() {
        return (ForkJoinTask.getSurplusQueuedTaskCount() <= 3);
    }

}
