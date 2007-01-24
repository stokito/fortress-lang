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

// / type fn_name = fn_name_variant node
// /

// Note well; because this is a com.sun.fortress.interpreter.useful abstraction for the
// generalized names seen in Fortress, it will persist into
// more semantically aware parts of the system (i.e., into
// the interpreter, compiler, com.sun.fortress.interpreter.typechecker, etc).
public abstract class FnName extends Node implements Comparable<FnName> {
    /*
     * (non-Javadoc)
     *
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(FnName o) {
        Class tc = getClass();
        Class oc = o.getClass();
        if (tc != oc) {
            return tc.getName().compareTo(oc.getName());
        }
        return stringName().compareTo(o.stringName());
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.fortress.interpreter.nodes.Node#accept(com.sun.fortress.interpreter.nodes.NodeVisitor)
     */
    @Override
    public <T> T acceptInner(NodeVisitor<T> v) {
        // TODO Auto-generated method stub
        return null;
    }

    FnName(Span span) {
        super(span);
    }

    @Override
    final public int hashCode() {
        return mandatoryHashCode();
    }

    protected abstract int mandatoryHashCode();

    @Override
    final public boolean equals(Object o) {
        if (!(o instanceof FnName)) {
            return false;
        }
        if (!(getClass().equals(o.getClass()))) {
            return false;
        }
        return mandatoryEquals(o);
    }

    /**
     * precondition: o has the same class as the receiver.
     *
     * @param o
     * @return
     */
    protected abstract boolean mandatoryEquals(Object o);

    @Override
    public String stringName() {
        return name();
    }

    public abstract String name();

}
