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

import com.sun.fortress.interpreter.useful.MagicNumbers;


public class TupleExpr extends Expr {

    List<Expr> exprs;

    public TupleExpr(Span span, List<Expr> exprs) {
        super(span);
        if (exprs.size() == 0) {
            throw new Error("Empty tuples must be void");
        }
        this.exprs = exprs;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof TupleExpr) {
            TupleExpr te = (TupleExpr) o;
            return exprs.equals(te.getExprs());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return MagicNumbers.hashList(exprs, MagicNumbers.T);
    }

    @Override
    public <T> T acceptInner(NodeVisitor<T> v) {
        return v.forTupleExpr(this);
    }

    TupleExpr(Span span) {
        super(span);
    }

    /**
     * @return Returns the exprs.
     */
    public List<Expr> getExprs() {
        return exprs;
    }
}
