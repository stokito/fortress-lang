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

// /
// / and if_expr = if_expr_rec node
// / and if_expr_rec =
// / {
// / if_expr_clauses : if_clause list;
// / if_expr_else : expr option;
// / }
public class If extends FlowExpr {

    List<IfClause> clauses;

    Option<Expr> else_;

    public If(Span span, List<IfClause> clauses, Option<Expr> else_) {
        super(span);
        this.clauses = clauses;
        this.else_ = else_;
    }

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forIf(this);
    }

    If(Span span) {
        super(span);
    }

    /**
     * @return Returns the clauses.
     */
    public List<IfClause> getClauses() {
        return clauses;
    }

    /**
     * @return Returns the else_.
     */
    public Option<Expr> getElse_() {
        return else_;
    }
}
