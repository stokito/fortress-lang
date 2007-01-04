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

import java.util.Collections;
import java.util.List;

// / and rect_comp_clause = rect_comp_clause_rec node
// / and rect_comp_clause_rec =
// / {
// / rect_comp_clause_bind : expr list;
// / rect_comp_clause_init : expr;
// / rect_comp_clause_guards : expr list;
// / rect_comp_clause_gens : generator list;
// / }
// /

public class RectCompClause extends Tree {
    // This field should be List<Id> instead of List<Expr>.
    // We need to fix this after replacing the OCaml com.sun.fortress.interpreter.parser with the Rats!
    // com.sun.fortress.interpreter.parser.
    List<Expr> bind;

    Expr init;

    List<Expr> guards;

    List<Generator> gens;

    public RectCompClause(Span span, List<Expr> bind, Expr init,
            List<Generator> gens) {
        super(span);
        this.bind = bind;
        this.init = init;
        this.guards = Collections.<Expr> emptyList();
        this.gens = gens;
    }

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forRectCompClause(this);
    }

    RectCompClause(Span span) {
        super(span);
    }

    /**
     * @return Returns the bind.
     */
    public List<Expr> getBind() {
        return bind;
    }

    /**
     * @return Returns the gens.
     */
    public List<Generator> getGens() {
        return gens;
    }

    /**
     * @return Returns the guards.
     */
    public List<Expr> getGuards() {
        return guards;
    }

    /**
     * @return Returns the init.
     */
    public Expr getInit() {
        return init;
    }
}
