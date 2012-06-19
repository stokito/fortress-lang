/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.evaluator.values;

import java.util.List;

public class FTuple extends FTupleLike {

    public static FValue make(List<FValue> elems) {
        if (elems.size() <= 1) {
            if (elems.size() == 0) return FVoid.V;
            return elems.get(0);
        } else {
            return new FTuple(elems);
        }
    }

    protected FTuple() {
        super();
    }

    protected FTuple(List<FValue> elems) {
        super(elems);
    }

}
