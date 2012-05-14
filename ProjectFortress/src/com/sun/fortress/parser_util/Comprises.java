/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

/*
 * Fortress comprises clause.
 * Fortress AST node local to the Rats! com.sun.fortress.interpreter.parser.
 */
package com.sun.fortress.parser_util;

import com.sun.fortress.nodes.NamedType;
import com.sun.fortress.nodes_util.Span;
import edu.rice.cs.plt.tuple.Option;

import java.util.List;

public class Comprises extends TraitClause {
    private Option<List<NamedType>> comprises = Option.none();
    private boolean ellipses = false;

    public Comprises(Span span, List<NamedType> comprises, boolean ellipses) {
        super(span);
        this.comprises = Option.some(comprises);
        this.ellipses = ellipses;
    }

    public Comprises(Span span, List<NamedType> comprises) {
        super(span);
        this.comprises = Option.some(comprises);
        this.ellipses = false;
    }

    public Option<List<NamedType>> getComprises() {
        return comprises;
    }

    public boolean hasEllipses() {
        return ellipses;
    }

    public String message() {
        return "comprises";
    }

    public Span span() {
        return span;
    }
}
