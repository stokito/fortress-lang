/*******************************************************************************
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
 ******************************************************************************/

package com.sun.fortress.interpreter.evaluator.tasks;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.interpreter.evaluator.Evaluator;
import com.sun.fortress.interpreter.evaluator.tasks.FortressTaskRunner;
import com.sun.fortress.interpreter.evaluator.tasks.FortressTaskRunnerGroup;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.interpreter.evaluator.values.SingleFcn;
import com.sun.fortress.nodes.Expr;
import com.sun.fortress.useful.HasAt;

import jsr166y.forkjoin.*;

public class SpawnTask extends BaseTask {

    SingleFcn fcn;

    Evaluator eval;

    FValue val;

    public void compute() {
        FortressTaskRunner runner = (FortressTaskRunner) Thread.currentThread();
        runner.setCurrentTask(this);
        List<FValue> args = new ArrayList<FValue>();
        HasAt loc = new HasAt.FromString("FRED");
        BetterEnv e = eval.e;
        val = fcn.apply(args, loc, e);
    }

    public SpawnTask(SingleFcn sf, Evaluator e) {
        fcn = sf;
        eval = e;
    }

    public void print() {
        System.out.println("Spawn Task: Function = " + fcn +
                           " eval = " + eval +
                           " val = " + val);
    }

    public FValue result() {
        while (!isDone());
        return val;
    }

    public void waitForResult() {
        while (!isDone());
    }
}
