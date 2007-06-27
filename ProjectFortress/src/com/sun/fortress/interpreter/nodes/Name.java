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
import com.sun.fortress.interpreter.useful.None;
import com.sun.fortress.interpreter.useful.Option;
import com.sun.fortress.interpreter.useful.Some;

public class Name extends FnName {

    Option<Id> id;

    Option<Op> op;

    public Name(Span span, Option<Id> id, Option<Op> op) {
        super(span);
        this.id = id;
        this.op = op;
    }

    public Name(Span span, Id id) {
        this(span, new Some<Id>(id), new None<Op>());
    }

    public Name(Span span, Op op) {
        this(span, new None<Id>(), new Some<Op>(op));
    }

    Name(Span span) {
        super(span);
    }

    @Override
    public <T> T acceptInner(NodeVisitor<T> v) {
        return v.forName(this);
    }

    /**
     * @return Returns the id.
     */
    public Option<Id> getId() {
        return id;
    }

    /**
     * @return Returns the op.
     */
    public Option<Op> getOp() {
        return op;
    }

    @Override
    public String name() {
        if (id instanceof Some) {
            return ((Id) ((Some) id).getVal()).getName();
        } else if (op instanceof Some) {
            return ((Op) ((Some) op).getVal()).getName();
        } else {
            throw new Error("Uninitialized Name.");
        }
    }

    @Override
    public boolean mandatoryEquals(Object o) {
        if (o instanceof Name) {
            Name n = (Name) o;
            return id.equals(n.getId()) && op.equals(n.getOp());
        }
        return false;
    }

    @Override
    public int mandatoryHashCode() {
        return MagicNumbers.e + id.hashCode() * MagicNumbers.a * op.hashCode()
                * MagicNumbers.w;
    }

    @Override
    public String toString() {
        if (id.isPresent()) {
            return id.toString();
        } else if (op.isPresent()) {
            return op.toString();
        } else {
            throw new Error("Uninitialized Name.");
        }
    }
}
