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
import com.sun.fortress.interpreter.useful.Option;
import com.sun.fortress.interpreter.useful.Some;
import java.util.List;

import com.sun.fortress.interpreter.useful.IterableOnce;
import com.sun.fortress.interpreter.useful.UnitIterable;


public class PropertyDecl extends AbstractNode implements Decl, AbsDecl {
    Option<Id> id;

    List<Param> params;

    Expr expr;

    public PropertyDecl(Span span, Option<Id> id, List<Param> params, Expr expr) {
        super(span);
        this.id = id;
        this.params = params;
        this.expr = expr;
    }

    @Override
    public <T> T acceptInner(NodeVisitor<T> v) {
        return v.forPropertyDecl(this);
    }

    PropertyDecl(Span span) {
        super(span);
    }

    /**
     * @return Returns the id.
     */
    public Option<Id> getId() {
        return id;
    }

    /**
     * @return Returns the list of generators.
     */
    public List<Param> getParams() {
        return params;
    }

    /**
     * @return Returns the body expression.
     */
    public Expr getExpr() {
        return expr;
    }

    public IterableOnce<String> stringNames() {
        if (id instanceof Some) {
            Some s = (Some) id;
            return new UnitIterable<String>(((Id) (s.getVal())).getName());
        } else {
            return new UnitIterable<String>("_");
        }
    }

}
