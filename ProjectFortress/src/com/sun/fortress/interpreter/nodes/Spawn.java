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

// / and spawn_expr = spawn_expr_rec node
// / and spawn_expr_rec =
// / {
// / spawn_expr_region : expr option;
// / spawn_expr_body : expr;
// / }
// /
public class Spawn extends FlowExpr {
    // This field should go away after replacing the OCaml com.sun.fortress.interpreter.parser with
    // the Rats! com.sun.fortress.interpreter.parser.
    Option<Expr> region = new None<Expr>();

    Expr body;

    public Spawn(Span span, Expr body) {
        super(span);
        this.body = body;
    }

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forSpawn(this);
    }

    Spawn(Span span) {
        super(span);
    }

    /**
     * @return Returns the body.
     */
    public Expr getBody() {
        return body;
    }

    /**
     * @return Returns the region.
     */
    public Option<Expr> getRegion() {
        return region;
    }
}
