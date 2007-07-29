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

/** A parametric abstract implementation of a visitor over Precedence that return a value.
 ** This visitor implements the visitor interface with methods that
 ** return the value of the defaultCase.  These methods can be overriden
 ** in order to achieve different behavior for particular cases.
 **/
public abstract class PrecedenceAbstractVisitor<RetType> implements PrecedenceVisitor<RetType> {
   /**
    * This method is run for all cases by default, unless they are overridden by subclasses.
   **/
   protected abstract RetType defaultCase(Precedence that);

   /* Methods to visit an item. */
   public RetType forNone(None that) {
      return defaultCase(that);
   }

   public RetType forHigher(Higher that) {
      return defaultCase(that);
   }

   public RetType forLower(Lower that) {
      return defaultCase(that);
   }

   public RetType forEqual(Equal that) {
      return defaultCase(that);
   }


}
