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

/**
 * An extension of DF visitors that copies as it visits (by default).
 * Override forCASE if you want to transform an AST subtree.
 */
public abstract class OpExprCopyDepthFirstVisitor extends OpExprDepthFirstVisitor<OpExpr> {

   protected OpExpr[] makeArrayOfRetType(int size) {
      return new OpExpr[size];
   }

   /* Methods to visit an item. */
   public OpExpr forLeftOnly(Left that) {
      return new Left(that.getOp());
   }

   public OpExpr forRightOnly(Right that) {
      return new Right(that.getOp());
   }

   public OpExpr forRealExprOnly(RealExpr that) {
      return new RealExpr(that.getExpr());
   }

   public OpExpr forJuxtInfixOnly(JuxtInfix that) {
      return defaultCase(that);
   }

   public OpExpr forTightInfixOnly(TightInfix that) {
      return new TightInfix(that.getOp());
   }

   public OpExpr forLooseInfixOnly(LooseInfix that) {
      return new LooseInfix(that.getOp());
   }

   public OpExpr forPrefixOnly(Prefix that) {
      return new Prefix(that.getOp());
   }

   public OpExpr forPostfixOnly(Postfix that) {
      return new Postfix(that.getOp());
   }


   /** Implementation of OpExprDepthFirstVisitor methods to implement depth-first traversal. */
   public OpExpr forLeft(Left that) {
      return forLeftOnly(that);
   }

   public OpExpr forRight(Right that) {
      return forRightOnly(that);
   }

   public OpExpr forRealExpr(RealExpr that) {
      return forRealExprOnly(that);
   }

   public OpExpr forTightInfix(TightInfix that) {
      return forTightInfixOnly(that);
   }

   public OpExpr forLooseInfix(LooseInfix that) {
      return forLooseInfixOnly(that);
   }

   public OpExpr forPrefix(Prefix that) {
      return forPrefixOnly(that);
   }

   public OpExpr forPostfix(Postfix that) {
      return forPostfixOnly(that);
   }

}
