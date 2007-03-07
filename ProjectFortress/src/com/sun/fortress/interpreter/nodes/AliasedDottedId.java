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

public class AliasedDottedId extends Node {

    DottedId id;

    Option<DottedId> alias;

    public AliasedDottedId(Span span, DottedId id, Option<DottedId> alias) {
        super(span);
        this.id = id;
        this.alias = alias;
    }

    public AliasedDottedId(Span span, DottedId id) {
        this(span, id, new None<DottedId>());
    }

    public AliasedDottedId(Span span) {
        super(span);
        // TODO Auto-generated constructor stub
    }

    public AliasedDottedId(Node rewriteFrom) {
        super(rewriteFrom);
        // TODO Auto-generated constructor stub
    }
    
    public DottedId getId() { return id; }
    public Option<DottedId> getAlias() { return alias; }
    
    @Override
    public <T> T acceptInner(NodeVisitor<T> v) {
        // TODO Auto-generated method stub
        return null;
    }

}
