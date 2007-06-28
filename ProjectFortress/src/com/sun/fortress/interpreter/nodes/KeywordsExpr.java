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


// / and keywords_expr = keywords_expr_rec node
// / and keywords_expr_rec =
// / {
// / keywords_expr_args : expr list;
// / keywords_expr_keywords : (id * expr) list;
// / }
// /
public class KeywordsExpr extends TupleExpr {

    List<Expr> args;

    List<Pair<Id, Expr>> keywords;

    public KeywordsExpr(Span span, List<Expr> args,
            List<Pair<Id, Expr>> keywords) {
        super(span);
        this.args = args;
        this.keywords = keywords;
    }

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forKeywordsExpr(this);
    }

    KeywordsExpr(Span span) {
        super(span);
    }

    /**
     * @return Returns the args.
     */
    public List<Expr> getArgs() {
        return args;
    }

    /**
     * @return Returns the keywords.
     */
    public List<Pair<Id, Expr>> getKeywords() {
        return keywords;
    }
}
