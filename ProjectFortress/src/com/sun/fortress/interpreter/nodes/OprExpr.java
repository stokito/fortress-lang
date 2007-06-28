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
import java.util.ArrayList;
import java.util.List;

// / and opr_expr = opr_expr_rec node
// / and opr_expr_rec =
// / {
// / opr_expr_op : opr_name;
// / opr_expr_args : expr list;
// / }
// /
public class OprExpr extends Expr {
    OprName op;

    List<Expr> args;

    public OprExpr(Span span, OprName op, List<Expr> args) {
        super(span);
        this.op = op;
        this.args = args;
    }

    public OprExpr(Span span, OprName op) {
        super(span);
        this.op = op;
        this.args = new ArrayList<Expr>();
    }

    public OprExpr(Span span, OprName op, Expr arg) {
        super(span);
        this.op = op;
	List<Expr> es = new ArrayList<Expr>();
	es.add(arg);
        this.args = es;
    }

    public OprExpr(Span span, OprName op, Expr first, Expr second) {
        super(span);
        this.op = op;
	List<Expr> es = new ArrayList<Expr>();
	es.add(first);
	es.add(second);
        this.args = es;
    }

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forOprExpr(this);
    }

    OprExpr(Span span) {
        super(span);
    }

    /**
     * @return Returns the args.
     */
    public List<Expr> getArgs() {
        return args;
    }

    /**
     * @return Returns the op.
     */
    public OprName getOp() {
        return op;
    }
}
