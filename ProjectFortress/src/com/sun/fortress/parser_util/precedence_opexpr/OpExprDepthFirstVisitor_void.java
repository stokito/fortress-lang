/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.parser_util.precedence_opexpr;

/**
 * An abstract implementation of a visitor over PrecedenceOpExpr that does not return a value.
 * This visitor implements the visitor interface with methods that
 * first visit children, and then call visitCASEOnly().
 * (CASE is replaced by the case name.)
 * The default implementation of the forCASEOnly methods call
 * protected method defaultCase(). This method defaults to no-op.
 */
public class OpExprDepthFirstVisitor_void implements OpExprVisitor_void {
    /* Methods to visit an item. */
    public void forLeftDoFirst(Left that) {
        defaultDoFirst(that);
    }

    public void forLeftOnly(Left that) {
        defaultCase(that);
    }

    public void forRightDoFirst(Right that) {
        defaultDoFirst(that);
    }

    public void forRightOnly(Right that) {
        defaultCase(that);
    }

    public void forRealExprDoFirst(RealExpr that) {
        defaultDoFirst(that);
    }

    public void forRealExprOnly(RealExpr that) {
        defaultCase(that);
    }

    public void forRealTypeDoFirst(RealType that) {
        defaultDoFirst(that);
    }

    public void forRealTypeOnly(RealType that) {
        defaultCase(that);
    }

    public void forJuxtInfixDoFirst(JuxtInfix that) {
        defaultDoFirst(that);
    }

    public void forJuxtInfixOnly(JuxtInfix that) {
        defaultCase(that);
    }

    public void forTightInfixDoFirst(TightInfix that) {
        forJuxtInfixDoFirst(that);
    }

    public void forTightInfixOnly(TightInfix that) {
        forJuxtInfixOnly(that);
    }

    public void forLooseInfixDoFirst(LooseInfix that) {
        forJuxtInfixDoFirst(that);
    }

    public void forLooseInfixOnly(LooseInfix that) {
        forJuxtInfixOnly(that);
    }

    public void forPrefixDoFirst(Prefix that) {
        defaultDoFirst(that);
    }

    public void forPrefixOnly(Prefix that) {
        defaultCase(that);
    }

    public void forPostfixDoFirst(Postfix that) {
        defaultDoFirst(that);
    }

    public void forPostfixOnly(Postfix that) {
        defaultCase(that);
    }

    /* Implementation of OpExprVisitor_void methods to implement depth-first traversal. */
    public void forLeft(Left that) {
        forLeftDoFirst(that);
        forLeftOnly(that);
    }

    public void forRight(Right that) {
        forRightDoFirst(that);
        forRightOnly(that);
    }

    public void forRealExpr(RealExpr that) {
        forRealExprDoFirst(that);
        forRealExprOnly(that);
    }

    public void forRealType(RealType that) {
        forRealTypeDoFirst(that);
        forRealTypeOnly(that);
    }

    public void forTightInfix(TightInfix that) {
        forTightInfixDoFirst(that);
        forTightInfixOnly(that);
    }

    public void forLooseInfix(LooseInfix that) {
        forLooseInfixDoFirst(that);
        forLooseInfixOnly(that);
    }

    public void forPrefix(Prefix that) {
        forPrefixDoFirst(that);
        forPrefixOnly(that);
    }

    public void forPostfix(Postfix that) {
        forPostfixDoFirst(that);
        forPostfixOnly(that);
    }

    /**
     * This method is called by default from cases that do not
     * * override forCASEDoFirst.
     */
    protected void defaultDoFirst(PrecedenceOpExpr that) {
    }

    /**
     * This method is called by default from cases that do not
     * * override forCASEOnly.
     */
    protected void defaultCase(PrecedenceOpExpr that) {
    }
}
