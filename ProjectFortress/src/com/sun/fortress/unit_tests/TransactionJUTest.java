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

package com.sun.fortress.unit_tests;

import com.sun.fortress.interpreter.unit_tests.*;
import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.sun.fortress.interpreter.evaluator.transactions.*;
import com.sun.fortress.interpreter.evaluator.tasks.*;
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
