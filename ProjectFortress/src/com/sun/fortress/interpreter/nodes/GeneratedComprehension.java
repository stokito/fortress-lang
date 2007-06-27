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

public abstract class GeneratedComprehension extends Comprehension {
    protected GeneratedComprehension(Span span) {
        super(span);
    }

    // Now, guards are also generators.
    // This field should go away after we replace the OCaml com.sun.fortress.interpreter.parser with
    // the Rats! com.sun.fortress.interpreter.parser.
    List<Expr> guards;

    List<Generator> gens;

    /**
     * @return Returns the gens.
     */
    public List<Generator> getGens() {
        return gens;
    }

    /**
     * @return Returns the guards.
     */
    public List<Expr> getGuards() {
        return guards;
    }
}

// / and comprehension_expr =
// / [
// / | `SetCompExpr of set_comp_expr
// / | `ListCompExpr of list_comp_expr
// / | `MapCompExpr of map_comp_expr
// / | `RectangularCompExpr of rect_comp_clause list
// / ] node
// /
