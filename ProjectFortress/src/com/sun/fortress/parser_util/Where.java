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

/*
 * Fortress where clause.
 * Fortress AST node local to the Rats! com.sun.fortress.interpreter.parser.
 */
package com.sun.fortress.parser_util;

import java.util.List;
import edu.rice.cs.plt.tuple.Option;

import com.sun.fortress.nodes_util.Span;
import com.sun.fortress.nodes.WhereClause;

public class Where extends TraitClause {
    private Option<List<WhereClause>> where = Option.none();

    public Where(Span span, List<WhereClause> where) {
        super(span);
        if (where.size() == 0) { this.where = Option.none(); }
        else { this.where = Option.some(where); }
    }

    public Option<List<WhereClause>> getWhere() {
        return where;
    }

    public String message() {
        return "where";
    }

    public Span span() {
        return span;
    }
}
