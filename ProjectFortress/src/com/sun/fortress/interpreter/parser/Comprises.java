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
 * Fortress comprises clause.
 * Fortress AST node local to the Rats! com.sun.fortress.interpreter.parser.
 */
package com.sun.fortress.interpreter.parser;

import java.util.List;
import com.sun.fortress.nodes_util.Span;
import com.sun.fortress.useful.Option;
import com.sun.fortress.useful.Some;
import com.sun.fortress.useful.None;
import com.sun.fortress.nodes.TypeRef;

public class Comprises extends TraitClause {
    private Option<List<TypeRef>> comprises = None.<List<TypeRef>>make();

    public Comprises(Span span, List<TypeRef> comprises) {
        super(span);
        this.comprises = Some.<List<TypeRef>>make(comprises);
    }

    public Option<List<TypeRef>> getComprises() {
        return comprises;
    }

    public String message() {
        return "comprises";
    }

    public Span span() {
        return span;
    }
}
