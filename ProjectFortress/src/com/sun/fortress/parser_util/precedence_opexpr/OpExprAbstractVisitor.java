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
 * return the value of the defaultCase.  These methods can be overriden
 * in order to achieve different behavior for particular cases.
 */
public abstract class OpExprAbstractVisitor<RetType> implements OpExprVisitor<RetType> {
    /**
     * This method is run for all cases by default, unless they are overridden by subclasses.
     */
    protected abstract RetType defaultCase(PrecedenceOpExpr that);

    /* Methods to visit an item. */
    public RetType forLeft(Left that) {
        return defaultCase(that);
    }

    public RetType forRight(Right that) {
        return defaultCase(that);
    }

    public RetType forRealExpr(RealExpr that) {
        return defaultCase(that);
    }

    public RetType forJuxtInfix(JuxtInfix that) {
        return defaultCase(that);
    }

    public RetType forTightInfix(TightInfix that) {
        return forJuxtInfix(that);
    }

    public RetType forLooseInfix(LooseInfix that) {
        return forJuxtInfix(that);
    }

    public RetType forPrefix(Prefix that) {
        return defaultCase(that);
    }

    public RetType forPostfix(Postfix that) {
        return defaultCase(that);
    }


}
