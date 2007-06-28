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

public class ArrayComprehensionClause extends AbstractNode {
    List<Expr> bind;

    Expr init;

    List<Generator> gens;

    public ArrayComprehensionClause(Span span, List<Expr> bind, Expr init,
            List<Generator> gens) {
        super(span);
        this.bind = bind;
        this.init = init;
        this.gens = gens;
    }

    @Override
    public <T> T acceptInner(NodeVisitor<T> v) {
        return v.forArrayComprehensionClause(this);
    }

    ArrayComprehensionClause(Span span) {
        super(span);
    }

    /**
     * @return Returns the bind.
     */
    public List<Expr> getBind() {
        return bind;
    }

    /**
     * @return Returns the gens.
     */
    public List<Generator> getGens() {
        return gens;
    }

    /**
     * @return Returns the init.
     */
    public Expr getInit() {
        return init;
    }
}
