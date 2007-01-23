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

// / and unpasting_dim = unpasting_dim_rec node
// / and unpasting_dim_rec =
// / {
// / unpasting_dim_base : nat_type option;
// / unpasting_dim_size : nat_type;
// / }
// /
public class UnpastingDim extends Node {
    Option<NatRef> base;

    Option<NatRef> size;

    public UnpastingDim(Span span, Option<NatRef> base, Option<NatRef> size) {
        super(span);
        this.base = base;
        this.size = size;
    }

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forUnpastingDim(this);
    }

    UnpastingDim(Span span) {
        super(span);
    }

    /**
     * @return Returns the base.
     */
    public Option<NatRef> getBase() {
        return base;
    }

    /**
     * @return Returns the size.
     */
    public Option<NatRef> getSize() {
        return size;
    }
}
