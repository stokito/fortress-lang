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

// / and accumulator_expr = accumulator_expr_rec node
// / and accumulator_expr_rec =
// / {
// / accumulator_expr_op : op;
// / accumulator_expr_gens : generator list;
// / accumulator_expr_body : expr;
// / }
// /
public class Accumulator extends FlowExpr {

    Op op;

    List<Generator> gens;

    Expr body;

    public Accumulator(Span span, Op op, List<Generator> gens, Expr body) {
        super(span);
        this.op = op;
        this.gens = gens;
        this.body = body;
    }

    @Override
    public <T> T acceptInner(NodeVisitor<T> v) {
        return v.forAccumulator(this);
    }

    Accumulator(Span span) {
        super(span);
    }

    /**
     * @return Returns the body.
     */
    public Expr getBody() {
        return body;
    }

    /**
     * @return Returns the gens.
     */
    public List<Generator> getGens() {
        return gens;
    }

    /**
     * @return Returns the op.
     */
    public Op getOp() {
        return op;
    }
}
