/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.parser_util.precedence_opexpr;

/**
 * A parametric interface for visitors over InfixFrame that return a value.
 */
public interface InfixFrameVisitor<RetType> {

    /**
     * Process an instance of Tight.
     */
    public RetType forTight(Tight that);

    /**
     * Process an instance of Loose.
     */
    public RetType forLoose(Loose that);

    /**
     * Process an instance of TypeTight.
     */
    public RetType forTypeTight(TypeTight that);

    /**
     * Process an instance of TypeLoose.
     */
    public RetType forTypeLoose(TypeLoose that);

    /**
     * Process an instance of TightChain.
     */
    public RetType forTightChain(TightChain that);

    /**
     * Process an instance of LooseChain.
     */
    public RetType forLooseChain(LooseChain that);
}
