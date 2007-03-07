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


public class ImportStar extends ImportFrom {

    List<Name> except;

    @Override
    public <T> T acceptInner(NodeVisitor<T> v) {
        return v.forImportStar(this);
    }

    /**
     * for reflective access.
     *
     * @param span
     */
    public ImportStar(Span span) {
        super(span);
    }

    public ImportStar(Span span, List<Name> except, DottedId source) {
        super(span);
        this.except = except;
        this.source = source;
    }

    public List<Name> getExcept() {
        return except;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ImportStar;
    }

    @Override
    public int hashCode() {
        return MagicNumbers.S;
    }
}
