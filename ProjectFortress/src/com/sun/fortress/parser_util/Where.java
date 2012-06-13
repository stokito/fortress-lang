/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

/*
 * Fortress where clause.
 * Fortress AST node local to the Rats! com.sun.fortress.interpreter.parser.
 */
package com.sun.fortress.parser_util;

import com.sun.fortress.nodes.WhereClause;
import com.sun.fortress.nodes_util.Span;
import edu.rice.cs.plt.tuple.Option;

public class Where extends TraitClause {
    private Option<WhereClause> where = Option.<WhereClause>none();

    public Where(Span span, Option<WhereClause> where) {
        super(span);
        this.where = where;
    }

    public Option<WhereClause> getWhere() {
        return where;
    }

    public String message() {
        return "where";
    }

    public Span span() {
        return span;
    }
}
