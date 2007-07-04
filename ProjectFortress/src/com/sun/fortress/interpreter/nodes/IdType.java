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

import com.sun.fortress.interpreter.nodes_util.*;
import com.sun.fortress.interpreter.useful.MagicNumbers;

public class IdType extends TypeRef {

    DottedId name;

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forIdType(this);
    }

    IdType(Span span) {
        super(span);
    }

    public IdType(Span span, DottedId dotted) {
        super(span);
        name = dotted;
    }

    public IdType(Span span, Id id) {
        super(span);
        name = NodeFactory.makeDottedId(span, id);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof IdType) {
            IdType it = (IdType) o;
            return name.equals(it.getName());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return name.hashCode() * MagicNumbers.y;
    }

    /**
     * @return Returns the name.
     */
    public DottedId getName() {
        return name;
    }

    @Override
    public String toString() {
        return name.toString();
    }

    @Override
    int subtypeCompareTo(TypeRef o) {
        //        return NodeComparator.compare(name, ((IdType) o).name);
        return name.compareTo(((IdType) o).name);
    }
}
