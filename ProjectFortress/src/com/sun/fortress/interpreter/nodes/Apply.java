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

// / and apply_expr = apply_expr_rec node
// / and apply_expr_rec =
// / {
// / apply_expr_fn : expr;
// / apply_expr_args : expr; (* TupleExpr for multiple args *)
// / }
// /
public class Apply extends Expr {
    Expr fn;

    Expr args; // multiple args as tuple

    public Apply(Span span, Expr fn, Expr args) {
        super(span);
        this.fn = fn;
        this.args = args;
    }

    @Override
    public <T> T acceptInner(NodeVisitor<T> v) {
        return v.forApply(this);
    }

    Apply(Span span) {
        super(span);
    }

    /**
     * @return Returns the args.
     */
    public Expr getArgs() {
        return args;
    }

    /**
     * @return Returns the fn.
     */
    public Expr getFn() {
        return fn;
    }
}
