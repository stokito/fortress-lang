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
import com.sun.fortress.interpreter.useful.MagicNumbers;

public class PostFix extends OprName {
    Op op;

    public PostFix(Span span, Op op) {
        super(span);
        this.op = op;
    }

    @Override
    public <T> T acceptInner(NodeVisitor<T> v) {
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

    public @Override
    String name() {
        return op.getName();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.fortress.interpreter.nodes.FnName#mandatoryEquals(java.lang.Object)
     */
    @Override
    protected boolean mandatoryEquals(Object o) {
        PostFix p = (PostFix) o;
        return p.name().equals(name());
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.fortress.interpreter.nodes.FnName#mandatoryHashCode()
     */
    @Override
    protected int mandatoryHashCode() {
        return name().hashCode() * MagicNumbers.x;
    }
}
