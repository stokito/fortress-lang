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

import com.sun.fortress.interpreter.useful.Useful;

// / and quotient_dim_type = quotient_dim_type_rec node
// / and quotient_dim_type_rec =
// / {
// / quotient_dim_type_numerator : dim_type;
// / quotient_dim_type_denominator : dim_type;
// / }
// /
public class QuotientType extends StaticArg {
    TypeRef numerator;

    TypeRef denominator;

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forQuotientType(this);
    }

    public QuotientType(Span span, TypeRef numerator, TypeRef denominator) {
	super(span);
	this.numerator = numerator;
	this.denominator = denominator;
    }

    QuotientType(Span span) {
        super(span);
    }

    /**
     * @return Returns the denominator.
     */
    public TypeRef getDenominator() {
        return denominator;
    }

    /**
     * @return Returns the numerator.
     */
    public TypeRef getNumerator() {
        return numerator;
    }

    @Override
    int subtypeCompareTo(TypeRef o) {
        QuotientType x = (QuotientType) o;
        // TODO Don't I need to worry about reducing the fraction?
        return Useful.compare(numerator, x.numerator, denominator,
                x.denominator);
    }
}
