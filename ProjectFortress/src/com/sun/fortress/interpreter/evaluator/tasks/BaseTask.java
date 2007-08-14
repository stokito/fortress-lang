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

import java.io.IOException;

import jsr166y.forkjoin.*;
import com.sun.fortress.interpreter.evaluator.transactions.exceptions.AbortedException;
import com.sun.fortress.interpreter.evaluator.transactions.exceptions.GracefulException;
import com.sun.fortress.interpreter.evaluator.transactions.exceptions.PanicException;
import com.sun.fortress.interpreter.evaluator.transactions.exceptions.SnapshotException;
import java.util.concurrent.Callable;
import com.sun.fortress.interpreter.evaluator.ProgramError;

public abstract class BaseTask extends RecursiveAction {
    public boolean causedException;
    public Throwable err;
    public BaseTask currentTask;
    int transactionCount;

    public void initTask() {
        setCurrentTask(this);
        transactionCount = 0;
    }

    public static <T> T doIt(Callable<T> xaction) {
        FortressTaskRunner taskrunner = (FortressTaskRunner) Thread.currentThread();
        T res = taskrunner.doIt(xaction);
        return res;
    }

    public void finalizeTask() {
        setCurrentTask(parent);
        if (parent != null) {
            if (causedException) {
                parent.causedException = true;
                parent.err = err;
     }
        }
    }

    public BaseTask(BaseTask parent) {
        causedException = false;
        setParentTask(parent);
    }

    public boolean causedException() {return causedException;}
    public Throwable getTaskException() {return err;}

    public static BaseTask getCurrentTask() {
        FortressTaskRunner taskrunner = (FortressTaskRunner) Thread.currentThread();
        return (BaseTask) taskrunner.getCurrentTask();
    }

    public static void setCurrentTask(BaseTask task) {
        FortressTaskRunner taskrunner = (FortressTaskRunner) Thread.currentThread();
        taskrunner.setCurrentTask(task);
    }

    BaseTask parent;
    public BaseTask getParentTask() { return parent;}
    public void setParentTask(BaseTask task) { parent = task;}
    
    
    public abstract void print();
    
    public static void printTaskTrace() {
        BaseTask currentTask = getCurrentTask();
        while (currentTask != null) {
            currentTask.print();
            currentTask = currentTask.getParentTask();
        }
    }
    
    Object tag;
    // Finds the current task and tags it
    public static void tagCurrentTask(Object obj) { 
        BaseTask currentTask = getCurrentTask();
        currentTask.setTag(obj);
    }
    
    // Get the tag from the current task
    public static Object  getCurrentTag() { 
        BaseTask currentTask = getCurrentTask();
        return currentTask.getTag();
    }
    
    public void setTag(Object obj) { tag = obj;}
    public Object getTag() { return tag;}
}


