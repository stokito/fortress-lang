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

/** An interface for visitors over InfixFrame that do not return a value. */
public interface InfixFrameVisitor_void {

   /** Process an instance of Tight. */
   public void forTight(Tight that);

   /** Process an instance of Loose. */
   public void forLoose(Loose that);

   /** Process an instance of TypeTight. */
   public void forTypeTight(TypeTight that);

   /** Process an instance of TypeLoose. */
   public void forTypeLoose(TypeLoose that);

   /** Process an instance of TightChain. */
   public void forTightChain(TightChain that);

   /** Process an instance of LooseChain. */
   public void forLooseChain(LooseChain that);
}
