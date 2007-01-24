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

public class FixedDim extends Indices {

    List<ExtentRange> extents;

    public FixedDim(Span span, List<ExtentRange> extents) {
        super(span);
        this.extents = extents;
    }

    @Override
    public <T> T acceptInner(NodeVisitor<T> v) {
        return v.forFixedDim(this);
    }

    FixedDim(Span span) {
        super(span);
    }

    /**
     * @return Returns the extents.
     */
    public List<ExtentRange> getExtents() {
        return extents;
    }

    @Override
    int subtypeCompareTo(Indices o) {
        return ExtentRange.listComparer
                .compare(extents, ((FixedDim) o).extents);
    }

}
