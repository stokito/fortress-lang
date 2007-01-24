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

// / and type_apply_expr = type_apply_expr_rec node
// / and type_apply_expr_rec =
// / {
// / type_apply_expr_obj : expr;
// / type_apply_expr_args : type_arg list;
// / }
// /
public class TypeApply extends Expr {
    Expr expr;

    List<StaticArg> args;

    public TypeApply(Span span, Expr expr, List<StaticArg> args) {
        super(span);
        this.expr = expr;
        this.args = args;
    }

    @Override
    public <T> T acceptInner(NodeVisitor<T> v) {
        return v.forTypeApply(this);
    }

    TypeApply(Span span) {
        super(span);
    }

    /**
     * @return Returns the args.
     */
    public List<StaticArg> getArgs() {
        return args;
    }

    /**
     * @return Returns the expr.
     */
    public Expr getExpr() {
        return expr;
    }
}
