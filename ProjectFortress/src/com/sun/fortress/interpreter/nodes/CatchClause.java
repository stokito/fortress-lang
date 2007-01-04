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

// / and catch_clause = catch_clause_rec node
// / and catch_clause_rec =
// / {
// / catch_clause_match : type_ref;
// / catch_clause_body : expr;
// / }
// /
public class CatchClause extends Tree {

    TypeRef match;

    List<Expr> body;

    public CatchClause(Span span, TypeRef match, List<Expr> body) {
        super(span);
        this.match = match;
        this.body = body;
    }

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forCatchClause(this);
    }

    CatchClause(Span span) {
        super(span);
    }

    /**
     * @return Returns the body.
     */
    public List<Expr> getBody() {
        return body;
    }

    /**
     * @return Returns the match.
     */
    public TypeRef getMatch() {
        return match;
    }
}
