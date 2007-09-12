/*******************************************************************************
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
 ******************************************************************************/

package com.sun.fortress.interpreter.evaluator.values;

import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.interpreter.evaluator.Evaluator;
import com.sun.fortress.interpreter.evaluator.tasks.BaseTask;
import com.sun.fortress.interpreter.evaluator.tasks.FortressTaskRunner;
import com.sun.fortress.interpreter.evaluator.tasks.FortressTaskRunnerGroup;
import com.sun.fortress.interpreter.evaluator.tasks.SpawnTask;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.nodes.Expr;

import jsr166y.forkjoin.*;

public class FThread extends FConstructedValue {

    private final FortressTaskRunnerGroup group;
    private final SpawnTask st;

    public String toString() {return "FThread: task = " + st;}
    public String getString() {return "FThread: task = " + st;}

    public FThread(Expr b, Evaluator e) {
	
	int numThreads = Runtime.getRuntime().availableProcessors();
        String numThreadsString = System.getenv("NumFortressThreads");
        
	if (numThreadsString != null)
            numThreads = Integer.parseInt(numThreadsString);
	
	group = new FortressTaskRunnerGroup(numThreads);
        st = new SpawnTask(b,e);
	group.execute(st);
    }

    public FValue val() { return st.result();}
    public void f_wait() { st.waitForResult();}
    public Boolean ready() { return st.isDone();}
    public void stop() { st.cancel();}
}

