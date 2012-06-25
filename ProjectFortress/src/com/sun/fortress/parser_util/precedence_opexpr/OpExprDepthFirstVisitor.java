/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.parser_util.precedence_opexpr;

/**
 * A parametric abstract implementation of a visitor over PrecedenceOpExpr that return a value.
 * This visitor implements the visitor interface with methods that
 * first visit children, and then call visitCASEOnly(), passing in
 * the values of the visits of the children. (CASE is replaced by the case name.)
 */
public abstract class OpExprDepthFirstVisitor<RetType> implements OpExprVisitor<RetType> {
    protected abstract RetType[] makeArrayOfRetType(int len);

    /**
     * This method is called by default from cases that do not
     * override forCASEOnly.
     */
    protected abstract RetType defaultCase(PrecedenceOpExpr that);

    /* Methods to visit an item. */
    public RetType forLeftOnly(Left that) {
        return defaultCase(that);
    }

    public RetType forRightOnly(Right that) {
        return defaultCase(that);
    }

    public RetType forRealExprOnly(RealExpr that) {
        return defaultCase(that);
    }

    public RetType forRealTypeOnly(RealType that) {
        return defaultCase(that);
    }

    public RetType forJuxtInfixOnly(JuxtInfix that) {
        return defaultCase(that);
    }

    public RetType forTightInfixOnly(TightInfix that) {
        return forJuxtInfixOnly(that);
    }

    public RetType forLooseInfixOnly(LooseInfix that) {
        return forJuxtInfixOnly(that);
    }

    public RetType forPrefixOnly(Prefix that) {
        return defaultCase(that);
    }

    public RetType forPostfixOnly(Postfix that) {
        return defaultCase(that);
    }


    /**
     * Implementation of OpExprVisitor methods to implement depth-first traversal.
     */
    public RetType forLeft(Left that) {
        return forLeftOnly(that);
    }

    public RetType forRight(Right that) {
        return forRightOnly(that);
    }

    public RetType forRealExpr(RealExpr that) {
        return forRealExprOnly(that);
    }

    public RetType forTightInfix(TightInfix that) {
        return forTightInfixOnly(that);
    }

    public RetType forLooseInfix(LooseInfix that) {
        return forLooseInfixOnly(that);
    }

    public RetType forPrefix(Prefix that) {
        return forPrefixOnly(that);
    }

    public RetType forPostfix(Postfix that) {
        return forPostfixOnly(that);
    }

}
