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

// / and try_expr = try_expr_rec node
// / and try_expr_rec =
// / {
// / try_expr_body : expr;
// / try_expr_catch : catch_expr option;
// / try_expr_forbid : type_ref list;
// / try_expr_finally : expr option;
// / }
// /
public class Try extends FlowExpr {
    Expr body;

    Option<CatchExpr> catch_;

    List<TypeRef> forbid;

    Option<Expr> finally_;

    public Try(Span span, Expr body, Option<CatchExpr> catch_,
            List<TypeRef> forbid, Option<Expr> finally_) {
        super(span);
        this.body = body;
        this.catch_ = catch_;
        this.forbid = forbid;
        this.finally_ = finally_;
    }

    @Override
    public <T> T acceptInner(NodeVisitor<T> v) {
        return v.forTry(this);
    }

    Try(Span span) {
        super(span);
    }

    /**
     * @return Returns the body.
     */
    public Expr getBody() {
        return body;
    }

    /**
     * @return Returns the catch_.
     */
    public Option<CatchExpr> getCatch_() {
        return catch_;
    }

    /**
     * @return Returns the finally_.
     */
    public Option<Expr> getFinally_() {
        return finally_;
    }

    /**
     * @return Returns the forbid.
     */
    public List<TypeRef> getForbid() {
        return forbid;
    }
}
