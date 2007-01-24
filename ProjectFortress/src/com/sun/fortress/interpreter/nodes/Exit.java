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

// / and exit_expr = exit_expr_rec node
// / and exit_expr_rec =
// / {
// / exit_expr_name : id option;
// / exit_expr_return : expr option;
// / }
// /
public class Exit extends FlowExpr {

    Option<Id> name;

    Option<Expr> return_;

    public Exit(Span span, Option<Id> name, Option<Expr> return_) {
        super(span);
        this.name = name;
        this.return_ = return_;
    }

    @Override
    public <T> T acceptInner(NodeVisitor<T> v) {
        return v.forExit(this);
    }

    Exit(Span span) {
        super(span);
    }

    /**
     * @return Returns the name.
     */
    public Option<Id> getName() {
        return name;
    }

    /**
     * @return Returns the return_.
     */
    public Option<Expr> getReturn_() {
        return return_;
    }
}
