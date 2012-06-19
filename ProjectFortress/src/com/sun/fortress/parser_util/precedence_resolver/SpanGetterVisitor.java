/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.parser_util.precedence_resolver;

import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.nodes_util.Span;
import com.sun.fortress.parser_util.precedence_opexpr.*;


/**
 * A parametric abstract implementation of a visitor over OpExpr that return a value.
 * * This visitor implements the visitor interface with methods that
 * * return the value of the defaultCase.  These methods can be overriden
 * * in order to achieve different behavior for particular cases.
 */
public class SpanGetterVisitor implements OpExprVisitor<Span> {
    /**
     * This method is run for all cases by default, unless they are overridden by subclasses.
     */

    public Span forRealExpr(RealExpr that) {
        return NodeUtil.getSpan(that.getExpr());
    }

    public Span forRealType(RealType that) {
        return NodeUtil.getSpan(that.getType());
    }

    /* Methods to visit an item. */
    public Span forLeft(Left that) {
        return NodeUtil.getSpan(that.getOp());
    }

    public Span forRight(Right that) {
        return NodeUtil.getSpan(that.getOp());
    }

    public Span forTightInfix(TightInfix that) {
        return NodeUtil.getSpan(that.getOp());
    }

    public Span forLooseInfix(LooseInfix that) {
        return NodeUtil.getSpan(that.getOp());
    }

    public Span forPrefix(Prefix that) {
        return NodeUtil.getSpan(that.getOp());
    }

    public Span forPostfix(Postfix that) {
        return NodeUtil.getSpan(that.getOp());
    }
}
