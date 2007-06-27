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

// / and type_clause = type_clause_rec node
// / and type_clause_rec =
// / {
// / type_clause_match : type_ref list;
// / type_clause_body : expr;
// / }
// /
public class TypeCaseClause extends Node {

    List<TypeRef> match;

    List<Expr> body;

    public TypeCaseClause(Span s, List<TypeRef> match, List<Expr> body) {
        super(s);
        this.match = match;
        this.body = body;
    }

    @Override
    public <T> T acceptInner(NodeVisitor<T> v) {
        return v.forTypeCaseClause(this);
    }

    TypeCaseClause(Span span) {
        super(span);
    }

    /**
     * @return Returns the body.
     */
    public List<Expr> getBody() {
        return body;
    }

    /**
     * @return Returns the match.
     */
    public List<TypeRef> getMatch() {
        return match;
    }
}
