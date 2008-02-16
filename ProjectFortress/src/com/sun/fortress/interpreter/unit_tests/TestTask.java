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

package com.sun.fortress.interpreter.unit_tests;

import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.sun.fortress.interpreter.evaluator.transactions.*;
import com.sun.fortress.interpreter.evaluator.tasks.*;
import com.sun.fortress.useful.TcWrapper;

public class TestTask extends BaseTask {
    public void compute() {
        // Verify that we don't add duplicates
        FortressTaskRunner runner = (FortressTaskRunner) Thread.currentThread();
        runner.setCurrentTask(this);
        Transaction fred = new Transaction();
        ReadSet rs1 = new ReadSet();
        for (int i = 0; i < 10; i++)
            rs1.add(fred);
        Assert.assertEquals(rs1.size(), 1);

        // Verify that multiple threads adding to a readset don't stomp on each other.
        ReadSet rs2 = new ReadSet();
        int taskcount = 8;
        int transcount = 256;

        TestTask2[] tasks = new TestTask2[taskcount];
        for (int i = 0; i < taskcount; i++) {
            tasks[i] = new TestTask2(rs2, transcount);
        }

        TestTask2.forkJoin(tasks);
        Assert.assertEquals(rs2.size(), taskcount * transcount);
    }

    public void print() {
        System.out.println("TestTask");
    }

}
