/********************************************************************************
 Copyright 2009,2013, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ********************************************************************************/

package com.sun.fortress.runtimeSystem;

import jsr166y.ForkJoinPool;
import jsr166y.ForkJoinTask;
import jsr166y.RecursiveAction;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import static com.sun.fortress.runtimeSystem.FortressExecutable.numThreads;
import static com.sun.fortress.runtimeSystem.FortressExecutable.spawnThreshold;
import static com.sun.fortress.runtimeSystem.FortressExecutable.useHelpJoin;

import com.sun.fortress.useful.MagicNumbers;

/** Base class for Fortress tasks.  Includes administrative methods
 *  that make code generation for task spawn a bit easier.
 *
 *  We shy away from the "easiest thing of all": forking in the constructor.
 *  Given the plethora of final fields in the usual Fortress task, such a step
 *  would be a memory model disaster!  Instead, we generate code like:
 *    Fib$task0 tmp = new Fib$task0(arg);
 *    tmp.forkIfProfitable();
 *    ... other code ...
 *    tmp.joinOrRun();
 *    ... use tmp.result (compiler-inserted field) if desired ...
 */
public abstract class BaseTask extends FortressExecutable {
    // Could get this by hacking ForkJoinTask's status field, but
    // not touching that for now as it's too changeable
    private static final int UNFORKED = 0;
    private static final int FORKED = 1;
    private static final int EXECUTED = 2;
    private int actuallyForked = UNFORKED;

    BaseTask parent;
    Transaction transaction;
    private static Random gen = new Random();

    private static Boolean debug = false;

    private static void debug(String s) {
        if (debug)
            System.out.println("BaseTaskDebug: " + Thread.currentThread().getName() + ":" + s);
    }

    /** Each base task has a depth in the pedigree tree, the value of
    the next magic number, a unique gamma value that is the dot
    product of the task's pedigree and the magic numbers array, and a
    dot product that is the task's gamma value with extra magic added
    in for each random number the task generates **/

    private int depth;
    private int nextMagicNumber; 
    private int gamma; 
    private long dotProduct;
    
    // The constructor used by the primordial task.
    public BaseTask() {
        super();
        this.parent = null;
        this.transaction = null;

        // Pedigree information
        this.depth = 0;
        this.nextMagicNumber = MagicNumbers.a(this.depth + 1);
        this.gamma = MagicNumbers.a(this.depth);
        this.dotProduct = this.gamma;
    }

    public BaseTask(BaseTask parent) {
        super();
        this.parent = parent;
        this.transaction = parent.transaction;

        // Pedigree information
        this.depth = parent.getDepth() + 1;
        this.nextMagicNumber = MagicNumbers.a(this.depth + 1);
        this.gamma = parent.makeNewChild();
        this.dotProduct = this.gamma;
    }

    private int getDepth() {
        return this.depth;
    }

    /* Returns a unique gamma for a child task */
    public int makeNewChild() {
        this.gamma += this.nextMagicNumber;
        return this.gamma;
    }

    /* Records that a new random number has been asked for, and
     * returns the task's dotProduct */
    public long makeNewRandom() {
        this.dotProduct += this.nextMagicNumber;
        return dotProduct;
    }

    private static boolean unsafeWorthSpawning() {
        return (numThreads > 1 &&
                (spawnThreshold < 0 || getSurplusQueuedTaskCount() <= spawnThreshold));
    }

    public static boolean worthSpawning() {
        return numThreads > 1 && !ForkJoinTask.inForkJoinPool() || unsafeWorthSpawning();
    }

    // Corner case: we attempted to spawn work from a class
    // initializer.  That doesn't work as the initializer isn't run in
    // a ForkJoinWorkerThread.  So we must submit the task to the
    // global task pool instead.
    private final void executeAlways() {
        FortressExecutable.group.execute(this);
        actuallyForked = EXECUTED;
    }

    private final void forkAlways() {
        this.fork();
        actuallyForked = FORKED;
    }

