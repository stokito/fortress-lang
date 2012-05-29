/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.evaluator.values;

import java.util.List;


public interface Method {
    public FValue applyMethod(FObject selfValue, List<FValue> args);

    public FValue applyMethod(FObject selfValue);

    public FValue applyMethod(FObject selfValue, FValue a);

    public FValue applyMethod(FObject selfValue, FValue a, FValue b);

    public FValue applyMethod(FObject selfValue, FValue a, FValue b, FValue c);
}
