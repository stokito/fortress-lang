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
import java.util.List;

// / and multi_dim_row = multi_dim_row_rec node
// / and multi_dim_row_rec =
// / {
// / multi_dim_row_elements : multi_dim_expr list;
// / multi_dim_row_dimension : int;
// / }
// /
public class MultiDimRow extends MultiDim {

    int dimension;

    List<MultiDim> elements;

    public MultiDimRow(Span span, int dimension, List<MultiDim> elements) {
        super(span);
        this.dimension = dimension;
        this.elements = elements;
    }

    @Override
    public <T> T acceptInner(NodeVisitor<T> v) {
        return v.forMultiDimRow(this);
    }

    MultiDimRow(Span span) {
        super(span);
    }

    /**
     * @return Returns the dimension.
     */
    public int getDimension() {
        return dimension;
    }

    /**
     * @return Returns the elements.
     */
    public List<MultiDim> getElements() {
        return elements;
    }
}
