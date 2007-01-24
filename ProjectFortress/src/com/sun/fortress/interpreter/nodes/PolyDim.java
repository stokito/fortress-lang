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

public class PolyDim extends Indices {
    private static int nextSeq = 1;

    private final int seq;

    @Override
    public <T> T acceptInner(NodeVisitor<T> v) {
        return v.forPolyDim(this);
    }

    PolyDim(Span span) {
        super(span);
        seq = nextSeq++;
    }

    @Override
    int subtypeCompareTo(Indices o) {
        return seq - ((PolyDim) o).seq;
    }
}
