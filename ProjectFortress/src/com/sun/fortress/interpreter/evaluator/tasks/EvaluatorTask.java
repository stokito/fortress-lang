/********************************************************************************
    Copyright 2009 Sun Microsystems, Inc.,
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
import com.sun.fortress.repository.GraphRepository;
import com.sun.fortress.compiler.index.ComponentIndex;
import com.sun.fortress.interpreter.Driver;
import com.sun.fortress.nodes.CompilationUnit;
import com.sun.fortress.nodes.Component;
import com.sun.fortress.interpreter.evaluator.values.FValue;

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
            theResult =  Driver.runProgramTask(p, functionToRun,
                                               fortressRepository);
        } catch (IOException e) {
            recordException(e);
            //            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
        }
    }

    public FValue result() {
        return theResult;
    }
}
