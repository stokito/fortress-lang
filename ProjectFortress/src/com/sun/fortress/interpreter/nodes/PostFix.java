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
import com.sun.fortress.interpreter.useful.MagicNumbers;

public class PostFix extends OprName {
    Op op;

    public PostFix(Span span, Op op) {
        super(span);
        this.op = op;
    }

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forPostFix(this);
    }

    PostFix(Span span) {
        super(span);
    }

    /**
     * @return Returns the op.
     */
    public Op getOp() {
        return op;
    }

    public boolean equals(Object o) {
        PostFix p = (PostFix) o;
        return NodeUtil.getName(p).equals(NodeUtil.getName(this));
    }

    public int hashCode() {
        return NodeUtil.getName(this).hashCode() * MagicNumbers.x;
    }
}
