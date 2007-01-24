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

import com.sun.fortress.interpreter.useful.MagicNumbers;

public class SubscriptAssign extends OprName {
    public SubscriptAssign(Span span) {
        super(span);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.fortress.interpreter.nodes.FnName#mandatoryEquals(java.lang.Object)
     */
    @Override
    protected boolean mandatoryEquals(Object o) {
        SubscriptAssign sa = (SubscriptAssign) o;
        return sa.name().equals(name());
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.fortress.interpreter.nodes.FnName#mandatoryHashCode()
     */
    @Override
    protected int mandatoryHashCode() {
        return name().hashCode() * MagicNumbers.R;
    }

    @Override
    public <T> T acceptInner(NodeVisitor<T> v) {
        return v.forSubscriptAssign(this);
    }

    public @Override
    String name() {
        return "[]=";
    }
}
