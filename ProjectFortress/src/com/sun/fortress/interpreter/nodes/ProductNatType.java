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

package com.sun.fortress.interpreter.nodes;

import java.util.List;

public class ProductNatType extends CompoundNatType {
    public ProductNatType(Span span, List<StaticArg> value) {
        super(span, value);
    }

    @Override
    public <T> T acceptInner(NodeVisitor<T> v) {
        return v.forProductNatType(this);
    }

    ProductNatType(Span span) {
        super(span);
    }

}
