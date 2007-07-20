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

package com.sun.fortress.interpreter.evaluator.types;

import com.sun.fortress.useful.Useful;

public class FTypeVector extends FAggregateType {
    public FTypeVector() {
        super("Vector");

    }

    FType vector_type;

    TypeRange vector_dim;

    @Override
    public FType getElementType() {
        return vector_type;
    }

    String lazyName;

    public String getName() {
        if (lazyName == null)
            lazyName = "Vector "
                    + Useful.inOxfords(vector_type.getName(), vector_dim
                            .toString());
        return lazyName;
    }

}
