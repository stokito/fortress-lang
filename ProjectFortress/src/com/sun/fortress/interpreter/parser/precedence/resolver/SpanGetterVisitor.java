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

package com.sun.fortress.interpreter.parser.precedence.resolver;
import com.sun.fortress.interpreter.nodes_util.Span;
import com.sun.fortress.interpreter.parser.precedence.opexpr.Left;
import com.sun.fortress.interpreter.parser.precedence.opexpr.LooseInfix;
import com.sun.fortress.interpreter.parser.precedence.opexpr.OpExprVisitor;
import com.sun.fortress.interpreter.parser.precedence.opexpr.Postfix;
import com.sun.fortress.interpreter.parser.precedence.opexpr.Prefix;
import com.sun.fortress.interpreter.parser.precedence.opexpr.RealExpr;
import com.sun.fortress.interpreter.parser.precedence.opexpr.Right;
import com.sun.fortress.interpreter.parser.precedence.opexpr.TightInfix;


/** A parametric abstract implementation of a visitor over OpExpr that return a value.
 ** This visitor implements the visitor interface with methods that
 ** return the value of the defaultCase.  These methods can be overriden
 ** in order to achieve different behavior for particular cases.
 **/
public class SpanGetterVisitor implements OpExprVisitor<Span> {
   /**
    * This method is run for all cases by default, unless they are overridden by subclasses.
   **/

   public Span forRealExpr(RealExpr that) {
     return that.getExpr().getSpan();
   }

   /* Methods to visit an item. */
   public Span forLeft(Left that) {
     return that.getOp().getSpan();
   }

   public Span forRight(Right that) {
     return that.getOp().getSpan();
   }

   public Span forTightInfix(TightInfix that) {
     return that.getOp().getSpan();
   }

   public Span forLooseInfix(LooseInfix that) {
     return that.getOp().getSpan();
   }

   public Span forPrefix(Prefix that) {
     return that.getOp().getSpan();
   }

   public Span forPostfix(Postfix that) {
     return that.getOp().getSpan();
   }
}
