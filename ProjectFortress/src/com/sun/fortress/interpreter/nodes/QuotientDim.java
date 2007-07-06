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

// / and quotient_dim_type = quotient_dim_type_rec node
// / and quotient_dim_type_rec =
// / {
// / quotient_dim_type_numerator : dim_type;
// / quotient_dim_type_denominator : dim_type;
// / }
// /
public class QuotientDim extends DimType {
    DimType numerator;

    DimType denominator;

    public QuotientDim(Span span, DimType numerator, DimType denominator) {
        super(span);
        this.numerator = numerator;
        this.denominator = denominator;
    }

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forQuotientDim(this);
    }

    QuotientDim(Span span) {
        super(span);
    }

    /**
     * @return Returns the denominator.
     */
    public DimType getDenominator() {
        return denominator;
    }

    /**
     * @return Returns the numerator.
     */
    public DimType getNumerator() {
        return numerator;
    }
}
