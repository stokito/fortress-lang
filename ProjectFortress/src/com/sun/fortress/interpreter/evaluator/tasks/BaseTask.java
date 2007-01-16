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
import EDU.oswego.cs.dl.util.concurrent.FJTaskRunnerGroup;
import com.sun.fortress.interpreter.evaluator.Evaluator;

import dstm2.exceptions.AbortedException;
import dstm2.exceptions.GracefulException;
import dstm2.exceptions.PanicException;
import dstm2.exceptions.SnapshotException;
import dstm2.factory.AtomicFactory;
import dstm2.factory.Factory;
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
import dstm2.atomic;
import dstm2.AtomicArray;
import dstm2.ContentionManager;
import dstm2.manager.BackoffManager;
import dstm2.Thread;
import dstm2.Transaction;

import static dstm2.Defaults.*;

public abstract class BaseTask extends FJTask {
    Thread _thread;
    public boolean causedException;
    public Throwable err;

    public BaseTask() {
        _thread = new Thread();
        try {
            Class managerClass = Class.forName("dstm2.manager.BackoffManager");
            _thread.setContentionManagerClass(managerClass);
            _thread.setAdapterClass("dstm2.factory.ofree.Adapter");
        } catch (ClassNotFoundException ex) {
            System.out.println("UhOh Contention Manager not found");
            System.exit(0);
        }
    }
    public boolean causedException() {return causedException;}
    public Throwable getException() {return err;}

}

    
