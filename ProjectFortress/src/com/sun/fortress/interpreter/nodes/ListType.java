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

public class ListType extends TypeRef {

    TypeRef element;

    public ListType(Span span, TypeRef element) {
        super(span);
        this.element = element;
    }

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forListType(this);
    }

    ListType(Span span) {
        super(span);
    }

    /**
     * @return Returns the element.
     */
    public TypeRef getElement() {
        return element;
    }

    @Override
    int subtypeCompareTo(TypeRef o) {
        ListType x = (ListType) o;
        return element.compareTo(((ListType) o).element);
    }
}
