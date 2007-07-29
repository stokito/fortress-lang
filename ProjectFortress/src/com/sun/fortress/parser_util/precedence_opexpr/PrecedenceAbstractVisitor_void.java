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

/** An abstract implementation of a visitor over Precedence that does not return a value.
 ** This visitor implements the visitor interface with methods that
 ** execute defaultCase.  These methods can be overriden
 ** in order to achieve different behavior for particular cases.
 **/
public class PrecedenceAbstractVisitor_void implements PrecedenceVisitor_void {
   /* Methods to visit an item. */
   public void forNone(None that) {
      defaultCase(that);
   }

   public void forHigher(Higher that) {
      defaultCase(that);
   }

   public void forLower(Lower that) {
      defaultCase(that);
   }

   public void forEqual(Equal that) {
      defaultCase(that);
   }

   /** This method is called by default from cases that do not
    ** override forCASEOnly.
   **/
   protected void defaultCase(Precedence that) {}
}