    public final void forkIfProfitable() {
        if (ForkJoinTask.inForkJoinPool()) {
            if (unsafeWorthSpawning()) {
                this.forkAlways();
            }
        } else {
            this.executeAlways();
        }
    }

    public final void joinOrRun() {
        // Emphasize common UNFORKED case.
        if (actuallyForked == UNFORKED) {
            this.compute();
        } else if (useHelpJoin) {
            this.helpJoin();
        } else {
            this.join();
        }
    }

   public static void startTransaction() {
        FortressTaskRunner ftr = (FortressTaskRunner) Thread.currentThread();
        BaseTask currentTask = ftr.getTask();
        debug("startTransaction: ftr = " + ftr + " current task = " + currentTask);

        Transaction transaction = Transaction.TXBegin(currentTask.transaction());
        debug("Start transaction ftr = " + ftr + " current action = " + currentTask + " trans = " + transaction);
        currentTask.setTransaction(transaction);
    }

    public static void endTransaction() {
        FortressTaskRunner ftr = (FortressTaskRunner) Thread.currentThread();
        BaseTask currentTask = ftr.getTask();
        debug("endTransaction: ftr = " + ftr + " current task = " + currentTask);
        Transaction transaction = ftr.getTask().transaction();
        debug("End transaction ftr = " + ftr + " current action = " + currentTask + " trans = " + transaction);
        if (transaction != null) {
            transaction.TXCommit();
            currentTask.setTransaction(transaction.getParent());
        }
        ftr.recordEndTransaction();
    }
    
    public static void cleanupTransaction() {
        FortressTaskRunner ftr = (FortressTaskRunner) Thread.currentThread();
        BaseTask currentTask = ftr.getTask();
        Transaction transaction = ftr.getTask().transaction();
        debug("Cleanup transaction ftr = " + ftr + " current action = " + currentTask + " trans = " + transaction);
        if (transaction != null)
            currentTask.setTransaction(transaction.getParent());
        else
            currentTask.setTransaction(null);
    }

    public static void delayTransaction() {
        FortressTaskRunner ftr = (FortressTaskRunner) Thread.currentThread();
        long startTime = System.nanoTime();
        long elapsedTime = System.nanoTime() - startTime;

        if (useExponentialBackoff) {
            double r = gen.nextDouble() + 0.5; // Random number between 0.5 and 1.5
            long waitNanoseconds = (long) ((1L << (ftr.getRetries() + machineConstant)) * r);
            
            while (elapsedTime < waitNanoseconds) {
                elapsedTime = System.nanoTime() - startTime;
            }
        }
        
        ftr.incElapsedTime(elapsedTime);
        ftr.incRetries();
    }

    public static Transaction getCurrentTransaction() {
        FortressTaskRunner ftr = (FortressTaskRunner) Thread.currentThread();
        debug("getCurrentTransaction: ftr = " + ftr );
        if (ftr.getTask() != null)
            return ftr.getTask().transaction();
        else return null;
    }

    public static BaseTask getCurrentTask() {
        FortressTaskRunner ftr = (FortressTaskRunner) Thread.currentThread();
        debug("getCurrentTask: ftr = " + ftr );
        return ftr.getTask();
    }

    public static boolean inATransaction() {
        FortressTaskRunner ftr = (FortressTaskRunner) Thread.currentThread();
        debug("inATransaction: ftr = " + ftr + " task = " + ftr.getTask());

        if (ftr.getTask() != null) 
            if (ftr.getTask().transaction() != null)
                return true;
        return false;
    }

    public static void setTask(BaseTask t) {
        FortressTaskRunner ftr = (FortressTaskRunner) Thread.currentThread();
        debug("setTask: ftr = " + ftr + " task = " + t);
        ftr.setTask(t);
    }

    Transaction transaction() {
        return transaction;
    }

    public void setTransaction(Transaction t) {
        transaction = t;
    }    

    public void printStatistics() {
        FortressTaskRunnerGroup ftrg = (FortressTaskRunnerGroup) getPool();
        ftrg.printStatistics();
    }
}
