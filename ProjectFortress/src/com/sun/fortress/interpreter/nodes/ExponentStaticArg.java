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

// / and exponent_type = exponent_type_rec node
// / and exponent_type_rec =
// / {
// / exponent_type_base : type;
// / exponent_type_power : nat_type;
// / }
// /
public class ExponentStaticArg extends StaticArg {

    TypeRef base;

    TypeRef power;

    public ExponentStaticArg(Span span, TypeRef base, IntLiteral power) {
        super(span);
        this.base = base;
        this.power = new BaseNatRef(span, power);
    }

    public ExponentStaticArg(Span span, TypeRef base, TypeRef power) {
        super(span);
        this.base = base;
        this.power = power;
    }

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forExponentStaticArg(this);
    }

    ExponentStaticArg(Span span) {
        super(span);
    }

    /**
     * @return Returns the base.
     */
    public TypeRef getBase() {
        return base;
    }

    /**
     * @return Returns the power.
     */
    public TypeRef getPower() {
        return power;
    }
}
