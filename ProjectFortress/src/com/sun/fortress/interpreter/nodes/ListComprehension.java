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

import java.util.Collections;
import java.util.List;

// / and list_comp_expr = list_comp_expr_rec node
// / and list_comp_expr_rec =
// / {
// / list_comp_expr_element : expr;
// / list_comp_expr_guards : expr list;
// / list_comp_expr_gens : generator list;
// / }
// /
public class ListComprehension extends GeneratedComprehension {
    Expr element;

    public ListComprehension(Span span, List<Generator> gens, Expr element) {
        super(span);
        this.guards = Collections.<Expr> emptyList();
        this.gens = gens;
        this.element = element;
    }

    @Override
    public <T> T acceptInner(NodeVisitor<T> v) {
        return v.forListComprehension(this);
    }

    ListComprehension(Span span) {
        super(span);
    }

    /**
     * @return Returns the element.
     */
    public Expr getElement() {
        return element;
    }
}
