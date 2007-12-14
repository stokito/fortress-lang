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

package com.sun.fortress.parser_util.precedence_opexpr;

import com.sun.fortress.nodes.Op;
import com.sun.fortress.nodes.Expr;
import com.sun.fortress.nodes_util.Span;

/**
 * Class Exponentiation, a component of the MathItem composite hierarchy.
 * Note: null is not allowed as a value for any field.
 */
public class Exponentiation extends Object implements MathItem {
   private final Op _op;
   private final Expr _expr;

   /**
    * Constructs a Exponentiation.
    * @throws java.lang.IllegalArgumentException if any parameter to the constructor is null.
    */
   public Exponentiation(Op in_op, Expr in_expr) {
      super();
      _op = in_op;
      _expr = in_expr;
   }

    public Op getOp() { return _op; }
    public Expr getExpr() { return _expr; }

    public Span getSpan() {
        return _expr.getSpan();
    }
}
