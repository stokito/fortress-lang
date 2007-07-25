/********************************************************************************
    Copyright 2007 Sun Microsystems, Inc., 
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

import EDU.oswego.cs.dl.util.concurrent.FJTask;
import EDU.oswego.cs.dl.util.concurrent.FJTaskRunner;

import com.sun.fortress.interpreter.evaluator.transactions.ContentionManager;
import com.sun.fortress.interpreter.evaluator.transactions.exceptions.AbortedException;
import com.sun.fortress.interpreter.evaluator.transactions.exceptions.GracefulException;
import com.sun.fortress.interpreter.evaluator.transactions.exceptions.PanicException;
import com.sun.fortress.interpreter.evaluator.transactions.exceptions.SnapshotException;
import com.sun.fortress.interpreter.evaluator.transactions.Transaction;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import java.util.concurrent.Callable;
import com.sun.fortress.interpreter.evaluator.ProgramError;

public class FortressTaskRunner extends FJTaskRunner {
 /**
 * Contention manager class.
 */
    protected static Class contentionManagerClass;

/**
 * Set to true when benchmark runs out of time.
 **/
    public static volatile boolean stop = false;
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

    static ThreadLocal<ThreadState> _threadState = new ThreadLocal<ThreadState>() {
        protected synchronized ThreadState initialValue() {
        return new ThreadState();

	}
    };
    static ThreadLocal<Thread> _thread = new ThreadLocal<Thread>() {
        protected synchronized Thread initialValue() {
        return null;
        }
    };

    private static int MAX_NESTING_DEPTH = 100;

    private static Object lock = new Object();

    public volatile BaseTask currentTask;

    public BaseTask getCurrentTask() {return currentTask;}
    public void setCurrentTask(BaseTask task) {currentTask = task;}

    public FortressTaskRunner(FortressTaskRunnerGroup group) {
	super(group); 
        try {
            Class managerClass = Class.forName("com.sun.fortress.interpreter.evaluator.transactions.manager.FortressManager");
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
	ThreadState threadState = _threadState.get();
	return threadState.validate();
    }

    /**
     * Gets the current transaction, if any, of the invoking <code>Thread</code>.
     *
     * @return the current thread's current transaction; <code>null</code> if
     *         there is no current transaction.
     */
    static public Transaction getTransaction() {
	return _threadState.get().transaction;
    }

    /**
     * Gets the contention manager of the invoking <code>Thread</code>.
     *
     * @return the invoking thread's contention manager
     */
    static public ContentionManager getContentionManager() {
	return _threadState.get().manager;
    }

    /**
     * Execute a transaction
     * @param xaction execute this object's <CODE>call()</CODE> method.
     * @return result of <CODE>call()</CODE> method
     */
    public static <T> T doIt(Callable<T> xaction) {
	ThreadState threadState = _threadState.get();
	ContentionManager manager = threadState.manager;
	T result = null;
	try {
	    while (!FortressTaskRunner.stop) {
		threadState.beginTransaction();
		try {
		    result = xaction.call();
		} catch (AbortedException e) {
		} catch (SnapshotException e) {
		    threadState.abortTransaction();
		} catch (ProgramError e) {
 		    e.printStackTrace(System.out);
		    throw new PanicException("Unhandled exception " + e);
		} catch (Exception e) {
		    e.printStackTrace();
		    throw new PanicException("Unhandled exception " + e);
		} catch (Error e) {
		    e.printStackTrace();
		    throw new PanicException("Error " + e);
		}
                threadState.totalMemRefs += threadState.transaction.memRefs;
		if (threadState.commitTransaction()) {
		    threadState.committedMemRefs += threadState.transaction.memRefs;
		    return result;
		}
		threadState.transaction.attempts++;
		// transaction aborted
	    }
	    if (threadState.transaction != null) {
		threadState.abortTransaction();
	    }
	} finally {
	    threadState.transaction = null;
	}
	throw new GracefulException();
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
	_threadState.get().onValidate.add(c);
    }
    /**
     * Register a method to be called every time the current transaction is validated.
     * @param c abort if this object's <CODE>call()</CODE> method returns false
     */
    public static void onValidateOnce(Callable<Boolean> c) {
	_threadState.get().onValidateOnce.add(c);
    }
    /**
     * Register a method to be called every time this thread begins a transaction.
     * @param r call this object's <CODE>run()</CODE> method
     */
    public static void onBegin(Runnable r) {
	_threadState.get().onBegin.add(r);
    }
    /**
     * Register a method to be called the next time this thread begins a transaction.
     * @param r call this object's <CODE>run()</CODE> method
     */
    public static void onBeginOnce(Runnable r) {
	_threadState.get().onBeginOnce.add(r);
    }
    /**
     * Register a method to be called every time this thread commits a transaction.
     * @param r call this object's <CODE>run()</CODE> method
     */
    public static void onCommit(Runnable r) {
	_threadState.get().onCommit.add(r);
    }
    /**
     * Register a method to be called once if the current transaction commits.
     * @param r call this object's <CODE>run()</CODE> method
     */
    public static void onCommitOnce(Runnable r) {
	_threadState.get().onCommitOnce.add(r);
    }
    /**
     * Register a method to be called every time this thread aborts a transaction.
     * @param r call this objec't <CODE>run()</CODE> method
     */
    public static void onAbort(Runnable r) {
	_threadState.get().onAbort.add(r);
    }
    /**
     * Register a method to be called once if the current transaction aborts.
     * @param r call this object's <CODE>run()</CODE> method
     */
    public static void onAbortOnce(Runnable r) {
	_threadState.get().onAbortOnce.add(r);
    }
    /**
     * get thread ID for debugging
     * @return unique id
     */
    public static int getID() {
	return _threadState.get().hashCode();
    }

    /**
     * reset thread statistics
     */
    public static void clear() {
	totalTotal = 0;
	totalCommitted = 0;
	totalCommittedMemRefs = 0;
	totalTotalMemRefs = 0;
	stop = false;
    }

    /**
     * Class that holds thread's actual state
     */
    public static class ThreadState {
  
	int depth = 0;
	ContentionManager manager;
  
	private long committed = 0;        // number of committed transactions
	private long total = 0;            // total number of transactions
	private long committedMemRefs = 0; // number of committed reads and writes
	private long totalMemRefs = 0;     // total number of reads and writes
  
	Set<Callable<Boolean>> onValidate = new HashSet<Callable<Boolean>>();
	Set<Runnable>          onCommit   = new HashSet<Runnable>();
	Set<Runnable>          onBegin    = new HashSet<Runnable>();
	Set<Runnable>          onAbort    = new HashSet<Runnable>();
	Set<Callable<Boolean>> onValidateOnce = new HashSet<Callable<Boolean>>();
	Set<Runnable>          onBeginOnce    = new HashSet<Runnable>();
	Set<Runnable>          onCommitOnce   = new HashSet<Runnable>();
	Set<Runnable>          onAbortOnce    = new HashSet<Runnable>();
  
	Transaction transaction = null;
  
	/**
	 * Creates new ThreadState
	 */
	public ThreadState() {
	    try {
		manager = (ContentionManager)FortressTaskRunner.contentionManagerClass.newInstance();
	    } catch (NullPointerException e) {
		throw new PanicException("No default contention manager class set.");
	    } catch (Exception e) {  // Some problem with instantiation
		throw new PanicException(e);
	    }
	}
  
	/**
	 * Resets any metering information (commits/aborts, etc).
	 */
	public void reset() {
	    committed = 0;        // number of committed transactions
	    total = 0;            // total number of transactions
	    committedMemRefs = 0; // number of committed reads and writes
	    totalMemRefs = 0;     // total number of reads and writes
	}
  
	/**
	 * used for debugging
	 * @return string representation of thread state
	 */
	public String toString() {
	    return
		"Thread" + hashCode() + "["+
		"committed: " + committed + "," +
		"aborted: " + ( total -  committed) +
		"]";
	}
  
	/**
	 * Can this transaction still commit?
	 * This method may be called at any time, not just at transaction end,
	 * so we do not clear the onValidateOnce table.
	 * @return true iff transaction might still commit
	 */
	public boolean validate() {
	    try {
		// permanent
		for (Callable<Boolean> v : onValidate) {
		    if (!v.call()) {
			return false;
		    }
		}
		// temporary
		for (Callable<Boolean> v : onValidateOnce) {
		    if (!v.call()) {
			return false;
		    }
		}
		onValidateOnce.clear();
		return transaction.validate();
	    } catch (Exception ex) {
		return false;
	    }
	}
  
	/**
	 * Call methods registered to be called on transaction begin.
	 */
	public void runBeginHandlers() {
	    try {
		// permanent
		for (Runnable r: onBegin) {
		    r.run();
		}
		// temporary
		for (Runnable r: onBeginOnce) {
		    r.run();
		}
		onBeginOnce.clear();
	    } catch (Exception ex) {
		throw new PanicException(ex);
	    }
	}
	/**
	 * Call methods registered to be called on commit.
	 */
	public void runCommitHandlers() {
	    try {
		// permanent
		for (Runnable r: onCommit) {
		    r.run();
		}
		// temporary
		for (Runnable r: onCommitOnce) {
		    r.run();
		}
		onCommitOnce.clear();
	    } catch (Exception ex) {
		throw new PanicException(ex);
	    }
	}
  
	/**
	 * Starts a new transaction.  Cannot nest transactions deeper than
	 * <code>Thread.MAX_NESTING_DEPTH.</code> The contention manager of the
	 * invoking thread is notified when a transaction is begun.
	 */
	public void beginTransaction() {
	    transaction = new Transaction();
	    if (depth == 0) {
		total++;
	    }
	    // first thing to fix if we allow nested transactions
	    if (depth >= 1) {
		throw new PanicException("beginTransaction: attempting to nest transactions too deeply.");
	    }
	    depth++;
	}
  
	/**
	 * Attempts to commit the current transaction of the invoking
	 * <code>Thread</code>.  Always succeeds for nested
	 * transactions.  The contention manager of the invoking thread is
	 * notified of the result.  If the transaction does not commit
	 * because a <code>TMObject</code> opened for reading was
	 * invalidated, the contention manager is also notified of the
	 * inonValidate.
	 *
	 *
	 * @return whether commit succeeded.
	 */
	public boolean commitTransaction() {
	    depth--;
	    if (depth < 0) {
		throw new PanicException("commitTransaction invoked when no transaction active.");
	    }
	    if (depth > 0) {
		throw new PanicException("commitTransaction invoked on nested transaction.");
	    }
	    if (depth == 0) {
		if (validate() && transaction.commit()) {
		    committed++;
		    runCommitHandlers();
		    return true;
		}
		abortTransaction();
		return false;
	    } else {
		return true;
	    }
	}
  
	/**
	 * Aborts the current transaction of the invoking <code>Thread</code>.
	 * Does not end transaction, but ensures it will never commit.
	 */
	public void abortTransaction() {
	    runAbortHandlers();
	    transaction.abort();
	}
  
	/**
	 * Call methods registered to be called on commit.
	 */
	public void runAbortHandlers() {
	    try {
		// permanent
		for (Runnable r: onAbort) {
		    r.run();
		}
		// temporary
		for (Runnable r: onAbortOnce) {
		    r.run();
		}
		onAbortOnce.clear();
	    } catch (Exception ex) {
		throw new PanicException(ex);
	    }
	}
    }

}










    
