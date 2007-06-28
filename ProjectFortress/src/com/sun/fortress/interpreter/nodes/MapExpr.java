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

import com.sun.fortress.interpreter.useful.Pair;


public class MapExpr extends Expr {

    List<Pair<Expr, Expr>> elements;

    public MapExpr(Span span, List<Pair<Expr, Expr>> elements) {
        super(span);
        this.elements = elements;
    }

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forMapExpr(this);
    }

    MapExpr(Span span) {
        super(span);
    }

    /**
     * @return Returns the elements.
     */
    public List<Pair<Expr, Expr>> getElements() {
        return elements;
    }
}
