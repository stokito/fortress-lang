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
 * * first visit children, and then call visitCASEOnly().
 * * (CASE is replaced by the case name.)
 * * The default implementation of the forCASEOnly methods call
 * * protected method defaultCase(). This method defaults to no-op.
 */
public class InfixFrameDepthFirstVisitor_void implements InfixFrameVisitor_void {
    /* Methods to visit an item. */
    public void forNonChainDoFirst(NonChain that) {
        defaultDoFirst(that);
    }

    public void forNonChainOnly(NonChain that) {
        defaultCase(that);
    }

    public void forTightDoFirst(Tight that) {
        forNonChainDoFirst(that);
    }

    public void forTightOnly(Tight that) {
        forNonChainOnly(that);
    }

    public void forLooseDoFirst(Loose that) {
        forNonChainDoFirst(that);
    }

    public void forLooseOnly(Loose that) {
        forNonChainOnly(that);
    }

    public void forTypeInfixFrameDoFirst(TypeInfixFrame that) {
        defaultDoFirst(that);
    }

    public void forTypeInfixFrameOnly(TypeInfixFrame that) {
        defaultCase(that);
    }

    public void forTypeTightDoFirst(TypeTight that) {
        forTypeInfixFrameDoFirst(that);
    }

    public void forTypeTightOnly(TypeTight that) {
        forTypeInfixFrameOnly(that);
    }

    public void forTypeLooseDoFirst(TypeLoose that) {
        forTypeInfixFrameDoFirst(that);
    }

    public void forTypeLooseOnly(TypeLoose that) {
        forTypeInfixFrameOnly(that);
    }

    public void forChainDoFirst(Chain that) {
        defaultDoFirst(that);
    }

    public void forChainOnly(Chain that) {
        defaultCase(that);
    }

    public void forTightChainDoFirst(TightChain that) {
        forChainDoFirst(that);
    }

    public void forTightChainOnly(TightChain that) {
        forChainOnly(that);
    }

    public void forLooseChainDoFirst(LooseChain that) {
        forChainDoFirst(that);
    }

    public void forLooseChainOnly(LooseChain that) {
        forChainOnly(that);
    }

    /* Implementation of InfixFrameVisitor_void methods to implement depth-first traversal. */
    public void forTight(Tight that) {
        forTightDoFirst(that);
        forTightOnly(that);
    }

    public void forLoose(Loose that) {
        forLooseDoFirst(that);
        forLooseOnly(that);
    }

    public void forTypeTight(TypeTight that) {
        forTypeTightDoFirst(that);
        forTypeTightOnly(that);
    }

    public void forTypeLoose(TypeLoose that) {
        forTypeLooseDoFirst(that);
        forTypeLooseOnly(that);
    }

    public void forTightChain(TightChain that) {
        forTightChainDoFirst(that);
        forTightChainOnly(that);
    }

    public void forLooseChain(LooseChain that) {
        forLooseChainDoFirst(that);
        forLooseChainOnly(that);
    }

    /**
     * This method is called by default from cases that do not
     * * override forCASEDoFirst.
     */
    protected void defaultDoFirst(InfixFrame that) {
    }

    /**
     * This method is called by default from cases that do not
     * * override forCASEOnly.
     */
    protected void defaultCase(InfixFrame that) {
    }
}
