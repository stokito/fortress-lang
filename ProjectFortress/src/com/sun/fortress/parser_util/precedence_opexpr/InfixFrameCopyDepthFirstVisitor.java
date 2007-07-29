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
public abstract class InfixFrameCopyDepthFirstVisitor extends InfixFrameDepthFirstVisitor<InfixFrame> {

   protected InfixFrame[] makeArrayOfRetType(int size) {
      return new InfixFrame[size];
   }

   /* Methods to visit an item. */
   public InfixFrame forNonChainOnly(NonChain that) {
      return defaultCase(that);
   }

   public InfixFrame forTightOnly(Tight that) {
      return new Tight(that.getOp(), that.getExprs());
   }

   public InfixFrame forLooseOnly(Loose that) {
      return new Loose(that.getOp(), that.getExprs());
   }

   public InfixFrame forChainOnly(Chain that) {
      return defaultCase(that);
   }

   public InfixFrame forTightChainOnly(TightChain that) {
      return new TightChain(that.getLinks());
   }

   public InfixFrame forLooseChainOnly(LooseChain that) {
      return new LooseChain(that.getLinks());
   }


   /** Implementation of InfixFrameDepthFirstVisitor methods to implement depth-first traversal. */
   public InfixFrame forTight(Tight that) {
      return forTightOnly(that);
   }

   public InfixFrame forLoose(Loose that) {
      return forLooseOnly(that);
   }

   public InfixFrame forTightChain(TightChain that) {
      return forTightChainOnly(that);
   }

   public InfixFrame forLooseChain(LooseChain that) {
      return forLooseChainOnly(that);
   }

}
