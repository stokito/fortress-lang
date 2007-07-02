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

public class Opr extends OprName {
    Op op;

    public Opr(Span span, Op op) {
        super(span);
        this.op = op;
    }

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forOpr(this);
    }

    Opr(Span span) {
        super(span);
    }

    /**
     * @param op
     *            The op to set.
     */
    public void setOp(Op op) {
        this.op = op;
    }

    public Op getOp() {
        return op;
    }

    public String toString() {
        return op.getName();
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
        Opr opr = (Opr) o;
        return name().equals(opr.name());
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.fortress.interpreter.nodes.FnName#mandatoryHashCode()
     */
    @Override
    protected int mandatoryHashCode() {
        // TODO Auto-generated method stub
        return name().hashCode() * MagicNumbers.p;
    }
}
