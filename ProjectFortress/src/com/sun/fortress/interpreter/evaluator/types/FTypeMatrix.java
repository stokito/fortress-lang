/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.evaluator.types;

import com.sun.fortress.useful.Useful;

import java.util.List;

public class FTypeMatrix extends FAggregateType {
    final FType elementType;
    final List<TypeRange> bounds;

    public FTypeMatrix(FType element_type, List<TypeRange> bounds) {
        super("Matrix");
        this.elementType = element_type;
        this.bounds = bounds;
    }

    String lazyName;

    public String getName() {
        if (lazyName == null)
            lazyName = "Matrix " + Useful.inOxfords(elementType.getName()) + "^" + Useful.listInDelimiters("(",
                                                                                                           bounds,
                                                                                                           ")");
        return lazyName;
    }

    public String toString() {
        return getName();
    }

    public boolean subtypeOf(FType other) {
        if (!(other instanceof FTypeMatrix)) return super.subtypeOf(other);
        FTypeMatrix m = (FTypeMatrix) other;
        if (!elementType.subtypeOf(m.elementType)) return false;
        List<TypeRange> obounds = m.bounds;
        int sz = bounds.size();
        if (sz != obounds.size()) return false;
        for (int i = 0; i < sz; i++) {
            if (!(bounds.get(i).compatible(obounds.get(i)))) return false;
        }
        return true;
    }

    @Override
    public FType getElementType() {
        return elementType;
    }
}
