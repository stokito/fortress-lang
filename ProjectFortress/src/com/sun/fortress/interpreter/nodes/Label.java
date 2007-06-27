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
 * Labeled expressions are parsed into elements of this class.
 * 
 * DelimitedExpr ::= label Id BlockElems end Id 
 * FlowExpr ::= exit Id? ( with Expr)? 
 * 
 * Simple example:
 * label I95 
 *   if goingTo (Sun) 
 *   then exit I95 with x32B 
 * end
 */
public class Label extends FlowExpr {
    Id name;

    Expr body;

    public Label(Span span, Id name, Expr body) {
        super(span);
        this.name = name;
        this.body = body;
    }

    @Override
    public <T> T acceptInner(NodeVisitor<T> v) {
        return v.forLabel(this);
    }

    Label(Span span) {
        super(span);
    }

    /**
     * @return Returns the body.
     */
    public Expr getBody() {
        return body;
    }

    /**
     * @return Returns the name.
     */
    public Id getName() {
        return name;
    }
}
