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

import java.util.List;

// / and where_extends = where_extends_rec node
// / and where_extends_rec =
// / {
// / where_extends_name : id;
// / where_extends_super : type_ref list;
// / }
// /
public class WhereExtends extends WhereClause {

    Id name;

    List<TypeRef> supers;

    public WhereExtends(Span s, Id name, List<TypeRef> supers) {
        super(s);
        this.name = name;
        this.supers = supers;
    }

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forWhereExtends(this);
    }

    WhereExtends(Span span) {
        super(span);
    }

    /**
     * @return Returns the name.
     */
    public Id getName() {
        return name;
    }

    /**
     * @return Returns the supers.
     */
    public List<TypeRef> getSupers() {
        return supers;
    }
}
