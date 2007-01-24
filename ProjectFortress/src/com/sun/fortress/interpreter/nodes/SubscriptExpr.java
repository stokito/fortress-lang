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

// / and subscript_expr = subscript_expr_rec node
// / and subscript_expr_rec =
// / {
// / subscript_expr_obj : expr;
// / subscript_expr_subs : expr list;
// / }
// /
public class SubscriptExpr extends Expr implements LHS {
    Expr obj;

    List<Expr> subs;

    public SubscriptExpr(Span span, Expr obj, List<Expr> subs) {
        super(span);
        this.obj = obj;
        this.subs = subs;
    }

    @Override
    public <T> T acceptInner(NodeVisitor<T> v) {
        return v.forSubscriptExpr(this);
    }

    SubscriptExpr(Span span) {
        super(span);
    }

    /**
     * @return Returns the obj.
     */
    public Expr getObj() {
        return obj;
    }

    /**
     * @return Returns the subs.
     */
    public List<Expr> getSubs() {
        return subs;
    }
}
