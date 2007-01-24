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

// / and catch_expr = catch_expr_rec node
// / and catch_expr_rec =
// / {
// / catch_expr_name : id;
// / catch_expr_clauses : catch_clause list;
// / }
// /
public class CatchExpr extends Expr {

    Id name;

    List<CatchClause> clauses;

    public CatchExpr(Span span, Id name, List<CatchClause> clauses) {
        super(span);
        this.name = name;
        this.clauses = clauses;
    }

    @Override
    public <T> T acceptInner(NodeVisitor<T> v) {
        return v.forCatchExpr(this);
    }

    CatchExpr(Span span) {
        super(span);
    }

    /**
     * @return Returns the clauses.
     */
    public List<CatchClause> getClauses() {
        return clauses;
    }

    /**
     * @return Returns the name.
     */
    public Id getName() {
        return name;
    }
}
