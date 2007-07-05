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
import com.sun.fortress.interpreter.useful.Option;
import java.util.Comparator;

import com.sun.fortress.interpreter.useful.AnyListComparer;
import com.sun.fortress.interpreter.useful.ListComparer;

public abstract class TypeRef extends AbstractNode {

    TypeRef(Span s) {
        super(s);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Comparable#compareTo(java.lang.Object)
    public int compareTo(TypeRef o) {
        Class tclass = getClass();
        Class oclass = o.getClass();
        if (oclass != tclass) {
            return tclass.getName().compareTo(oclass.getName());
        }
        return subtypeCompareTo(o);
    }
     */

    abstract int subtypeCompareTo(TypeRef o);
}

// / and type_ref =
// / [
// / | `VoidType
// / | `ArrowType of arrow_type
// / | `IdType of dotted_name
// / | `ParamType of param_type
// / | `TupleType of type_ref list
// / | `VectorType of vector_type
// / | `MatrixType of matrix_type
// / | `ArrayType of indexed_type
// / | `MapType of map_type
// / | `SetType of type_ref
// / | `DimType of dim_type
// / (* `RestType only appears in `TupleType or params, at the end of the
// list *)
// / | `RestType of type_ref
// / ] node
// /
