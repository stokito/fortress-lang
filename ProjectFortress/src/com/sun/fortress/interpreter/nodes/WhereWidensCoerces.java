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

public class WhereWidensCoerces extends WhereClause {

    TypeRef first;

    TypeRef second;

    public WhereWidensCoerces(Span s, TypeRef first, TypeRef second) {
        super(s);
        this.first = first;
        this.second = second;
    }

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forWhereWidensCoerces(this);
    }

    WhereWidensCoerces(Span span) {
        super(span);
    }

    /**
     * @return Returns the first type.
     */
    public TypeRef getFirst() {
        return first;
    }

    /**
     * @return Returns the second type.
     */
    public TypeRef getSecond() {
        return second;
    }
}
