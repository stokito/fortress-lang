/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.tests.unit_tests;

import com.sun.fortress.interpreter.evaluator.tasks.BaseTask;
import com.sun.fortress.interpreter.evaluator.tasks.FortressTaskRunner;
import com.sun.fortress.interpreter.evaluator.transactions.ReadSet;
import com.sun.fortress.interpreter.evaluator.transactions.Transaction;
import junit.framework.Assert;

public class TestTask extends BaseTask {
    public void compute() {
        // Verify that we don't add duplicates
        FortressTaskRunner runner = (FortressTaskRunner) Thread.currentThread();
        runner.setCurrentTask(this);
        Transaction fred = new Transaction();
        ReadSet rs1 = new ReadSet();
        for (int i = 0; i < 10; i++) {
            rs1.add(fred);
        }
        Assert.assertEquals(rs1.size(), 1);


        // Verify that multiple threads adding to a readset don't stomp on each other.
        ReadSet rs2 = new ReadSet();
        int taskcount = 8;
        int transcount = 256;

        TestTask2[] tasks = new TestTask2[taskcount];
        for (int i = 0; i < taskcount; i++) {
            tasks[i] = new TestTask2(rs2, transcount);
        }

        //        TestTask2.forkJoin(tasks);
        //        Assert.assertEquals(rs2.size(), taskcount * transcount);
    }

    public void print() {
        System.out.println("TestTask");
    }

}
