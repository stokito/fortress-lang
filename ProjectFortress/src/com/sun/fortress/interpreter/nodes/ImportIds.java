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

import com.sun.fortress.interpreter.useful.MagicNumbers;

// / and import_ids = import_ids_rec node
// / and import_ids_rec =
// / {
// / import_ids_source : dotted_name;
// / import_ids_names : import_names;
// / }
// /
public class ImportIds extends Import {
    DottedId source;

    ImportFrom names;

    public ImportIds(Span s, DottedId source, ImportFrom names) {
        super(s);
        this.source = source;
        this.names = names;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof ImportIds) {
            ImportIds ii = (ImportIds) o;
            return source.equals(ii.getSource()) && names.equals(ii.getNames());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return MagicNumbers.i * source.hashCode() + names.hashCode();
    }

    @Override
    public <T> T acceptInner(NodeVisitor<T> v) {
        return v.forImportIds(this);
    }

    ImportIds(Span span) {
        super(span);
    }

    /**
     * @return Returns the names.
     */
    public ImportFrom getNames() {
        return names;
    }

    /**
     * @return Returns the source.
     */
    public DottedId getSource() {
        return source;
    }

}
