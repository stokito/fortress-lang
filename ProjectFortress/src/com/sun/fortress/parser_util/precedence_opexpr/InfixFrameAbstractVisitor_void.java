/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.parser_util.precedence_opexpr;

/**
 * An abstract implementation of a visitor over InfixFrame that does not return a value.
 * * This visitor implements the visitor interface with methods that
 * * execute defaultCase.  These methods can be overriden
 * * in order to achieve different behavior for particular cases.
 */
public class InfixFrameAbstractVisitor_void implements InfixFrameVisitor_void {
    /* Methods to visit an item. */
    public void forNonChain(NonChain that) {
        defaultCase(that);
    }

    public void forTight(Tight that) {
        forNonChain(that);
    }

    public void forLoose(Loose that) {
        forNonChain(that);
    }

    public void forChain(Chain that) {
        defaultCase(that);
    }

    public void forTightChain(TightChain that) {
        forChain(that);
    }

    public void forLooseChain(LooseChain that) {
        forChain(that);
    }

    public void forTypeInfixFrame(TypeInfixFrame that) {
        defaultCase(that);
    }

    public void forTypeTight(TypeTight that) {
        forTypeInfixFrame(that);
    }

    public void forTypeLoose(TypeLoose that) {
        forTypeInfixFrame(that);
    }

    /**
     * This method is called by default from cases that do not
     * * override forCASEOnly.
     */
    protected void defaultCase(InfixFrame that) {
    }
}
