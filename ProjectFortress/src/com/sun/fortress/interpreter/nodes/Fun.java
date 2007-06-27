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

// / type fn_name_variant =
// / [
// / | `Fun of id
// / | opr_name_variant
// / ]
// /
public class Fun extends FnName {
    /*
     * (non-Javadoc)
     *
     * @see com.sun.fortress.interpreter.nodes.FnName#mandatoryEquals(java.lang.Object)
     */
    @Override
    protected boolean mandatoryEquals(Object o) {
        Fun f = (Fun) o;
        return f.name_.equals(name_);

    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.fortress.interpreter.nodes.FnName#mandatoryHashCode()
     */
    @Override
    protected int mandatoryHashCode() {
        return name_.hashCode();
    }

    Id name_;

    @Override
    public <T> T acceptInner(NodeVisitor<T> v) {
        return v.forFun(this);
    }

    Fun(Span span) {
        super(span);
    }

    public Fun(Span span, String s) {
        super(span);
        name_ = new Id(span, s);
    }

    public Fun(Span span, Id id) {
        super(span);
        name_ = id;
    }

    /**
     * Call this only for names that have no location. (When/if this constructor
     * disappears, it will be because we have a better plan for those names, and
     * its disappearance will identify all those places that need updating).
     *
     * @param s
     */
    public Fun(String s) {
        super(new Span());
        name_ = new Id(span, s);
    }

    /**
     * @return Returns the name.
     */
    public Id getName() {
        return name_;
    }

    public @Override
    String name() {
        return name_.getName();
    }

    @Override
    public String toString() {
        return name();
    }
}
