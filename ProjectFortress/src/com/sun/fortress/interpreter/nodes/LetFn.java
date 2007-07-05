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
import com.sun.fortress.interpreter.useful.MagicNumbers;
import com.sun.fortress.interpreter.useful.UnitIterable;


// / and let_fn_expr = let_fn_expr_rec node
// / and let_fn_expr_rec =
// / {
// / let_fn_expr_fns : fn_bind list;
// / }
// /
public class LetFn extends LetExpr {
    List<FnDecl> fns;

    public LetFn(Span span, List<FnDecl> fns, List<Expr> body) {
        super(span);
        this.fns = fns;
        this.body = body;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof LetFn) {
            LetFn lf = (LetFn) o;
            return fns.equals(lf.getFns()) && body.equals(lf.getBody());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return MagicNumbers.hashList(fns, MagicNumbers.f)
                + MagicNumbers.hashList(body, MagicNumbers.b);
    }

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forLetFn(this);
    }

    LetFn(Span span) {
        super(span);
    }

    /**
     * @return Returns the fns.
     */
    public List<FnDecl> getFns() {
        return fns;
    }
}
