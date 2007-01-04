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

import com.sun.fortress.interpreter.useful.IterableOnce;
import com.sun.fortress.interpreter.useful.UnitIterable;


// / and unpasting_bind = unpasting_bind_rec node
// / and unpasting_bind_rec =
// / {
// / unpasting_bind_name : id;
// / unpasting_bind_dim : unpasting_dim;
// / }
// /
public class UnpastingBind extends Unpasting {
    Id name;

    Option<List<ExtentRange>> dim;

    public UnpastingBind(Span s, Id name, Option<List<ExtentRange>> dim) {
        super(s);
        this.name = name;
        this.dim = dim;
    }

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forUnpastingBind(this);
    }

    UnpastingBind(Span span) {
        super(span);
    }

    /**
     * @return Returns the dim.
     */
    public Option<List<ExtentRange>> getDim() {
        return dim;
    }

    /**
     * @return Returns the name.
     */
    public Id getName() {
        return name;
    }

    @Override
    public IterableOnce<String> stringNames() {
        return new UnitIterable<String>(getName().getName());
    }
}
