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

import com.sun.fortress.interpreter.useful.IterableOnce;


// / and unpasting_split = unpasting_split_rec node
// / and unpasting_split_rec =
// / {
// / unpasting_split_elems : unpasting list;
// / unpasting_split_dim : unpasting_dim;
// / }
// /
public class UnpastingSplit extends Unpasting {
    List<Unpasting> elems;

    int dim;

    public UnpastingSplit(Span span, List<Unpasting> elems, int dim) {
        super(span);
        this.elems = elems;
        this.dim = dim;
    }

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forUnpastingSplit(this);
    }

    UnpastingSplit(Span span) {
        super(span);
    }

    /**
     * @return Returns the dim.
     */
    public int getDim() {
        return dim;
    }

    /**
     * @return Returns the elems.
     */
    public List<Unpasting> getElems() {
        return elems;
    }
}
