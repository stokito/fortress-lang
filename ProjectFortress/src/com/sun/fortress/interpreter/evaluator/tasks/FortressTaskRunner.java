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

import com.sun.fortress.exceptions.FortressError;
import com.sun.fortress.exceptions.FortressException;
import com.sun.fortress.exceptions.transactions.AbortedException;
import com.sun.fortress.exceptions.transactions.OrphanedException;
import com.sun.fortress.exceptions.transactions.PanicException;
import com.sun.fortress.interpreter.evaluator.transactions.ContentionManager;
import com.sun.fortress.interpreter.evaluator.transactions.manager.FortressManager2;
import com.sun.fortress.interpreter.evaluator.transactions.manager.FortressManager3;
import com.sun.fortress.interpreter.evaluator.transactions.Transaction;
import java.util.concurrent.Callable;

import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.interpreter.evaluator.values.FVoid;
import static com.sun.fortress.exceptions.InterpreterBug.bug;

public class FortressTaskRunner extends ForkJoinWorkerThread {

    private static ContentionManager manager = new FortressManager3();
    
    public volatile BaseTask task;

    public BaseTask task() {return task;}

    public static BaseTask getTask() {
	FortressTaskRunner runner = (FortressTaskRunner) Thread.currentThread();
	return runner.task();
    }

    public static TaskState getTaskState() {
		FortressTaskRunner runner = (FortressTaskRunner) Thread.currentThread();
		TaskState result = runner.task().taskState();
		return result;
    }

    public static Transaction transaction() {
	return getTaskState().transaction();
    }

    public static boolean inATransaction() {
		return transaction() != null;
    }

    public void setTask(BaseTask t) {task = t;}

    public static void setCurrentTask(BaseTask task) {
	FortressTaskRunner runner = (FortressTaskRunner) Thread.currentThread();
	runner.setTask(task);
    }


    public FortressTaskRunner(FortressTaskRunnerGroup group) {super(group);}

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
	return getTaskState().validate();
    }

    /**
     * Gets the current transaction, if any, of the invoking <code>Thread</code>.
     *
     * @return the current thread's current transaction; <code>null</code> if
     *         there is no current transaction.
     */
    static public Transaction getTransaction() {
		return getTaskState().transaction();
    }

    /**
     * Gets the contention manager
     *
     * @return the contention manager
     */

    public static ContentionManager getContentionManager() { return manager;}

    public static void debugPrintln(String msg) { 
		FortressTaskRunner runner = (FortressTaskRunner) Thread.currentThread();
		System.out.println(runner.getName() + ":" + runner.task() + ":" + msg);
    }

    public static <T> T doItOnce(Callable<T> xaction) throws AbortedException, Exception {		
		getTaskState().beginTransaction();
		try {
			T result = xaction.call();
			if (getTaskState().commitTransaction()) {
				return result;
			} else {
				getTransaction().abort();
				throw new AbortedException(getTransaction(), " commit failed");
			}
		} catch (FortressError fe) {
			throw fe;
		} finally {
			getTaskState().giveUpTransaction();		
		}
    }

    /**
     * Execute a transaction
     * @param xaction execute this object's <CODE>call()</CODE> method.
     * @return result of <CODE>call()</CODE> method
     */

    public static <T> T doIt(Callable<T> xaction) throws Exception {
        while (true) {
			// Someday figure out how aborted transactions get this far...
			Transaction me = getTransaction();
			if (me != null && !me.isActive())
				throw new AbortedException(me, "Got to doit with an aborted current transaction");
			try {
				T result = doItOnce(xaction);
				return result;
			} catch (OrphanedException oe) {
			} catch (AbortedException ae) {
			}
		}
	}

    /**
     * get thread ID for debugging
     * @return unique id
     */
    public static int getID() {
	FortressTaskRunner runner = (FortressTaskRunner) Thread.currentThread();
	return runner.hashCode();
    }

}
