/*******************************************************************************
    Copyright 2008 Sun Microsystems, Inc.,
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

/**
 * An abstract implementation of a visitor over PrecedenceOpExpr that does not return a value.
 * This visitor implements the visitor interface with methods that
 * execute defaultCase.  These methods can be overriden
 * in order to achieve different behavior for particular cases.
 */
public class OpExprAbstractVisitor_void implements OpExprVisitor_void {
   /* Methods to visit an item. */
   public void forLeft(Left that) {
      defaultCase(that);
   }

   public void forRight(Right that) {
      defaultCase(that);
   }

   public void forRealExpr(RealExpr that) {
      defaultCase(that);
   }

   public void forRealType(RealType that) {
      defaultCase(that);
   }

   public void forJuxtInfix(JuxtInfix that) {
      defaultCase(that);
   }

   public void forTightInfix(TightInfix that) {
      forJuxtInfix(that);
   }

   public void forLooseInfix(LooseInfix that) {
      forJuxtInfix(that);
   }

   public void forPrefix(Prefix that) {
      defaultCase(that);
   }

   public void forPostfix(Postfix that) {
      defaultCase(that);
   }

   /** This method is called by default from cases that do not
    ** override forCASEOnly.
   **/
   protected void defaultCase(PrecedenceOpExpr that) {}
}
