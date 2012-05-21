/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

/*
 * Fortress trait clause: excludes, comprises, or where clause.
 * Fortress AST node local to the Rats! com.sun.fortress.interpreter.parser.
 */
package com.sun.fortress.parser_util;

import com.sun.fortress.nodes_util.Span;

public abstract class TraitClause {

    Span span;

    TraitClause(Span span) {
        this.span = span;
    }

    abstract public String message();

    abstract public Span span();
}
