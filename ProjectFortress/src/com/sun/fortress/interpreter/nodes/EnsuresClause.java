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

import com.sun.fortress.interpreter.useful.Option;
import java.util.ArrayList;
import java.util.List;

// / and ensures_clause = ensures_clause_rec node
// / and ensures_clause_rec =
// / {
// / ensures_clause_pre : expr option;
// / ensures_clause_post : expr list;
// / }
// /
public class EnsuresClause extends Node {

    Option<Expr> pre;

    // Due to the Fortress syntax change,
    // the following field should have type Expr.
    List<Expr> post;

    public EnsuresClause(Span span, Expr post, Option<Expr> pre) {
        super(span);
        this.pre = pre;
        List<Expr> es = new ArrayList<Expr>();
        es.add(post);
        this.post = es;
    }

    @Override
    public <T> T acceptInner(NodeVisitor<T> v) {
        return v.forEnsuresClause(this);
    }

    EnsuresClause(Span span) {
        super(span);
    }

    /**
     * @return Returns the post.
     */
    public List<Expr> getPost() {
        return post;
    }

    /**
     * @return Returns the pre.
     */
    public Option<Expr> getPre() {
        return pre;
    }
}
