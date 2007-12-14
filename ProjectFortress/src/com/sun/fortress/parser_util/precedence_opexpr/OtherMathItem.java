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

import com.sun.fortress.nodes.Expr;
import com.sun.fortress.nodes_util.Span;

/**
 * Class OtherMathItem, a component of the MathItem composite hierarchy.
 * Note: null is not allowed as a value for any field.
 */
public class OtherMathItem extends Object implements MathItem {
   private final Expr _expr;

   /**
    * Constructs a OtherMathItem.
    * @throws java.lang.IllegalArgumentException if any parameter to the constructor is null.
    */
   public OtherMathItem(Expr in_expr) {
      super();
      _expr = in_expr;
   }

    public Expr getExpr() { return _expr; }
    public Span getSpan() { return _expr.getSpan(); }
}
