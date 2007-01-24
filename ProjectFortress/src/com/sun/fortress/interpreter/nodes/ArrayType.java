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

import java.util.Collections;

import com.sun.fortress.interpreter.useful.Useful;


// / and indexed_type = indexed_type_rec node
// / and indexed_type_rec =
// / {
// / indexed_type_element : type_ref;
// / indexed_type_indices : indices;
// / }
// /
public class ArrayType extends TypeRef {

    TypeRef element;

    Indices indices;

    public ArrayType(Span span, TypeRef element, Option<FixedDim> ind) {
        super(span);
        this.element = element;
        if (ind instanceof Some) {
            Some s = (Some) ind;
            this.indices = (FixedDim) s.getVal();
        } else {
            this.indices = new FixedDim(span, Collections
                    .<ExtentRange> emptyList());
        }
    }

    @Override
    public <T> T acceptInner(NodeVisitor<T> v) {
        return v.forArrayType(this);
    }

    ArrayType(Span span) {
        super(span);
    }

    /**
     * @return Returns the element.
     */
    public TypeRef getElement() {
        return element;
    }

    /**
     * @return Returns the indices.
     */
    public Indices getIndices() {
        return indices;
    }

    @Override
    int subtypeCompareTo(TypeRef o) {
        ArrayType a = (ArrayType) o;
        return Useful.compare(element, a.element, indices, a.indices);
    }
}
