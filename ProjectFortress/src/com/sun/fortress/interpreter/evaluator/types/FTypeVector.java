/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.evaluator.types;

import com.sun.fortress.useful.Useful;

public class FTypeVector extends FAggregateType {
    public FTypeVector(FType elt_type, TypeRange range) {
        super("Vector");
        vector_type = elt_type;
        vector_dim = range;
    }

    FType vector_type;

    TypeRange vector_dim;

    @Override
    public FType getElementType() {
        return vector_type;
    }

    String lazyName;

    public String getName() {
        if (lazyName == null) lazyName = "Vector " + Useful.inOxfords(vector_type.getName(), vector_dim.toString());
        return lazyName;
    }

}
