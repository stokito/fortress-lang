/********************************************************************************
 Copyright 2009 Sun Microsystems, Inc.,
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

package com.sun.fortress.runtimeSystem;

import jsr166y.ForkJoinPool;
import jsr166y.ForkJoinTask;
import jsr166y.RecursiveAction;

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
public abstract class BaseTask extends RecursiveAction {
    // Could get this by hacking ForkJoinTask's status field, but
    // not touching that for now as it's too changeable
    private static int UNFORKED = 0;
    private static int FORKED = 1;
    private static int EXECUTED = 2;
    private int actuallyForked = UNFORKED;

    private static boolean unsafeWorthSpawning() {
        return (getSurplusQueuedTaskCount() < 3);
    }

    public static boolean worthSpawning() {
        return !ForkJoinTask.inForkJoinPool() || unsafeWorthSpawning();
    }

    // The constructor used by all compiled instances (right now)
    public BaseTask() {
        super();
    }

    // Corner case: we attempted to spawn work from a class
    // initializer.  That doesn't work as the initializer isn't run in
    // a ForkJoinWorkerThread.  So we must submit the task to the
    // global task pool instead.
    private void executeAlways() {
        // RIGHT NOW THIS DOESN"T WORK AT ALL AND IS DISABLED.
        // However, the same stuff works from within FortressExecutable
        // to invoke the run() method on an executable component.
        // Why are these behaving differently?  The thread context is the
        // same in both cases!

        // FortressExecutable.group.execute(this);
        // actuallyForked = EXECUTED;
    }

    private void forkAlways() {
        this.fork();
        actuallyForked = FORKED;
    }

    public void forkIfProfitable() {
        if (ForkJoinTask.inForkJoinPool()) {
            if (unsafeWorthSpawning()) {
                this.forkAlways();
            }
        } else {
            this.executeAlways();
        }
    }

    public void joinOrRun() {
        // Written as an if chain to emphasize common UNFORKED case.
        if (actuallyForked == UNFORKED) {
            this.compute();
        } else if (actuallyForked == FORKED) {
            this.helpJoin();
        } else {
            this.join();
        }
    }
}
