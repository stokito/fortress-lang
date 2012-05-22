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

import java.util.concurrent.atomic.AtomicInteger;

import static com.sun.fortress.runtimeSystem.FortressExecutable.numThreads;
import static com.sun.fortress.runtimeSystem.FortressExecutable.spawnThreshold;
import static com.sun.fortress.runtimeSystem.FortressExecutable.useHelpJoin;

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

  // Debugging
    private static Boolean debug = false;
    private static AtomicInteger counter = new AtomicInteger();
    public int count;
    public int depth;
    public String name;


   private static void debug(String s) {
        if (debug)
            System.out.println("BaseTaskDebug: " + Thread.currentThread().getName() + ":" + s);
    }

    int depth() { return depth;}
    String name() {return name;}

    private static boolean unsafeWorthSpawning() {
        return (numThreads > 1 &&
                (spawnThreshold < 0 || getSurplusQueuedTaskCount() <= spawnThreshold));
    }

    public static boolean worthSpawning() {
        return numThreads > 1 && !ForkJoinTask.inForkJoinPool() || unsafeWorthSpawning();
    }

    // The constructor used by the primordial task.
    public BaseTask() {
        super();
        this.parent = null;
        this.transaction = null;
        this.depth = 0;
        this.count = counter.getAndIncrement();
        this.name = "" + this.count;
    }

    public BaseTask(BaseTask parent) {
        super();
        this.parent = parent;
        this.transaction = parent.transaction;
        this.depth = parent.depth + 1;
        this.count = counter.getAndIncrement();
        this.name = parent.name + "." + this.count;
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
}
