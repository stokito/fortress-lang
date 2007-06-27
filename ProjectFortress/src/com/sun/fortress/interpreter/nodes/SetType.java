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

public class SetType extends TypeRef {
    public SetType(Span s, TypeRef t) {
        super(s);
        this.elementType = t;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof SetType) {
            SetType rt = (SetType) o;
            return elementType.equals(rt.getElementType());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return elementType.hashCode() * MagicNumbers.s;
    }

    TypeRef elementType;

    @Override
    public <T> T acceptInner(NodeVisitor<T> v) {
        return v.forSetType(this);
    }

    SetType(Span span) {
        super(span);
    }

    /**
     * @return Returns the elementType.
     */
    public TypeRef getElementType() {
        return elementType;
    }

    @Override
    int subtypeCompareTo(TypeRef o) {
        return elementType.compareTo(((SetType) o).elementType);
    }
}
