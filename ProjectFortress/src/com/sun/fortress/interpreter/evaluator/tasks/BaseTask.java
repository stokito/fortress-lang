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
import com.sun.fortress.interpreter.evaluator.tasks.TaskState;

public abstract class BaseTask extends RecursiveAction {
    Throwable err = null;
    TaskState taskState = null;
    final private int depth;
    final private BaseTask parent;

    // Debugging
    private static Boolean debug = false;
    private static AtomicInteger counter = new AtomicInteger();
    private int count;
    String name;

    public BaseTask(BaseTask p) {
        parent = p;
        taskState = new TaskState(p.taskState());
        depth = p.depth() + 1;
        if (debug) {
            count = counter.getAndIncrement();
            name =  p.name() + "." + count;
        } else {
            name = "BaseTask";
            count = 0;
        }
    }

    public int depth() { return depth;}
    public int count() { return count;}
    public String name() {return name;}

    // For primordial evaluator task
    public BaseTask() {
        parent = null;
        depth = 0;
        count = 0;
        name = "0";
    }

    public BaseTask parent() { return parent;}

    public void recordException(Throwable t) {
        err = t;
    }

    public boolean causedException() {return err!=null;}
    public Throwable taskException() {return err;}

    public abstract void print();

    public static void printTaskTrace() {
        BaseTask task = FortressTaskRunner.getTask();
        while (task != null) {
            task.print();
            task = task.parent();
        }
    }

    // Jan made this lazy.  Revisit it FIXME
    public TaskState taskState() { return taskState; }

    public String toString() {
        return "[BaseTask" + name() + ":" + taskState() + "]" ;
    }

}
