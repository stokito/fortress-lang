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

public abstract class Expr extends Tree {

    protected Expr(Span span) {
        super(span);
    }

    protected Expr(Node rewriteFrom) {
        super(rewriteFrom);
    }

}
// / and expr =
// / [
// / | `VarRefExpr of id
// / | `FieldSelection of field_selection
// / | `TightJuxt of expr list
// / | `LooseJuxt of expr list
// / | `OprExpr of opr_expr
// / | `ChainExpr of chain_expr
// / | `AssignmentExpr of assignment_expr
// / | `ApplyExpr of apply_expr
// / | `KeywordsExpr of keywords_expr
// / | `TypeApplyExpr of type_apply_expr
// / | `SubscriptExpr of subscript_expr
// / | `LetExpr of let_expr * expr list
// / | `FlowExpr of flow_expr
// / | `ValueExpr of value_expr
// / | `ComprehensionExpr of comprehension_expr
// / | `TypeAscriptionExpr of type_ascription
// / ] node
// /
