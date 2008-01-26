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

/** A parametric interface for visitors over InfixFrame that return a value. */
public interface InfixFrameVisitor<RetType> {

   /** Process an instance of Tight. */
   public RetType forTight(Tight that);

   /** Process an instance of Loose. */
   public RetType forLoose(Loose that);

   /** Process an instance of TypeTight. */
   public RetType forTypeTight(TypeTight that);

   /** Process an instance of TypeLoose. */
   public RetType forTypeLoose(TypeLoose that);

   /** Process an instance of TightChain. */
   public RetType forTightChain(TightChain that);

   /** Process an instance of LooseChain. */
   public RetType forLooseChain(LooseChain that);
}
