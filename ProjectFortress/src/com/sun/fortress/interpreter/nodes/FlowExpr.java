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

public abstract class FlowExpr extends Expr {
    protected FlowExpr(Span span) {
        super(span);
    }
}

// / and flow_expr =
// / [
// / | `IfExpr of if_expr
// / | `CaseExpr of case_expr
// / | `DispatchExpr of type_case_expr
// / | `TypeCaseExpr of type_case_expr
// / | `TryExpr of try_expr
// / | `ThrowExpr of expr
// / | `LabelExpr of label_expr
// / | `ExitExpr of exit_expr
// / | `ForExpr of for_expr
// / | `WhileExpr of while_expr
// / | `AccumulatorExpr of accumulator_expr
// / | `AtomicExpr of expr
// / | `TryAtomicExpr of expr
// / | `BlockExpr of expr list
// / | `SpawnExpr of spawn_expr
// / ] node
