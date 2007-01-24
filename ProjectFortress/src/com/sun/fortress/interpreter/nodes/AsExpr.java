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

public class AsExpr extends Expr {

    Expr expr;

    TypeRef type;

    public AsExpr(Span span, Expr expr, TypeRef type) {
        super(span);
        this.expr = expr;
        this.type = type;
    }

    @Override
    public <T> T acceptInner(NodeVisitor<T> v) {
        return v.forAsExpr(this);
    }

    AsExpr(Span span) {
        super(span);
    }

    /**
     * @return Returns the expr.
     */
    public Expr getExpr() {
        return expr;
    }

    /**
     * @return Returns the type.
     */
    public TypeRef getType() {
        return type;
    }
}
