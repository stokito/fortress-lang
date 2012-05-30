/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.parser_util.precedence_opexpr;

/**
 * A parametric interface for visitors over OpExpr that return a value.
 */
public interface OpExprVisitor<RetType> {

    /**
     * Process an instance of Left.
     */
    public RetType forLeft(Left that);

    /**
     * Process an instance of Right.
     */
    public RetType forRight(Right that);

    /**
     * Process an instance of RealExpr.
     */
    public RetType forRealExpr(RealExpr that);

    /**
     * Process an instance of RealType.
     */
    public RetType forRealType(RealType that);

    /**
     * Process an instance of TightInfix.
     */
    public RetType forTightInfix(TightInfix that);

    /**
     * Process an instance of LooseInfix.
     */
    public RetType forLooseInfix(LooseInfix that);

    /**
     * Process an instance of Prefix.
     */
    public RetType forPrefix(Prefix that);

    /**
     * Process an instance of Postfix.
     */
    public RetType forPostfix(Postfix that);
}
