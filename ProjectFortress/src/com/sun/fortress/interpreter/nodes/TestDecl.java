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

import com.sun.fortress.interpreter.useful.IterableOnce;
import com.sun.fortress.interpreter.useful.UnitIterable;


public class TestDecl extends Node implements Decl, AbsDecl {
    Id id;

    List<Generator> gens;

    Expr expr;

    public TestDecl(Span span, Id id, List<Generator> gens, Expr expr) {
        super(span);
        this.id = id;
        this.gens = gens;
        this.expr = expr;
    }

    @Override
    public <T> T acceptInner(NodeVisitor<T> v) {
        return v.forTestDecl(this);
    }

    TestDecl(Span span) {
        super(span);
    }

    /**
     * @return Returns the id.
     */
    public Id getId() {
        return id;
    }

    /**
     * @return Returns the list of generators.
     */
    public List<Generator> getGens() {
        return gens;
    }

    /**
     * @return Returns the body expression.
     */
    public Expr getExpr() {
        return expr;
    }

    public IterableOnce<String> stringNames() {
        return new UnitIterable<String>(id.getName());
    }

}
