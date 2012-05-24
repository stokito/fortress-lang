/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.evaluator;

import java.util.concurrent.locks.ReentrantLock;

public class InstantiationLock {
    static public final ReentrantLock L = new ReentrantLock();
    //static public String lastOverloadThrowable;
    //static public OverloadedFunction lastOverload;
}
