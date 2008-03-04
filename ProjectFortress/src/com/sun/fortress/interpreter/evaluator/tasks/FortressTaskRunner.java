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

import jsr166y.forkjoin.*;
import java.util.concurrent.atomic.AtomicInteger;

import com.sun.fortress.interpreter.evaluator.transactions.ContentionManager;
import com.sun.fortress.interpreter.evaluator.transactions.exceptions.AbortedException;
import com.sun.fortress.interpreter.evaluator.transactions.exceptions.PanicException;
import com.sun.fortress.interpreter.evaluator.transactions.Transaction;
import com.sun.fortress.interpreter.evaluator.FortressException;
import java.util.concurrent.Callable;
import com.sun.fortress.interpreter.evaluator.FortressError;

import static com.sun.fortress.interpreter.evaluator.InterpreterBug.bug;

public class FortressTaskRunner extends ForkJoinWorkerThread {
 /**
 * Contention manager class.
 */
    protected static Class contentionManagerClass;
/**
 * number of committed transactions for all threads
 */
    public static long totalCommitted = 0;
/**
 * total number of transactions for all threads
 */
    public static long totalTotal = 0;
/**
 * number of committed memory references for all threads
 */
    public static long totalCommittedMemRefs = 0;
/**
 * total number of memory references for all threads
 */
    public static long totalTotalMemRefs = 0;

    private static int MAX_NESTING_DEPTH = 100;

    private static Object lock = new Object();

    public volatile BaseTask currentTask;

    public BaseTask getCurrentTask() {return currentTask;}

    public void setCurrentTask(BaseTask task) {
        currentTask = task;
    }

    public FortressTaskRunner(FortressTaskRunnerGroup group) {
        super(group);
        try {
            Class managerClass = Class.forName("com.sun.fortress.interpreter.evaluator.transactions.manager.FortressManager2");
            setContentionManagerClass(managerClass);
        } catch (ClassNotFoundException ex) {
            System.out.println("UhOh Contention Manager not found");
            System.exit(0);
        }
    }


