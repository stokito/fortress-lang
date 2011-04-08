/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.parser_util.precedence_opexpr;

import com.sun.fortress.nodes.Effect;
import com.sun.fortress.nodes.Op;
import com.sun.fortress.nodes_util.NodeFactory;

/**
 * Class JuxtInfix, a component of the OpExpr composite hierarchy.
 * Note: null is not allowed as a value for any field.
 */
public abstract class JuxtInfix extends Object implements InfixOpExpr {
    private final Op _op;
    private final Effect _effect;
    private int _hashCode;
    private boolean _hasHashCode = false;

    /**
     * Constructs a JuxtInfix.
     *
     * @throws java.lang.IllegalArgumentException
     *          if any parameter to the constructor is null.
     */
    public JuxtInfix(Op in_op) {
        super();

        if (in_op == null) {
            throw new java.lang.IllegalArgumentException(
                    "Parameter 'op' to the JuxtInfix constructor was null. This class may not have null field values.");
        }
        _op = in_op;
        _effect = NodeFactory.emptyEffect;
    }

    public JuxtInfix(Op in_op, Effect in_effect) {
        super();

        if (in_op == null) {
            throw new java.lang.IllegalArgumentException(
                    "Parameter 'op' to the JuxtInfix constructor was null. This class may not have null field values.");
        }
        _op = in_op;
        if (in_effect == null) {
            throw new java.lang.IllegalArgumentException(
                    "Parameter '_effect' to the JuxtInfix constructor was null. This class may not have null field values.");
        }
        _effect = in_effect;
    }

    public Op getOp() {
        return _op;
    }

    public Effect getEffect() {
        return _effect;
    }

    public abstract <RetType> RetType accept(OpExprVisitor<RetType> visitor);

    public abstract void accept(OpExprVisitor_void visitor);

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
