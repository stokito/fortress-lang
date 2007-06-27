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
import com.sun.fortress.interpreter.useful.IterableOnce;
import com.sun.fortress.interpreter.useful.UnitIterable;

public class AbsExternalSyntax extends Node implements Decl, AbsDecl {
    Name openExpander;

    Id id;

    Name closeExpander;

    public AbsExternalSyntax(Span span, Name openExpander, Id id,
            Name closeExpander) {
        super(span);
        this.openExpander = openExpander;
        this.id = id;
        this.closeExpander = closeExpander;
    }

    @Override
    public <T> T acceptInner(NodeVisitor<T> v) {
        return v.forAbsExternalSyntax(this);
    }

    AbsExternalSyntax(Span span) {
        super(span);
    }

    /**
     * @return Returns the open exnpander.
     */
    public Name getOpenExpander() {
        return openExpander;
    }

    /**
     * @return Returns the id.
     */
    public Id getId() {
        return id;
    }

    /**
     * @return Returns the close exnpander.
     */
    public Name getCloseExpander() {
        return closeExpander;
    }

    public IterableOnce<String> stringNames() {
        return new UnitIterable<String>(id.getName());
    }

}
