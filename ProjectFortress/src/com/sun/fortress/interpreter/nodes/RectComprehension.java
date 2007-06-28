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
import java.util.List;

public class RectComprehension extends Comprehension {
    List<ArrayComprehensionClause> clauses;

    public RectComprehension(Span span, List<ArrayComprehensionClause> clauses) {
        super(span);
        this.clauses = clauses;
    }

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forRectComprehension(this);
    }

    RectComprehension(Span span) {
        super(span);
    }

    /**
     * @return Returns the clauses.
     */
    public List<ArrayComprehensionClause> getClauses() {
        return clauses;
    }
}
