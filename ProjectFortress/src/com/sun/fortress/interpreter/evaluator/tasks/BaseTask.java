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
import dstm2.Thread;
import dstm2.exceptions.AbortedException;
import dstm2.exceptions.GracefulException;
import dstm2.exceptions.PanicException;
import dstm2.exceptions.SnapshotException;
import java.util.concurrent.Callable;
import com.sun.fortress.interpreter.evaluator.ProgramError;

public abstract class BaseTask extends FJTask {
    Thread _thread;
    public boolean causedException;
    public Throwable err;
    public BaseTask currentTask;
    int transactionCount;

    public void initTask() {
        setCurrentTask(this);
        transactionCount = 0;
    }

    public static <T> T doIt(Callable<T> xaction) {
       BaseTask task = getCurrentTask();
       T result = null;
       task.transactionCount++;
       if (task.transactionCount == 1) {
          result = Thread.doIt(xaction);
       } else {
	 try {
          result = xaction.call();
	 } catch (AbortedException d) {Thread.getTransaction().abort(); 
         } catch (SnapshotException s) {Thread.getTransaction().abort();
         } catch (Exception e) {
           e.printStackTrace();
           throw new ProgramError("Unhandled Exception " + e);
	 }
       }
       task.transactionCount--;
       return result;
    }

    public void abortTransaction() { Thread.getTransaction().abort();}

    public void finalizeTask() {
	setCurrentTask(parent);
    }

    public BaseTask(BaseTask parent) {
        _thread = new Thread();
        try {
            Class managerClass = Class.forName("dstm2.manager.BackoffManager");
            _thread.setContentionManagerClass(managerClass);
            _thread.setAdapterClass("dstm2.factory.ofree.Adapter");
        } catch (ClassNotFoundException ex) {
            System.out.println("UhOh Contention Manager not found");
            System.exit(0);
        }
        setParentTask(parent);
    }

    public boolean causedException() {return causedException;}
    public Throwable getException() {return err;}

    public static BaseTask getCurrentTask() {
	FortressTaskRunner taskrunner = (FortressTaskRunner) FJTask.getFJTaskRunner();
	return (BaseTask) taskrunner.getCurrentTask();
    }

    public static void setCurrentTask(BaseTask task) {
	FortressTaskRunner taskrunner = (FortressTaskRunner) FJTask.getFJTaskRunner();
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

    
