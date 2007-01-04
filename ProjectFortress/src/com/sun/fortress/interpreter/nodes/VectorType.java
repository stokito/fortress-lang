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

import com.sun.fortress.interpreter.useful.Useful;

// / and vector_type = vector_type_rec node
// / and vector_type_rec =
// / {
// / vector_type_element : type_ref;
// / vector_type_dimension : extent_range;
// / }
// /
public class VectorType extends TypeRef {
    TypeRef element;

    ExtentRange dim;

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forVectorType(this);
    }

    VectorType(Span span) {
        super(span);
    }

    /**
     * @return Returns the dim.
     */
    public ExtentRange getDim() {
        return dim;
    }

    /**
     * @return Returns the element.
     */
    public TypeRef getElement() {
        return element;
    }

    @Override
    int subtypeCompareTo(TypeRef o) {
        VectorType x = (VectorType) o;
        // TODO Don't I need to worry about reducing the fraction?
        return Useful.compare(element, x.element, dim, x.dim);
    }
}
