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

/** A parametric abstract implementation of a visitor over InfixFrame that return a value.
 ** This visitor implements the visitor interface with methods that
 ** first visit children, and then call visitCASEOnly(), passing in
 ** the values of the visits of the children. (CASE is replaced by the case name.)
 **/
public abstract class InfixFrameDepthFirstVisitor<RetType> implements InfixFrameVisitor<RetType> {
   protected abstract RetType[] makeArrayOfRetType(int len);

   /**
    * This method is called by default from cases that do not
    * override forCASEOnly.
   **/
   protected abstract RetType defaultCase(InfixFrame that);

   /* Methods to visit an item. */
   public RetType forNonChainOnly(NonChain that) {
      return defaultCase(that);
   }

   public RetType forTightOnly(Tight that) {
      return forNonChainOnly(that);
   }

   public RetType forLooseOnly(Loose that) {
      return forNonChainOnly(that);
   }

   public RetType forChainOnly(Chain that) {
      return defaultCase(that);
   }

   public RetType forTightChainOnly(TightChain that) {
      return forChainOnly(that);
   }

   public RetType forLooseChainOnly(LooseChain that) {
      return forChainOnly(that);
   }


   /** Implementation of InfixFrameVisitor methods to implement depth-first traversal. */
   public RetType forTight(Tight that) {
      return forTightOnly(that);
   }

   public RetType forLoose(Loose that) {
      return forLooseOnly(that);
   }

   public RetType forTightChain(TightChain that) {
      return forTightChainOnly(that);
   }

   public RetType forLooseChain(LooseChain that) {
      return forLooseChainOnly(that);
   }

}
