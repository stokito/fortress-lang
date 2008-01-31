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
 * An extension of DF visitors that copies as it visits (by default).
 * Override forCASE if you want to transform an AST subtree.
 */
public abstract class OpExprCopyDepthFirstVisitor extends OpExprDepthFirstVisitor<PrecedenceOpExpr> {

   protected PrecedenceOpExpr[] makeArrayOfRetType(int size) {
      return new PrecedenceOpExpr[size];
   }

   /* Methods to visit an item. */
   public PrecedenceOpExpr forLeftOnly(Left that) {
      return new Left(that.getOp());
   }

   public PrecedenceOpExpr forRightOnly(Right that) {
      return new Right(that.getOp());
   }

   public PrecedenceOpExpr forRealExprOnly(RealExpr that) {
      return new RealExpr(that.getExpr());
   }

   public PrecedenceOpExpr forJuxtInfixOnly(JuxtInfix that) {
      return defaultCase(that);
   }

   public PrecedenceOpExpr forTightInfixOnly(TightInfix that) {
      return new TightInfix(that.getOp());
   }

   public PrecedenceOpExpr forLooseInfixOnly(LooseInfix that) {
      return new LooseInfix(that.getOp());
   }

   public PrecedenceOpExpr forPrefixOnly(Prefix that) {
       if (that instanceof TightPrefix)
           return new TightPrefix(that.getOp());
       else
           return new LoosePrefix(that.getOp());
   }

   public PrecedenceOpExpr forPostfixOnly(Postfix that) {
      return new Postfix(that.getOp());
   }


   /** Implementation of OpExprDepthFirstVisitor methods to implement depth-first traversal. */
   public PrecedenceOpExpr forLeft(Left that) {
      return forLeftOnly(that);
   }

   public PrecedenceOpExpr forRight(Right that) {
      return forRightOnly(that);
   }

   public PrecedenceOpExpr forRealExpr(RealExpr that) {
      return forRealExprOnly(that);
   }

   public PrecedenceOpExpr forTightInfix(TightInfix that) {
      return forTightInfixOnly(that);
   }

   public PrecedenceOpExpr forLooseInfix(LooseInfix that) {
      return forLooseInfixOnly(that);
   }

   public PrecedenceOpExpr forPrefix(Prefix that) {
      return forPrefixOnly(that);
   }

   public PrecedenceOpExpr forPostfix(Postfix that) {
      return forPostfixOnly(that);
   }

}
