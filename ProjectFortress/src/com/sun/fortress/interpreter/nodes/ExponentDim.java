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

import com.sun.fortress.interpreter.nodes_util.*;
import com.sun.fortress.interpreter.useful.Useful;

// / and exponent_dim_type = exponent_dim_type_rec node
// / and exponent_dim_type_rec =
// / {
// / exponent_dim_type_base : dim_type;
// / exponent_dim_type_power : nat_type;
// / }
// /
public class ExponentDim extends DimType {

    DimType base;

    NatRef power;

    public ExponentDim(Span span, DimType base, NatRef power) {
        super(span);
        this.base = base;
        this.power = power;
    }

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forExponentDim(this);
    }

    ExponentDim(Span span) {
        super(span);
    }

    /**
     * @return Returns the base.
     */
    public DimType getBase() {
        return base;
    }

    /**
     * @return Returns the power.
     */
    public NatRef getPower() {
        return power;
    }

    @Override
    int subtypeCompareTo(TypeRef o) {
        ExponentDim x = (ExponentDim) o;
        return NodeComparator.compare((TypeRef) power, x.power, (TypeRef) base, x.base); // casts
                                                                                    // for
                                                                                    // generics
    }
}
