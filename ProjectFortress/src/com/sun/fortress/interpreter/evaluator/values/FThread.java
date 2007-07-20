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
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.evaluator.tasks.BaseTask;
import com.sun.fortress.interpreter.evaluator.tasks.SpawnTask;
import com.sun.fortress.nodes.Expr;
import com.sun.fortress.interpreter.evaluator.Evaluator;

public class FThread extends FConstructedValue {
    private final SpawnTask st;


    public String toString() {return "FThread: task = " + st;}
    public String getString() {return "FThread: task = " + st;}

    public FThread(Expr b, Evaluator e) {
	st = new SpawnTask(b,e, BaseTask.getCurrentTask());
    }

    public FValue val() { return st.val();}
    public void f_wait() { st.st_wait();}
    public Boolean ready() { return st.ready();}
    public void stop() { st.cancel();}
}

