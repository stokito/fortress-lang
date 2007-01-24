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

// / and map_type = map_type_rec node
// / and map_type_rec =
// / {
// / map_type_key : type_ref;
// / map_type_value : type_ref;
// / }
// /
public class MapType extends TypeRef {

    TypeRef key;

    TypeRef value;

    public MapType(Span span, TypeRef key, TypeRef value) {
        super(span);
        this.key = key;
        this.value = value;
    }

    @Override
    public <T> T acceptInner(NodeVisitor<T> v) {
        return v.forMapType(this);
    }

    MapType(Span span) {
        super(span);
    }

    /**
     * @return Returns the key.
     */
    public TypeRef getKey() {
        return key;
    }

    /**
     * @return Returns the value.
     */
    public TypeRef getValue() {
        return value;
    }

    @Override
    int subtypeCompareTo(TypeRef o) {
        MapType x = (MapType) o;
        return Useful.compare(key, x.key, value, x.value);
    }
}
