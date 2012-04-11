/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.tests.unit_tests;

import com.sun.fortress.interpreter.evaluator.tasks.FortressTaskRunnerGroup;
import com.sun.fortress.useful.TestCaseWrapper;


public class TransactionJUTest extends TestCaseWrapper {
    public TransactionJUTest(String testName) {
        super(testName);
    }

    public TransactionJUTest() {
        super("TransactionTest");
    }

    public void testReadSet() {
        int numThreads = Runtime.getRuntime().availableProcessors();
        FortressTaskRunnerGroup group = new FortressTaskRunnerGroup(numThreads);
        TestTask task = new TestTask();
        group.invoke(task);
    }
}
