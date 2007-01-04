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

import java.util.List;

abstract public class TypeCaseOrDispatch extends FlowExpr {

    TypeCaseOrDispatch(Span span) {
        super(span);
    }

    List<Binding> bind;

    List<TypeCaseClause> clauses;

    Option<List<Expr>> else_;

    public TypeCaseOrDispatch(Span span, List<Binding> bind,
            List<TypeCaseClause> clauses, Option<List<Expr>> else_) {
        super(span);
        this.bind = bind;
        this.clauses = clauses;
        this.else_ = else_;
    }

    /**
     * @return Returns the bind.
     */
    public List<Binding> getBind() {
        return bind;
    }

    /**
     * @return Returns the clauses.
     */
    public List<TypeCaseClause> getClauses() {
        return clauses;
    }

    /**
     * @return Returns the else_.
     */
    public Option<List<Expr>> getElse_() {
        return else_;
    }

}
