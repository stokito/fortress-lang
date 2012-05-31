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

public class TestTask2 extends BaseTask {
    ReadSet rs;
    int count;

    public TestTask2(ReadSet _rs, int c) {
        rs = _rs;
        count = c;
    }

    public void print() {
        System.out.println("TestTask");
    }

    public void compute() {
        FortressTaskRunner runner = (FortressTaskRunner) Thread.currentThread();
        runner.setCurrentTask(this);
        for (int i = 0; i < count; i++) {
            rs.add(new Transaction());
        }

    }
}
