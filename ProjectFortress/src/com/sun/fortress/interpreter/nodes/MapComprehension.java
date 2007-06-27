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
import java.util.Collections;
import java.util.List;

// / and map_comp_expr = map_comp_expr_rec node
// / and map_comp_expr_rec =
// / {
// / map_comp_expr_key : expr;
// / map_comp_expr_value : expr;
// / map_comp_expr_guards : expr list;
// / map_comp_expr_gens : generator list;
// / }
// /
public class MapComprehension extends GeneratedComprehension {

    Expr key;

    Expr value;

    public MapComprehension(Span span, List<Generator> gens, Expr key,
            Expr value) {
        super(span);
        this.guards = Collections.<Expr> emptyList();
        this.gens = gens;
        this.key = key;
        this.value = value;
    }

    @Override
    public <T> T acceptInner(NodeVisitor<T> v) {
        return v.forMapComprehension(this);
    }

    MapComprehension(Span span) {
        super(span);
    }

    /**
     * @return Returns the key.
     */
    public Expr getKey() {
        return key;
    }

    /**
     * @return Returns the value.
     */
    public Expr getValue() {
        return value;
    }
}
