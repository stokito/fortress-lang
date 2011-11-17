/********************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ********************************************************************************/

package com.sun.fortress.interpreter.evaluator.tasks;

import com.sun.fortress.compiler.index.ComponentIndex;
import com.sun.fortress.interpreter.Driver;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.repository.GraphRepository;

import java.io.IOException;

public class EvaluatorTask extends BaseTask {

    final ComponentIndex p;

    final String functionToRun;

    final GraphRepository fortressRepository;

    FValue theResult;

    public EvaluatorTask(GraphRepository fr, ComponentIndex prog, String toRun) {
        super();
        p = prog;
        functionToRun = toRun;
        fortressRepository = fr;
    }

    public void print() {
        System.out.println("EvaluatorTask: Compilation Unit = " + p);
    }

    public void compute() {
        FortressTaskRunner.setCurrentTask(this);

        try {
            theResult = Driver.runProgramTask(p, functionToRun, fortressRepository);
        }
        catch (IOException e) {
            recordException(e);
            //            e.printStackTrace();
            throw new RuntimeException(e);
        }
        finally {
        }
    }

    public FValue result() {
        return theResult;
    }
}
