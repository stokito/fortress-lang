/********************************************************************************
 Copyright 2008,2013, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ********************************************************************************/

package com.sun.fortress.runtimeSystem;

import jsr166y.ForkJoinWorkerThread;
import java.util.concurrent.Callable;

public class FortressTaskRunner extends ForkJoinWorkerThread {

    public volatile BaseTask task;
    private int retries;
    private static long startTime = System.currentTimeMillis();

    public int retries() {
        return retries;
    }

    public static void incRetries() {
        FortressTaskRunner runner = (FortressTaskRunner) Thread.currentThread();
        runner.retries += 1;
    }

    public static void resetRetries() {
        FortressTaskRunner runner = (FortressTaskRunner) Thread.currentThread();
        runner.retries = 0;
    }

    public static int getRetries() {
        FortressTaskRunner runner = (FortressTaskRunner) Thread.currentThread();
        return runner.retries;
    }

    public BaseTask task() {
        return task;
    }

    public static BaseTask getTask() {
        FortressTaskRunner runner = (FortressTaskRunner) Thread.currentThread();
        return runner.task;
    }

    public static Transaction transaction() {
        return getTask().transaction();
    }

    public static boolean inATransaction() {
        return transaction() != null;
    }

    public void setTask(BaseTask t) {
        task = t;
    }

    public static void setCurrentTask(BaseTask task) {
        FortressTaskRunner runner = (FortressTaskRunner) Thread.currentThread();
        runner.setTask(task);
    }


    public FortressTaskRunner(FortressTaskRunnerGroup group) {
        super(group);
        retries = 0;
    }

    /**
     * Tests whether the current transaction can still commit.  Does not
     * actually end the transaction (either <code>commitTransaction</code> or
     * <code>abortTransaction</code> must still be called).  The contention
     * manager of the invoking thread is notified if the onValidate fails
     * because a <code>TMObject</code> opened for reading was invalidated.
     *
     * @return whether the current transaction may commit successfully.
     */

    public static void debugPrintln(String msg) {
        FortressTaskRunner runner = (FortressTaskRunner) Thread.currentThread();
        long t = System.currentTimeMillis() - startTime;
        System.out.println(runner.getName() + ":" + runner.getTask() + ":" + t + "ms " + msg);
    }

    /**
     * get thread ID for debugging
     *
     * @return unique id
     */
    public static int getID() {
        FortressTaskRunner runner = (FortressTaskRunner) Thread.currentThread();
        return runner.hashCode();
    }
}
