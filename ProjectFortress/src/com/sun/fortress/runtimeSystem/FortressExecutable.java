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

package com.sun.fortress.runtimeSystem;

import jsr166y.*;

import com.sun.fortress.nativeHelpers.systemHelper;
import com.sun.fortress.compiler.runtimeValues.FVoid;
import com.sun.fortress.interpreter.evaluator.tasks.FortressTaskRunner;
import com.sun.fortress.interpreter.evaluator.tasks.FortressTaskRunnerGroup;

// Superclass of the generated component class.  We can't refer to that one until
// we have defined it and we need to pass an instance of it to the primordial task.
// We need a run method here because the one in the generated class isn't visisble yet.

public abstract class FortressExecutable extends RecursiveAction {
    public static int numThreads = getNumThreads();
    public static FortressTaskRunnerGroup group = new FortressTaskRunnerGroup(numThreads);

    static int getNumThreads() {
        String numThreadsString = System.getenv("FORTRESS_THREADS");
        if (numThreadsString != null)
            return Integer.parseInt(numThreadsString);
        else {
            int availThreads = Runtime.getRuntime().availableProcessors();
            if (availThreads <= 2)
                return availThreads;
            else
                return (int) Math.floor((double) availThreads/2.0);
        }
    }

    public abstract FVoid run();

    public final void runExecutable(String args[]) {
        systemHelper.registerArgs(args);
        group.invoke(this);
    }

    public final void compute() {
        this.run();
    }

}
