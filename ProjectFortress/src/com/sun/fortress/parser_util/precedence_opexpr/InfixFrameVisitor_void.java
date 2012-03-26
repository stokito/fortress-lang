/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.parser_util.precedence_opexpr;

/**
 * An interface for visitors over InfixFrame that do not return a value.
 */
public interface InfixFrameVisitor_void {

    /**
     * Process an instance of Tight.
     */
    public void forTight(Tight that);

    /**
     * Process an instance of Loose.
     */
    public void forLoose(Loose that);

    /**
     * Process an instance of TypeTight.
     */
    public void forTypeTight(TypeTight that);

    /**
     * Process an instance of TypeLoose.
     */
    public void forTypeLoose(TypeLoose that);

    /**
     * Process an instance of TightChain.
     */
    public void forTightChain(TightChain that);

    /**
     * Process an instance of LooseChain.
     */
    public void forLooseChain(LooseChain that);
}
