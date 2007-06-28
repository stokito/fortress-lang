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
import java.util.List;

import com.sun.fortress.interpreter.useful.Useful;


// / and assignment_expr = assignment_expr_rec node
// / and assignment_expr_rec =
// / {
// / assignment_expr_lhs : expr;
// / assignment_expr_op : op option;
// / assignment_expr_rhs : expr;
// / }
// /
public class Assignment extends Expr {

    List<? extends LHS> lhs;

    Option<Op> op;

    Expr rhs;

    public Assignment(Span span, LHS lhs, Option<Op> op, Expr rhs) {
        super(span);
        this.lhs = Useful.list(lhs);
        this.op = op;
        this.rhs = rhs;
    }

    public Assignment(Span span, List<LHS> lhs, Option<Op> op, Expr rhs) {
        super(span);
        this.lhs = lhs;
        this.op = op;
        this.rhs = rhs;
    }

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forAssignment(this);
    }

    Assignment(Span span) {
        super(span);
    }

    /**
     * @return Returns the lhs.
     */
    public List<? extends LHS> getLhs() {
        return lhs;
    }

    /**
     * @return Returns the op.
     */
    public Option<Op> getOp() {
        return op;
    }

    /**
     * @return Returns the rhs.
     */
    public Expr getRhs() {
        return rhs;
    }
}
