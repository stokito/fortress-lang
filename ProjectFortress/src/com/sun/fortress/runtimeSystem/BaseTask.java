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
public abstract class BaseTask extends FortressAction {
    // Could get this by hacking ForkJoinTask's status field, but
    // not touching that for now as it's too changeable
    private static final int UNFORKED = 0;
    private static final int FORKED = 1;
    private static final int EXECUTED = 2;
    private int actuallyForked = UNFORKED;

    BaseTask parent;


  // Debugging
    private static Boolean debug = false;
    private static AtomicInteger counter = new AtomicInteger();
    private int count;
    public int depth;
    public String name;


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
        parent = null;
        depth = 0;
        name = "PrimordialTask";
        count = 0;
    }
        
    // The constructor used by all compiled instances (right now)
    public BaseTask(BaseTask p) {
        super();
        parent = p;
        transaction = p.transaction();
        depth = p.depth() + 1;
        if (debug) {
            count = counter.getAndIncrement();
            name = p.name() + "." + count;
        } else {
            name = "BaseTask";
            count = 0;
        }
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
}
