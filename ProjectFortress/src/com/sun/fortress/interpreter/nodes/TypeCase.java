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

// / and type_case_expr = type_case_expr_rec node
// / and type_case_expr_rec =
// / {
// / type_case_expr_bind : binding list;
// / type_case_expr_clauses : type_clause list;
// / type_case_expr_else : expr list option;
// / }
// /
public class TypeCase extends TypeCaseOrDispatch {

    public TypeCase(Span span, List<Binding> bind,
            List<TypeCaseClause> clauses, Option<List<Expr>> else_) {
        super(span, bind, clauses, else_);
    }

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forTypeCase(this);
    }

    TypeCase(Span span) {
        super(span);
    }
}
