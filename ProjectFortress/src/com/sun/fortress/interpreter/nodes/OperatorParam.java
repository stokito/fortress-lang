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

public class OperatorParam extends StaticParam {
    Op op;

    public OperatorParam(Span s, Op op) {
        super(s);
        this.op = op;
    }

    @Override
    public <T> T acceptInner(NodeVisitor<T> v) {
        return v.forOperatorParam(this);
    }

    OperatorParam(Span span) {
        super(span);
    }

    /**
     * @param op
     *            The op to set.
     */
    public void setOp(Op op) {
        this.op = op;
    }

    @Override
    public String getName() {
        return op.getName();
    }

    @Override
    public String toString() {
        return "opr " + getName();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.fortress.interpreter.nodes.StaticParam#subtypeCompareTo(com.sun.fortress.interpreter.nodes.StaticParam)
     */
    @Override
    int subtypeCompareTo(StaticParam o) {
        return getName().compareTo(o.getName());
    }

}
