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
import com.sun.fortress.interpreter.useful.MagicNumbers;

public class RestType extends TypeRef {
    public RestType(Span s, TypeRef t) {
        super(s);
        this.type = t;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof RestType) {
            RestType rt = (RestType) o;
            return type.equals(rt.getType());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return type.hashCode() * MagicNumbers.r;
    }

    TypeRef type;

    @Override
    public <T> T acceptInner(NodeVisitor<T> v) {
        return v.forRestType(this);
    }

    RestType(Span span) {
        super(span);
    }

    /**
     * @return Returns the type.
     */
    public TypeRef getType() {
        return type;
    }

    @Override
    public String toString() {
        return type.toString() + "...";
    }

    @Override
    int subtypeCompareTo(TypeRef o) {
        return type.compareTo(((RestType) o).type);
    }
}
