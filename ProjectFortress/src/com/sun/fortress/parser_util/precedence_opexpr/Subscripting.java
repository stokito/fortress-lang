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

import com.sun.fortress.nodes.SubscriptOp;
import com.sun.fortress.nodes.Expr;
import com.sun.fortress.nodes_util.Span;
import java.util.List;

/**
 * Class Subscripting, a component of the MathItem composite hierarchy.
 * Note: null is not allowed as a value for any field.
 */
public class Subscripting extends Object implements MathItem {
   private final SubscriptOp _op;
   private final List<Expr> _exprs;

   /**
    * Constructs a Subscripting.
    * @throws java.lang.IllegalArgumentException if any parameter to the constructor is null.
    */
   public Subscripting(SubscriptOp in_op, List<Expr> in_exprs) {
      super();
      _op = in_op;
      _exprs = in_exprs;
   }

    public SubscriptOp getOp() { return _op; }
    public List<Expr> getExprs() { return _exprs; }
    public Span getSpan() { return _op.getSpan(); }
}
