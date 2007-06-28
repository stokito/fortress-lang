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

// / and entry = entry_rec node
// / and entry_rec =
// / {
// / entry_key : expr;
// / entry_value : expr;
// / }
// /
public class Entry extends AbstractNode {
    Expr key;

    Expr value;

    public Entry(Span span, Expr key, Expr value) {
        super(span);
        this.key = key;
        this.value = value;
    }

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forEntry(this);
    }

    Entry(Span span) {
        super(span);
    }

    /**
     * @return Returns the key.
     */
    public Expr getKey() {
        return key;
    }

    /**
     * @return Returns the value.
     */
    public Expr getValue() {
        return value;
    }
}
