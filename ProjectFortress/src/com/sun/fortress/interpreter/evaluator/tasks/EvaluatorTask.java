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

import java.io.IOException;
import java.util.List;

import com.sun.fortress.repository.FortressRepository;
import com.sun.fortress.interpreter.Driver;
import com.sun.fortress.repository.ProjectProperties;
import com.sun.fortress.nodes.CompilationUnit;
import com.sun.fortress.interpreter.evaluator.values.FValue;

public class EvaluatorTask extends BaseTask {

    final CompilationUnit p;

    final boolean runTests;

    final List<String> args;

    final String functionToRun;

    final FortressRepository fortressRepository;

    FValue theResult;

    public EvaluatorTask(FortressRepository fr, CompilationUnit prog,
                         boolean tests, String toRun, List<String> args_) {
        super();
        p = prog;
        runTests = tests;
        args = args_;
        functionToRun = toRun;
        fortressRepository = fr;
    }

    public void print() {
        System.out.println("EvaluatorTask: Compilation Unit = " + p);
    }

    public void compute() {
        // The FortressTaskRunner isn't created yet when we first make our EvaluatorTask, so we need to initialize the thread state here.
        taskState = new TaskState();
        FortressTaskRunner.setCurrentTask(this);

        try {
            theResult =  Driver.runProgramTask(p, runTests, args, functionToRun,
                                               fortressRepository);
        } catch (IOException e) {
            recordException(e);
            //            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            fortressRepository.clear();
            taskState = null;
        }
    }

    public FValue result() {
        return theResult;
    }
}
