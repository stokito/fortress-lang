/*******************************************************************************
 Copyright 2009 Sun Microsystems, Inc.,
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
 * Fortress comprises clause.
 * Fortress AST node local to the Rats! com.sun.fortress.interpreter.parser.
 */
package com.sun.fortress.parser_util;

import com.sun.fortress.nodes.BaseType;
import com.sun.fortress.nodes_util.Span;
import edu.rice.cs.plt.tuple.Option;

import java.util.List;

public class Comprises extends TraitClause {
    private Option<List<BaseType>> comprises = Option.none();
    private boolean ellipses = false;

    public Comprises(Span span, List<BaseType> comprises, boolean ellipses) {
        super(span);
        this.comprises = Option.some(comprises);
        this.ellipses = ellipses;
    }

    public Comprises(Span span, List<BaseType> comprises) {
        super(span);
        this.comprises = Option.some(comprises);
        this.ellipses = false;
    }

    public Option<List<BaseType>> getComprises() {
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
