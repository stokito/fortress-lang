/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.parser_util.precedence_opexpr;

/**
 * A parametric abstract implementation of a visitor over InfixFrame that return a value.
 * * This visitor implements the visitor interface with methods that
 * * return the value of the defaultCase.  These methods can be overriden
 * * in order to achieve different behavior for particular cases.
 */
public abstract class InfixFrameAbstractVisitor<RetType> implements InfixFrameVisitor<RetType> {
    /**
     * This method is run for all cases by default, unless they are overridden by subclasses.
     */
    protected abstract RetType defaultCase(InfixFrame that);

    /* Methods to visit an item. */
    public RetType forNonChain(NonChain that) {
        return defaultCase(that);
    }

    public RetType forTight(Tight that) {
        return forNonChain(that);
    }

    public RetType forLoose(Loose that) {
        return forNonChain(that);
    }

    public RetType forChain(Chain that) {
        return defaultCase(that);
    }

    public RetType forTightChain(TightChain that) {
        return forChain(that);
    }

    public RetType forLooseChain(LooseChain that) {
        return forChain(that);
    }


}
