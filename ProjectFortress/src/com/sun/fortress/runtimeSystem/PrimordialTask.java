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
import java.util.concurrent.atomic.AtomicInteger;

import com.sun.fortress.exceptions.transactions.AbortedException;
import com.sun.fortress.exceptions.transactions.PanicException;
import com.sun.fortress.interpreter.evaluator.tasks.FortressTaskRunner;
import com.sun.fortress.interpreter.evaluator.tasks.FortressTaskRunnerGroup;

public class PrimordialTask extends RecursiveAction {
    FortressComponent fc;
    FortressTaskRunnerGroup group;

    static int getNumThreads() {
        int numThreads;

        String numThreadsString = System.getenv("FORTRESS_THREADS");
        if (numThreadsString != null)
            numThreads = Integer.parseInt(numThreadsString);
        else {
            int availThreads = Runtime.getRuntime().availableProcessors();
            if (availThreads <= 2)
                numThreads = availThreads;
            else
                numThreads = (int) Math.floor((double) availThreads/2.0);
        }
        return numThreads;
    }

    
    private PrimordialTask(FortressComponent fComponent) {
        //        System.out.println("Creating primordial task");
        fc = fComponent;
    }

    public void compute() {
        //        System.out.println("Primordial task compute " + fc.getClass() );
        fc.run();
    }

    public static PrimordialTask startFortress(FortressComponent fComponent) {
        //        System.out.println("Primordial task start fortress " + fComponent.getClass());
        PrimordialTask pt = new PrimordialTask(fComponent);
        pt.group = new FortressTaskRunnerGroup(getNumThreads());

        pt.group.invoke(pt);
        return pt;
    }
}

