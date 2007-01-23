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

import java.util.ArrayList;
import java.util.List;

// / and matrix_type = matrix_type_rec node
// / and matrix_type_rec =
// / {
// / matrix_type_element : type_ref;
// / matrix_type_row_dim : extent_range;
// / matrix_type_col_dim : extent_range;
// / }
// /
public class MatrixType extends TypeRef {

    TypeRef element;

    List<ExtentRange> dimensions;

    public MatrixType(Span span, TypeRef element, ExtentRange rowDim,
            ExtentRange colDim) {
        super(span);
        this.element = element;
        List<ExtentRange> dims = new ArrayList<ExtentRange>();
        dims.add(rowDim);
        dims.add(colDim);
        this.dimensions = dims;
    }

    public MatrixType(Span span, TypeRef element, ExtentRange dimension,
            List<ExtentRange> dimensions) {
        super(span);
        this.element = element;
        List<ExtentRange> dims = new ArrayList<ExtentRange>();
        dims.add(dimension);
        dims.addAll(dimensions);
        this.dimensions = dims;
    }

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forMatrixType(this);
    }

    MatrixType(Span span) {
        super(span);
    }

    /**
     * @return Returns the dimensions.
     */
    public List<ExtentRange> getDimensions() {
        return dimensions;
    }

    /**
     * @return Returns the element.
     */
    public TypeRef getElement() {
        return element;
    }

    @Override
    int subtypeCompareTo(TypeRef o) {
        MatrixType x = (MatrixType) o;
        int y = element.compareTo(x.element);
        if (y != 0) return y;
        return ExtentRange.listComparer.compare(dimensions, x.dimensions);
    }

}
