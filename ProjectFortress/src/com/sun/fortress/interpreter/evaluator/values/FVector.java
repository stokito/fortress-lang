/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.evaluator.values;

import com.sun.fortress.interpreter.evaluator.types.FTypeVector;
import com.sun.fortress.interpreter.evaluator.types.TypeRange;

public class FVector extends FConstructedValue implements IndexedShape {
    final Indexed val;

    public FVector(Indexed v, FTypeVector ft) {
        super(new FTypeVector(v.elementType, TypeRange.make(v)));
        val = v;
    }

    public int size(int i) {
        return val.size(i);
    }

    public int dim() {
        return val.dim();
    }

    public void copyTo(IndexedTarget target, int[] toIndex, int dim) {
        val.copyTo(target, toIndex, dim);
    }

    public void copyTo(IndexedTarget target) {
        val.copyTo(target);
    }
}
