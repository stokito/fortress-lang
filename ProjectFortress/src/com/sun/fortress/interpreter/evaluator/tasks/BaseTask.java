/********************************************************************************
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
********************************************************************************/

package com.sun.fortress.interpreter.evaluator.tasks;

import java.io.IOException;

import jsr166y.forkjoin.*;
import com.sun.fortress.interpreter.evaluator.transactions.exceptions.AbortedException;
import com.sun.fortress.interpreter.evaluator.transactions.exceptions.GracefulException;
import com.sun.fortress.interpreter.evaluator.transactions.exceptions.PanicException;
import java.util.concurrent.Callable;
import com.sun.fortress.interpreter.evaluator.tasks.ThreadState;

public abstract class BaseTask extends RecursiveAction {
    Throwable err;
    boolean causedException;
    final ThreadState threadState;
    final BaseTask parent;

    public BaseTask() {
        causedException = false;
        threadState = new ThreadState();
        parent = getCurrentTask();
    }

    public void recordException(Throwable t) {
        causedException = true;
        err = t;
    }

    public boolean causedException() {return causedException;}
    public Throwable taskException() {return err;}

    public static BaseTask getCurrentTask() {
        /* This may get called before we've started our FortressTaskGroup so in
           that case we have no current task and need to return null */
        Thread t = Thread.currentThread();
        BaseTask result = null;
        if (t instanceof FortressTaskRunner) {
            FortressTaskRunner taskrunner = (FortressTaskRunner) t;
            result = (BaseTask) taskrunner.getCurrentTask();
        }
        return result;
    }

    public static void setCurrentTask(BaseTask task) {
        FortressTaskRunner taskrunner = (FortressTaskRunner) Thread.currentThread();
        taskrunner.setCurrentTask(task);
    }

    public abstract void print();

    public static void printTaskTrace() {
        BaseTask currentTask = getCurrentTask();
        while (currentTask != null) {
            currentTask.print();
            currentTask = currentTask.parent;
        }
    }

    public ThreadState threadState() { return threadState;}

    public static ThreadState getThreadState() {
        BaseTask task = getCurrentTask();
        if (task == null)
            throw new RuntimeException("Not in a task!");
        return task.threadState;
    }
}
