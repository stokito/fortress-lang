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

    
