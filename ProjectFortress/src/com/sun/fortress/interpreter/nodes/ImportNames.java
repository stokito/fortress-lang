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


public class ImportNames extends ImportFrom {
    List<AliasedName> names;

    public ImportNames(Span s, DottedId source, List<AliasedName> names) {
        super(s);
        this.source = source;
        this.names = names;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof ImportNames) {
            ImportNames in = (ImportNames) o;
            return source.equals(in.getSource()) && names.equals(in.getNames());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return MagicNumbers.hashList(names, MagicNumbers.N);
    }

    @Override
    public <T> T acceptInner(NodeVisitor<T> v) {
        return v.forImportNames(this);
    }

    ImportNames(Span span) {
        super(span);
    }

    /**
     * @return Returns the source.
     */
    public DottedId getSource() {
        return source;
    }

    /**
     * @return Returns the names.
     */
    public List<AliasedName> getNames() {
        return names;
    }
}
