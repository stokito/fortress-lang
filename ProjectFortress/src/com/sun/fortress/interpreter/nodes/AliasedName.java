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
import com.sun.fortress.interpreter.useful.None;
import com.sun.fortress.interpreter.useful.Option;
import com.sun.fortress.interpreter.useful.Some;

public class AliasedName extends AbstractNode {

    FnName name;

    Option<FnName> alias;

    public AliasedName(Span span, FnName name, Option<FnName> alias) {
        super(span);
        this.name = name;
        this.alias = alias;
    }

    public AliasedName(Span span, Id id) {
        this(span, new Name(id.getSpan(), id), new None<FnName>());
    }

    public AliasedName(Span span, Id id, DottedId alias) {
        this(span, new Name(id.getSpan(), id), new Some<FnName>(alias));
    }

    public AliasedName(Span span, OprName op) {
        this(span, op, new None<FnName>());
    }

    public AliasedName(Span span, OprName op, OprName alias) {
        this(span, op, new Some<FnName>(alias));
    }

    public AliasedName(Span span) {
        super(span);
        // TODO Auto-generated constructor stub
    }

    public AliasedName(AbstractNode rewriteFrom) {
        super(rewriteFrom);
        // TODO Auto-generated constructor stub
    }

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        // TODO Auto-generated method stub
        return null;
    }

    public FnName getName() {
        return name;
    }

    public Option<FnName> getAlias() {
        return alias;
    }

}
