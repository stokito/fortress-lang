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
 * Fortress excludes clause.
 * Fortress AST node local to the Rats! com.sun.fortress.interpreter.parser.
 */
package com.sun.fortress.parser_util;

import java.util.List;
import com.sun.fortress.nodes_util.Span;
import com.sun.fortress.nodes.TraitType;

public class Excludes extends TraitClause {
    private List<TraitType> excludes  = FortressUtil.emptyTraitTypes();

    public Excludes(Span span, List<TraitType> excludes) {
        super(span);
        this.excludes = excludes;
    }

    public List<TraitType> getExcludes() {
        return excludes;
    }

    public String message() {
        return "excludes";
    }

    public Span span() {
        return span;
    }
}
