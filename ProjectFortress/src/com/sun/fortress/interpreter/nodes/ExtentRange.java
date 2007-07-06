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

import com.sun.fortress.interpreter.nodes_util.*;
import com.sun.fortress.interpreter.useful.ListComparer;
import com.sun.fortress.interpreter.useful.Option;

// / and extent_range = extent_range_rec node
// / and extent_range_rec =
// / {
// / extent_range_base : nat_type option;
// / extent_range_size : nat_type option;
// / }
// /
public class ExtentRange extends AbstractNode {
    Option<TypeRef> base;

    Option<TypeRef> size;

    public ExtentRange(Span s, Option<TypeRef> base, Option<TypeRef> size) {
        super(s);
        this.base = base;
        this.size = size;
    }

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forExtentRange(this);
    }

    ExtentRange(Span span) {
        super(span);
    }

    /**
     * @return Returns the base.
     */
    public Option<TypeRef> getBase() {
        return base;
    }

    /**
     * @return Returns the size.
     */
    public Option<TypeRef> getSize() {
        return size;
    }
}
