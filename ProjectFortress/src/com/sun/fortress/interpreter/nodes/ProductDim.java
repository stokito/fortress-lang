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

import com.sun.fortress.interpreter.nodes_util.Span;
import com.sun.fortress.interpreter.useful.Useful;

// / and product_dim_type = product_dim_type_rec node
// / and product_dim_type_rec =
// / {
// / product_dim_type_multiplier : dim_type;
// / product_dim_type_multiplicand : dim_type;
// / }
// /
public class ProductDim extends DimType {
    DimType multiplier;

    DimType multiplicand;

    public ProductDim(Span s, DimType multiplier, DimType multiplicand) {
        super(s);
        this.multiplier = multiplier;
        this.multiplicand = multiplicand;
    }

    @Override
    public <T> T acceptInner(NodeVisitor<T> v) {
        return v.forProductDim(this);
    }

    ProductDim(Span span) {
        super(span);
    }

    /**
     * @return Returns the multiplicand.
     */
    public DimType getMultiplicand() {
        return multiplicand;
    }

    /**
     * @return Returns the multiplier.
     */
    public DimType getMultiplier() {
        return multiplier;
    }

    @Override
    int subtypeCompareTo(TypeRef o) {
        ProductDim x = (ProductDim) o;
        return Useful.compare((TypeRef) multiplier, x.multiplier,
                (TypeRef) multiplicand, x.multiplicand); // cast for generics
    }
}
