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
import com.sun.fortress.interpreter.useful.None;
import com.sun.fortress.interpreter.useful.Option;
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

    Option<Enclosing> op;

    public SubscriptExpr(Span span, Expr obj, List<Expr> subs,
                         Option<Enclosing> op) {
        super(span);
        this.obj = obj;
        this.subs = subs;
        this.op = op;
    }

    public SubscriptExpr(Span span, Expr obj, List<Expr> subs) {
        this(span, obj, subs, None.<Enclosing>make());
    }

    @Override
    public <T> T accept(NodeVisitor<T> v) {
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
