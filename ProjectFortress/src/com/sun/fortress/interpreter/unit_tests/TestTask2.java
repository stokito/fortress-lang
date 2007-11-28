/*******************************************************************************
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
 ******************************************************************************/

package com.sun.fortress.interpreter.unit_tests;

import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.sun.fortress.interpreter.evaluator.transactions.*;
import com.sun.fortress.interpreter.evaluator.tasks.*;
import com.sun.fortress.useful.TcWrapper;

public class TestTask2 extends BaseTask {
    ReadSet rs;
    int count;
    
    TestTask2(ReadSet _rs, int c) {
        rs = _rs;
        count = c;
    }
    
    public void print() {
        System.out.println("TestTask");
    }
    
    public void compute() {
        FortressTaskRunner runner = (FortressTaskRunner) Thread.currentThread();
        runner.setCurrentTask(this);
        for (int i = 0 ; i < count; i++) {
            rs.add(new Transaction());
        }
        
    }
}
