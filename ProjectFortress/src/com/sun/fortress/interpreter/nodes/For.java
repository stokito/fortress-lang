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

// / and for_expr = for_expr_rec node
// / and for_expr_rec =
// / {
// / for_expr_gens : generator list;
// / for_expr_body : expr;
// / }
// /
public class For extends FlowExpr {

    List<Generator> gens;

    Expr body;

    public For(Span span, List<Generator> gens, Expr body) {
        super(span);
        this.gens = gens;
        this.body = body;
    }

    @Override
    public <T> T acceptInner(NodeVisitor<T> v) {
        return v.forFor(this);
    }

    For(Span span) {
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
}
