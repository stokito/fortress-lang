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

/** An interface for visitors over OpExpr that do not return a value. */
public interface OpExprVisitor_void {

   /** Process an instance of Left. */
   public void forLeft(Left that);

   /** Process an instance of Right. */
   public void forRight(Right that);

   /** Process an instance of RealExpr. */
   public void forRealExpr(RealExpr that);

   /** Process an instance of RealType. */
   public void forRealType(RealType that);

   /** Process an instance of TightInfix. */
   public void forTightInfix(TightInfix that);

   /** Process an instance of LooseInfix. */
   public void forLooseInfix(LooseInfix that);

   /** Process an instance of Prefix. */
   public void forPrefix(Prefix that);

   /** Process an instance of Postfix. */
   public void forPostfix(Postfix that);
}
