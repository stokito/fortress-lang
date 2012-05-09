/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.glue;

import com.sun.fortress.interpreter.evaluator.values.FString;
import com.sun.fortress.interpreter.evaluator.values.FValue;


/**
 * Test the new native code infrastructure based on reflection.
 * Downside: We'll need one new .java file for each native function.
 * Is that really a good plan?
 */
public class TestFn extends NativeFn2 {
    public FValue applyToArgs(FValue x, FValue y) {
        return FString.make("testFn " + x.getString() + "," + y.getString());
    }
}
