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

import com.sun.fortress.interpreter.useful.IterableOnce;
import com.sun.fortress.interpreter.useful.UnitIterable;

public class Dimension extends Node implements Decl {
    Id id;

    Option<TypeRef> derived;

    Option<TypeRef> default_;

    @Override
    public <T> T acceptInner(NodeVisitor<T> v) {
        return v.forDimension(this);
    }

    Dimension(Span span) {
        super(span);
    }

    /**
     * @return Returns the id.
     */
    public Id getId() {
        return id;
    }

    @Override
    public String stringName() {
        return id.getName();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.fortress.interpreter.nodes.DefOrDecl#stringNames()
     */
    public IterableOnce<String> stringNames() {
        return new UnitIterable<String>(stringName());
    }

}
