/********************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ********************************************************************************/

package com.sun.fortress.interpreter.evaluator.tasks;

import com.sun.fortress.exceptions.FortressError;
import com.sun.fortress.exceptions.transactions.AbortedException;
import com.sun.fortress.exceptions.transactions.OrphanedException;
import com.sun.fortress.interpreter.evaluator.transactions.ContentionManager;
import com.sun.fortress.interpreter.evaluator.transactions.Transaction;
import com.sun.fortress.interpreter.evaluator.transactions.manager.GreedyManager;
import jsr166y.ForkJoinWorkerThread;

import java.util.concurrent.Callable;

public class FortressTaskRunner extends ForkJoinWorkerThread {

    private static ContentionManager manager = new GreedyManager();
    public volatile BaseTask task;
    private int retries;
    private static long startTime = System.currentTimeMillis();

    public int retries() {
        return retries;
    }

    private static void incRetries() {
        FortressTaskRunner runner = (FortressTaskRunner) Thread.currentThread();
        runner.retries += 1;
    }

    private static void resetRetries() {
        FortressTaskRunner runner = (FortressTaskRunner) Thread.currentThread();
        runner.retries = 0;
    }

    private static int getRetries() {
        FortressTaskRunner runner = (FortressTaskRunner) Thread.currentThread();
        return runner.retries;
    }

    public BaseTask task() {
        return task;
    }

    public static BaseTask getTask() {
        FortressTaskRunner runner = (FortressTaskRunner) Thread.currentThread();
        return runner.task();
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
    public static boolean validate() {
        return transaction().validate();
    }

    /**
     * Gets the current transaction, if any, of the invoking <code>Thread</code>.
     *
     * @return the current thread's current transaction; <code>null</code> if
     *         there is no current transaction.
     */
    static public Transaction getTransaction() {
        return transaction();
    }

    /**
     * Gets the contention manager
     *
     * @return the contention manager
     */

    public static ContentionManager getContentionManager() {
        return manager;
    }

    public static void debugPrintln(String msg) {
        FortressTaskRunner runner = (FortressTaskRunner) Thread.currentThread();
        long t = System.currentTimeMillis() - startTime;
        System.out.println(runner.getName() + ":" + runner.task() + ":" + t + "ms " + msg);
    }

    public static <T> T doItOnce(Callable<T> xaction) throws AbortedException, Exception {
        getTask().beginTransaction();
        try {
            T result = xaction.call();
            if (getTask().commitTransaction()) {
                return result;
            } else {
                getTask().abortTransaction();
                throw new AbortedException(transaction(), " commit failed");
            }
        }
        catch (FortressError fe) {
            throw fe;
        }
        finally {
            getTask().giveUpTransaction();
        }
    }

    /**
     * Execute a transaction
     *
     * @param xaction execute this object's <CODE>call()</CODE> method.
     * @return result of <CODE>call()</CODE> method
     */

    public static <T> T doIt(Callable<T> xaction) throws Exception {
        resetRetries();
        while (true) {
            // Someday figure out how aborted transactions get this far...
            Transaction me = getTransaction();
            if (me != null && !me.isActive()) {
                throw new AbortedException(me, "Got to doit with an aborted current transaction");
            }
            try {
                T result = doItOnce(xaction);
                return result;
            }
            catch (OrphanedException oe) {
                if (oe.getTransaction() != null && oe.getTransaction() != me && oe.getTransaction().getParent() != me)
                    throw new RuntimeException(
                            Thread.currentThread().getName() + " OE transaction  = " + oe + " me = " + me +
                            " oe parent = " + oe.getTransaction().getParent());
            }
            catch (AbortedException ae) {
                if (ae.getTransaction() != null && ae.getTransaction() != me && ae.getTransaction().getParent() != me)
                    throw new RuntimeException(
                            Thread.currentThread().getName() + " AE transaction  = " + ae + " me = " + me +
                            " ae parent = " + ae.getTransaction().getParent());
            }
            finally {
                incRetries();
            }
        }
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
