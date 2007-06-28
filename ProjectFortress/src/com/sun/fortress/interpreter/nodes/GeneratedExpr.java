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
import java.util.List;

import com.sun.fortress.interpreter.useful.IterableOnce;
import com.sun.fortress.interpreter.useful.UnitIterable;


public class GeneratedExpr extends LetExpr {
    Expr expr;

    List<Generator> gens;

    public GeneratedExpr(Span span, Expr expr, List<Generator> gens,
            List<Expr> body) {
        super(span);
        this.expr = expr;
        this.gens = gens;
        this.body = body;
    }

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forGeneratedExpr(this);
    }

    GeneratedExpr(Span span) {
        super(span);
    }

    /**
     * @return Returns the expression.
     */
    public Expr getExpr() {
        return expr;
    }

    /**
     * @return Returns the generators.
     */
    public List<Generator> getGens() {
        return gens;
    }

    public IterableOnce<String> stringNames() {
        return new UnitIterable<String>("GeneratedExpr");
    }
}
