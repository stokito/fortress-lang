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
 * execute defaultCase.  These methods can be overriden
 * in order to achieve different behavior for particular cases.
 */
public class OpExprAbstractVisitor_void implements OpExprVisitor_void {
    /* Methods to visit an item. */
    public void forLeft(Left that) {
        defaultCase(that);
    }

    public void forRight(Right that) {
        defaultCase(that);
    }

    public void forRealExpr(RealExpr that) {
        defaultCase(that);
    }

    public void forRealType(RealType that) {
        defaultCase(that);
    }

    public void forJuxtInfix(JuxtInfix that) {
        defaultCase(that);
    }

    public void forTightInfix(TightInfix that) {
        forJuxtInfix(that);
    }

    public void forLooseInfix(LooseInfix that) {
        forJuxtInfix(that);
    }

    public void forPrefix(Prefix that) {
        defaultCase(that);
    }

    public void forPostfix(Postfix that) {
        defaultCase(that);
    }

    /**
     * This method is called by default from cases that do not
     * * override forCASEOnly.
     */
    protected void defaultCase(PrecedenceOpExpr that) {
    }
}
