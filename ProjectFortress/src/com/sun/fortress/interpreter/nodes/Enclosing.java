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

public class Enclosing extends OprName {
    Op open;

    Op close;

    public Enclosing(Span span, Op open, Op close) {
        super(span);
        this.open = open;
        this.close = close;
    }

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forEnclosing(this);
    }

    Enclosing(Span span) {
        super(span);
    }

    /**
     * @return Returns the close.
     */
    public Op getClose() {
        return close;
    }

    /**
     * @return Returns the open.
     */
    public Op getOpen() {
        return open;
    }

    public @Override
    String name() {
        return open.getName();
    }

    String closingName() {
        return close.getName();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.fortress.interpreter.nodes.FnName#mandatoryEquals(java.lang.Object)
     */
    @Override
    protected boolean mandatoryEquals(Object o) {
        Enclosing e = (Enclosing) o;
        return e.name().equals(name()) && e.closingName().equals(closingName());
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.fortress.interpreter.nodes.FnName#mandatoryHashCode()
     */
    @Override
    protected int mandatoryHashCode() {
        return name().hashCode() * MagicNumbers.e
                ^ closingName().hashCode() * MagicNumbers.g;
    }
}