    /**
     * Establishes a contention manager.  You must call this method
     * before creating any <code>Thread</code>.
     *
     * @see com.sun.fortress.interpreter.evaluator.transactions.ContentionManager
     * @param theClass class of desired contention manager.
     */
    public static void setContentionManagerClass(Class theClass) {
        Class cm;
        try {
            cm = Class.forName("com.sun.fortress.interpreter.evaluator.transactions.ContentionManager");
        } catch (ClassNotFoundException e) {
            throw new PanicException(e);
        }
        try {
            contentionManagerClass = theClass;
        } catch (Exception e) {
            throw new PanicException("The class " + theClass
                                     + " does not implement com.sun.fortress.interpreter.evaluator.transactions.ContentionManager");
        }
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
    static public boolean validate() {
        ThreadState threadState = BaseTask.getThreadState();
        return threadState.validate();
    }

    /**
     * Gets the current transaction, if any, of the invoking <code>Thread</code>.
     *
     * @return the current thread's current transaction; <code>null</code> if
     *         there is no current transaction.
     */
    static public Transaction getTransaction() {
        ThreadState threadState = BaseTask.getThreadState();
        return threadState.transaction();
    }

    /**
     * Gets the contention manager of the invoking <code>Thread</code>.
     *
     * @return the invoking thread's contention manager
     */
    static public ContentionManager getContentionManager() {
        ThreadState threadState = BaseTask.getThreadState();
        return threadState.manager();
    }

    public static <T> T doItOnce(Callable<T> xaction) {
        ThreadState threadState = BaseTask.getThreadState();
        ContentionManager manager = threadState.manager();
        T result = null;
        threadState.beginTransaction();
        try {
            result = xaction.call();
            if (threadState.commitTransaction()) {
                threadState.addToCommittedMemRefs(threadState.memRefs());
                return result;
            } else {
                throw new AbortedException();
            }
        } catch (AbortedException e) {
            throw e;
        } catch (FortressError e) {
            throw e;
        } catch (Exception e) {
            throw new PanicException(e);
        } finally {
            threadState.endTransaction();
        }
    }

    /**
     * Execute a transaction
     * @param xaction execute this object's <CODE>call()</CODE> method.
     * @return result of <CODE>call()</CODE> method
     */

    public static <T> T doIt(Callable<T> xaction) {
        while (true) {
            try {
                T result = doItOnce(xaction);
                return result;
            } catch (AbortedException e) {
                if (BaseTask.getThreadState().transactionNesting() > 0) {
                    throw e;  // to be handled by outermost transaction.
                }
            }
        }
    }

    /**
     * Execute transaction
     * @param xaction call this object's <CODE>run()</CODE> method
     */
    public static void doIt(final Runnable xaction) {
        doIt(new Callable<Boolean>() {
                 public Boolean call() {
                     xaction.run();
                     return false;
                 };
             });
    }

    /**
     * number of transactions committed by this thread
     * @return number of transactions committed by this thread
     */
    public static long getCommitted() {
        return totalCommitted;
    }

    /**
     * umber of transactions aborted by this thread
     * @return number of aborted transactions
     */
    public static long getAborted() {
        return totalTotal -  totalCommitted;
    }

    /**
     * number of transactions executed by this thread
     * @return number of transactions
     */
    public static long getTotal() {
        return totalTotal;
    }

    /**
     * Register a method to be called every time this thread validates any transaction.
     * @param c abort if this object's <CODE>call()</CODE> method returns false
     */
    public static void onValidate(Callable<Boolean> c) {
        ThreadState threadState = BaseTask.getThreadState();
        threadState.onValidate.add(c);
    }
    /**
     * Register a method to be called every time the current transaction is validated.
     * @param c abort if this object's <CODE>call()</CODE> method returns false
     */
    public static void onValidateOnce(Callable<Boolean> c) {
        ThreadState threadState = BaseTask.getThreadState();
        threadState.onValidateOnce.add(c);
    }
    /**
     * Register a method to be called every time this thread begins a transaction.
     * @param r call this object's <CODE>run()</CODE> method
     */
    public static void onBegin(Runnable r) {
        ThreadState threadState = BaseTask.getThreadState();
        threadState.onBegin.add(r);
    }
    /**
     * Register a method to be called the next time this thread begins a transaction.
     * @param r call this object's <CODE>run()</CODE> method
     */
    public static void onBeginOnce(Runnable r) {
        ThreadState threadState = BaseTask.getThreadState();
        threadState.onBeginOnce.add(r);
    }
    /**
     * Register a method to be called every time this thread commits a transaction.
     * @param r call this object's <CODE>run()</CODE> method
     */
    public static void onCommit(Runnable r) {
        ThreadState threadState = BaseTask.getThreadState();
        threadState.onCommit.add(r);
    }
    /**
     * Register a method to be called once if the current transaction commits.
     * @param r call this object's <CODE>run()</CODE> method
     */
    public static void onCommitOnce(Runnable r) {
        ThreadState threadState = BaseTask.getThreadState();
        threadState.onCommitOnce.add(r);
    }
    /**
     * Register a method to be called every time this thread aborts a transaction.
     * @param r call this objec't <CODE>run()</CODE> method
     */
    public static void onAbort(Runnable r) {
        ThreadState threadState = BaseTask.getThreadState();
        threadState.onAbort.add(r);
    }
    /**
     * Register a method to be called once if the current transaction aborts.
     * @param r call this object's <CODE>run()</CODE> method
     */
    public static void onAbortOnce(Runnable r) {
        ThreadState threadState = BaseTask.getThreadState();
        threadState.onAbortOnce.add(r);
    }
    /**
     * get thread ID for debugging
     * @return unique id
     */
    public static int getID() {
        ThreadState threadState = BaseTask.getThreadState();
        return threadState.hashCode();
    }

    /**
     * reset thread statistics
     */
    public static void clear() {
        totalTotal = 0;
        totalCommitted = 0;
        totalCommittedMemRefs = 0;
        totalTotalMemRefs = 0;
    }

}
