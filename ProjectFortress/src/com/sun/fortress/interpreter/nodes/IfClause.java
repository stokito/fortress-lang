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

// /
// / and if_clause = if_clause_rec node
// / and if_clause_rec =
// / {
// / if_clause_test : expr;
// / if_clause_body : expr;
// / }
// /
public class IfClause extends Node {

    Expr test;

    Expr body;

    public IfClause(Span span, Expr test, Expr body) {
        super(span);
        this.test = test;
        this.body = body;
    }

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forIfClause(this);
    }

    IfClause(Span span) {
        super(span);
    }

    /**
     * @return Returns the body.
     */
    public Expr getBody() {
        return body;
    }

    /**
     * @return Returns the test.
     */
    public Expr getTest() {
        return test;
    }
}
