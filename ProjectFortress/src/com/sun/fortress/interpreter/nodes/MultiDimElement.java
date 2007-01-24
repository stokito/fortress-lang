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

// / and multi_dim_expr =
// / [
// / | `MultiDimElement of expr
// / | `MultiDimRow of multi_dim_row
// / ] node
// /
public class MultiDimElement extends MultiDim {

    Expr element;

    public MultiDimElement(Span span, Expr element) {
        super(span);
        this.element = element;
    }

    @Override
    public <T> T acceptInner(NodeVisitor<T> v) {
        return v.forMultiDimElement(this);
    }

    MultiDimElement(Span span) {
        super(span);
    }

    /**
     * @return Returns the element.
     */
    public Expr getElement() {
        return element;
    }
}
