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

/** An abstract implementation of a visitor over InfixFrame that does not return a value.
 ** This visitor implements the visitor interface with methods that
 ** execute defaultCase.  These methods can be overriden
 ** in order to achieve different behavior for particular cases.
 **/
public class InfixFrameAbstractVisitor_void implements InfixFrameVisitor_void {
   /* Methods to visit an item. */
   public void forNonChain(NonChain that) {
      defaultCase(that);
   }

   public void forTight(Tight that) {
      forNonChain(that);
   }

   public void forLoose(Loose that) {
      forNonChain(that);
   }

   public void forChain(Chain that) {
      defaultCase(that);
   }

   public void forTightChain(TightChain that) {
      forChain(that);
   }

   public void forLooseChain(LooseChain that) {
      forChain(that);
   }

   public void forTypeInfixFrame(TypeInfixFrame that) {
      defaultCase(that);
   }

   public void forTypeTight(TypeTight that) {
      forTypeInfixFrame(that);
   }

   public void forTypeLoose(TypeLoose that) {
      forTypeInfixFrame(that);
   }

   /** This method is called by default from cases that do not
    ** override forCASEOnly.
   **/
   protected void defaultCase(InfixFrame that) {}
}
