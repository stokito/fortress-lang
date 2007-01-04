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

public class NatParam extends StaticParam {
    Id id;

    public NatParam(Span s, Id id) {
        super(s);
        this.id = id;
    }

    static public NatParam make(String s) {
        NatParam np = new NatParam(new Span());
        np.id = new Id(new Span(), s);
        return np;
    }

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forNatParam(this);
    }

    NatParam(Span span) {
        super(span);
    }

    /**
     * @return Returns the id.
     */
    public Id getId() {
        return id;
    }

    @Override
    public String getName() {
        return id.getName();
    }

    @Override
    public String toString() {
        return "nat " + getName();
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
