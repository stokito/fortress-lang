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
import java.util.List;

import com.sun.fortress.interpreter.drivers.Driver;
import com.sun.fortress.interpreter.drivers.ProjectProperties;
import com.sun.fortress.nodes.CompilationUnit;
import com.sun.fortress.shell.PathBasedRepository;
import com.sun.fortress.interpreter.evaluator.tasks.BaseTask;
import com.sun.fortress.interpreter.evaluator.values.FValue;

public class EvaluatorTask extends BaseTask {

    CompilationUnit p;

    boolean runTests = false;

    boolean woLibrary = false;

    List<String> args;
    
    FValue theResult;
    
    String functionToRun;

    public EvaluatorTask(CompilationUnit prog, boolean tests, boolean library,
            String toRun, List<String> args_) {
        p = prog;
        runTests = tests;
        woLibrary = library;
        args = args_;
        functionToRun = toRun;
    }

    public void print() {
        System.out.println("EvaluatorTask: Compilation Unit = " + p);
    }

    public void compute() {
        FortressTaskRunner runner = (FortressTaskRunner) Thread.currentThread();
        runner.setCurrentTask(this);
        try {
            theResult =  Driver.runProgramTask(p, runTests, woLibrary, args, functionToRun, new PathBasedRepository(ProjectProperties.SOURCE_PATH, ProjectProperties.SOURCE_PATH_NATIVE));
        }
        catch (IOException e) {
            causedException = true;
            err = e;
            //            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
    
    public FValue result() {
        return theResult;
    }
}
