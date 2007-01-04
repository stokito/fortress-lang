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

public abstract class ValueExpr extends Expr {
    protected ValueExpr(Span span) {
        super(span);
    }
}

// / and value_expr =
// / [
// / | `InfinityExpr
// / | `VoidExpr
// / | `FnExpr of fn_expr
// / | `ObjectExpr of object_expr
// / | `IntExpr of literal
// / | `FloatExpr of literal
// / | `StringExpr of string
// / | `CharExpr of int
// / | `TupleExpr of expr list
// / | `ListExpr of expr list
// / | `SetExpr of expr list
// / | `MapExpr of entry list
// / | `RectangularExpr of multi_dim_expr
// / | `IntervalExpr of interval_expr
// / ] node
// /
