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

import com.sun.fortress.interpreter.useful.Pair;


// / and chain_expr = chain_expr_rec node
// / and chain_expr_rec =
// / {
// / chain_expr_first : expr;
// / chain_expr_links : (op * expr) list;
// / }
// /
public class ChainExpr extends Expr {

    Expr first;

    List<Pair<Op, Expr>> links;

    public ChainExpr(Span span, Expr first, List<Pair<Op, Expr>> links) {
        super(span);
        this.first = first;
        this.links = links;
    }

    @Override
    public <T> T acceptInner(NodeVisitor<T> v) {
        return v.forChainExpr(this);
    }

    ChainExpr(Span span) {
        super(span);
    }

    /**
     * @return Returns the first.
     */
    public Expr getFirst() {
        return first;
    }

    /**
     * @return Returns the links.
     */
    public List<Pair<Op, Expr>> getLinks() {
        return links;
    }
}
