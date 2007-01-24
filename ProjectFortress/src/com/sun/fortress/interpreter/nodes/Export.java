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

import com.sun.fortress.interpreter.useful.MagicNumbers;
import com.sun.fortress.interpreter.useful.Useful;


// / and export = dotted_name
// /
public class Export extends Node {
    List<DottedId> names;

    public Export(Span s, DottedId name) {
        super(s);
        names = Useful.<DottedId> list(name);
    }

    public Export(Span s, List<DottedId> names) {
        super(s);
        this.names = names;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Export) {
            Export e = (Export) o;
            return names.equals(e.getNames());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return MagicNumbers.x
                * MagicNumbers.hashList(getNames(), MagicNumbers.N);
    }

    @Override
    public <T> T acceptInner(NodeVisitor<T> v) {
        return v.forExport(this);
    }

    Export(Span span) {
        super(span);
    }

    public List<DottedId> getNames() {
        return names;
    }
}
