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

import com.sun.fortress.interpreter.nodes_util.*;
import com.sun.fortress.interpreter.useful.Option;
import java.util.List;
import java.util.ArrayList;

import com.sun.fortress.interpreter.useful.IterableOnce;


// / and let_binding_expr = let_binding_expr_rec node
// / and let_binding_expr_rec =
// / {
// / let_binding_expr_lhs : lvalue list;
// / let_binding_expr_rhs : expr option;
// / }
// /
public class LocalVarDecl extends LetExpr {
    public LocalVarDecl(Span span) {
        super(span);
    }

    List<LValue> lhs;

    Option<Expr> rhs;

    /**
     * For backwards compatibility, this method allows specification of whether
     * its LValues are (all) mutable or not.
     *
     * @param span
     * @param lhs
     * @param rhs
     * @param body
     * @param mutable
     */
    public LocalVarDecl(Span span, List<LValue> lhs, Option<Expr> rhs,
            List<Expr> body, boolean mutable) {
        super(span);
        List<LValue> lvs = new ArrayList<LValue>();
        for (LValue l : lhs) {
            if (l instanceof LValueBind) {
                LValueBind lvb = (LValueBind) l;
                lvs.add(NodeFactory.makeLValue((LValueBind)l, mutable));
            }
        }
        this.lhs = lvs;
        this.rhs = rhs;
        this.body = body;
    }

    /**
     * The list of LValues ought to be correctly annotated w.r.t. mutability.
     *
     * @param span
     * @param lhs
     * @param rhs
     * @param body
     */
    public LocalVarDecl(Span span, List<LValue> lhs, Option<Expr> rhs,
            List<Expr> body) {
        super(span);
        this.lhs = lhs;
        this.rhs = rhs;
        this.body = body;
    }

    /**
     * @return Returns the lhs.
     */
    public List<LValue> getLhs() {
        return lhs;
    }

    /**
     * @return Returns the rhs.
     */
    public Option<Expr> getRhs() {
        return rhs;
    }

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        // TODO Auto-generated method stub
        return v.forLocalVarDecl(this);
    }
}
