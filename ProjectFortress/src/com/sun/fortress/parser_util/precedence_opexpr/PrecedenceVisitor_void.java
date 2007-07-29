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

/** An interface for visitors over Precedence that do not return a value. */
public interface PrecedenceVisitor_void {

   /** Process an instance of None. */
   public void forNone(None that);

   /** Process an instance of Higher. */
   public void forHigher(Higher that);

   /** Process an instance of Lower. */
   public void forLower(Lower that);

   /** Process an instance of Equal. */
   public void forEqual(Equal that);
}
