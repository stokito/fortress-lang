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
import com.sun.fortress.interpreter.useful.Option;
import com.sun.fortress.interpreter.useful.Some;
import java.util.List;

// / and case_expr = case_expr_rec node
// / and case_expr_rec =
// / {
// / case_expr_param : [ `Expr of expr | `Largest | `Smallest ];
// / case_expr_compare : op option; (* None if `Largest / `Smallest *)
// / case_expr_clauses : case_clause list;
// / case_expr_else : expr option;
// / }
// /
public class CaseExpr extends FlowExpr {

    CaseParam param;

    Option<Op> compare;

    List<CaseClause> clauses;

    Option<List<Expr>> else_;

    public CaseExpr(Span span, CaseParam param, Option<Op> compare,
            List<CaseClause> clauses, List<Expr> else_) {
        super(span);
        this.param = param;
        this.compare = compare;
        this.clauses = clauses;
        this.else_ = new Some<List<Expr>>(else_);
    }

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forCaseExpr(this);
    }

    CaseExpr(Span span) {
        super(span);
    }

    /**
     * @return Returns the clauses.
     */
    public List<CaseClause> getClauses() {
        return clauses;
    }

    /**
     * @return Returns the compare.
     */
    public Option<Op> getCompare() {
        return compare;
    }

    /**
     * @return Returns the else_.
     */
    public Option<List<Expr>> getElse_() {
        return else_;
    }

    /**
     * @return Returns the param.
     */
    public CaseParam getParam() {
        return param;
    }
}
