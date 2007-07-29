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
public abstract class PrecedenceCopyDepthFirstVisitor extends PrecedenceDepthFirstVisitor<Precedence> {

   protected Precedence[] makeArrayOfRetType(int size) {
      return new Precedence[size];
   }

   /* Methods to visit an item. */
   public Precedence forNoneOnly(None that) {
      return new None();
   }

   public Precedence forHigherOnly(Higher that) {
      return new Higher();
   }

   public Precedence forLowerOnly(Lower that) {
      return new Lower();
   }

   public Precedence forEqualOnly(Equal that) {
      return new Equal();
   }


   /** Implementation of PrecedenceDepthFirstVisitor methods to implement depth-first traversal. */
   public Precedence forNone(None that) {
      return forNoneOnly(that);
   }

   public Precedence forHigher(Higher that) {
      return forHigherOnly(that);
   }

   public Precedence forLower(Lower that) {
      return forLowerOnly(that);
   }

   public Precedence forEqual(Equal that) {
      return forEqualOnly(that);
   }

}
