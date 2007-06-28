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

/**
 * A 'while' loop is parsed into an element of this class.
 * 
 * DelimitedExpr ::= while Expr Do 
 * 
 * Simple example:
 * 
 * while true do 
 *   x += 1
 * end
 */
public class While extends FlowExpr {

    Expr test;

    Expr body;

    public While(Span span, Expr test, Expr body) {
        super(span);
        this.test = test;
        this.body = body;
    }

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forWhile(this);
    }

    While(Span span) {
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
