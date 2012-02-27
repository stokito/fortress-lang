/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.parser_util.precedence_opexpr;

import com.sun.fortress.nodes.Effect;
import com.sun.fortress.nodes.Op;
import com.sun.fortress.nodes.Type;

/**
 * Class TypeInfixFrame, a component of the InfixFrame composite hierarchy.
 * Note: null is not allowed as a value for any field.
 */
public abstract class TypeInfixFrame extends Object implements InfixFrame {
    private final Op _op;
    private final Effect _effect;
    private final Type _arg;
    private int _hashCode;
    private boolean _hasHashCode = false;

    /**
     * Constructs a TypeInfixFrame.
     *
     * @throws java.lang.IllegalArgumentException
     *          if any parameter to the constructor is null.
     */
    public TypeInfixFrame(Op in_op, Effect in_effect, Type in_arg) {
        super();
        _op = in_op;
        _effect = in_effect;
        _arg = in_arg;
    }

    public Op getOp() {
        return _op;
    }

    public Effect getEffect() {
        return _effect;
    }

    public Type getArg() {
        return _arg;
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
