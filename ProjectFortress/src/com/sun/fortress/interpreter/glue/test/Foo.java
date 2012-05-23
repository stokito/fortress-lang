/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.glue.test;

import com.sun.fortress.interpreter.evaluator.values.FObject;
import com.sun.fortress.interpreter.evaluator.values.FString;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.interpreter.glue.NativeMeth1;

public class Foo extends NativeMeth1 {

    public FValue applyMethod(FObject selfValue, FValue s) {
        FValue x = selfValue.selectField("y");
        return FString.make(s.getString() + x.getString());
    }

}
