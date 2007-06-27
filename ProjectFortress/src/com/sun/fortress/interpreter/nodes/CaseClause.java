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

// / and case_clause = case_clause_rec node
// / and case_clause_rec =
// / {
// / case_clause_match : expr;
// / case_clause_body : expr;
// / }
// /
public class CaseClause extends Node {

    Expr match;

    List<Expr> body;

    public CaseClause(Span span, Expr match, List<Expr> body) {
        super(span);
        this.match = match;
        this.body = body;
    }

    @Override
    public <T> T acceptInner(NodeVisitor<T> v) {
        return v.forCaseClause(this);
    }

    CaseClause(Span span) {
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
    public Expr getMatch() {
        return match;
    }
}
