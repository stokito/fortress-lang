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

import com.sun.fortress.nodes.Op;

/**
 * Class JuxtInfix, a component of the OpExpr composite hierarchy.
 * Note: null is not allowed as a value for any field.
 */
public abstract class JuxtInfix extends Object implements InfixOpExpr {
   private final Op _op;
   private int _hashCode;
   private boolean _hasHashCode = false;

   /**
    * Constructs a JuxtInfix.
    * @throws java.lang.IllegalArgumentException if any parameter to the constructor is null.
    */
   public JuxtInfix(Op in_op) {
      super();

      if (in_op == null) {
         throw new java.lang.IllegalArgumentException("Parameter 'op' to the JuxtInfix constructor was null. This class may not have null field values.");
      }
      _op = in_op;
   }

   public Op getOp() { return _op; }

   public abstract <RetType> RetType accept(OpExprVisitor<RetType> visitor);
   public abstract void accept(OpExprVisitor_void visitor);
   public abstract void outputHelp(TabPrintWriter writer);
   protected abstract int generateHashCode();
   public final int hashCode() {
      if (! _hasHashCode) { _hashCode = generateHashCode(); _hasHashCode = true; }
      return _hashCode;
   }
}
