/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.parser_util.precedence_opexpr;

import com.sun.fortress.nodes.Expr;
import com.sun.fortress.nodes.Op;
import com.sun.fortress.useful.PureList;


/**
 * Class NonChain, a component of the InfixFrame composite hierarchy.
 * Note: null is not allowed as a value for any field.
 */
public abstract class NonChain extends Object implements InfixFrame {
    private final Op _op;
    private final PureList<Expr> _exprs;
    private int _hashCode;
    private boolean _hasHashCode = false;

    /**
     * Constructs a NonChain.
     *
     * @throws java.lang.IllegalArgumentException
     *          if any parameter to the constructor is null.
     */
    public NonChain(Op in_op, PureList<Expr> in_exprs) {
        super();

        if (in_op == null) {
            throw new java.lang.IllegalArgumentException(
                    "Parameter 'op' to the NonChain constructor was null. This class may not have null field values.");
        }
        _op = in_op;

        if (in_exprs == null) {
            throw new java.lang.IllegalArgumentException(
                    "Parameter 'exprs' to the NonChain constructor was null. This class may not have null field values.");
        }
        _exprs = in_exprs;
    }

    public Op getOp() {
        return _op;
    }

    public PureList<Expr> getExprs() {
        return _exprs;
    }

    public abstract <RetType> RetType accept(InfixFrameVisitor<RetType> visitor);

    public abstract void accept(InfixFrameVisitor_void visitor);

    public abstract void outputHelp(TabPrintWriter writer);

    protected abstract int generateHashCode();

    public final int hashCode() {
        if (!_hasHashCode) {
            _hashCode = generateHashCode();
            _hasHashCode = true;
        }
        return _hashCode;
    }
}
