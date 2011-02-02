/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.evaluator;

import com.sun.fortress.exceptions.InterpreterBug;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.nodes_util.Span;

/**
 * A _WrappedFValue permits the interpreter to incorporate intermediate
 * interpreter-computed values
 * (com.sun.fortress.interpreter.evaluator.values.FValue) into a
 * rewritten expression AST (com.sun.fortress.interpreter.nodes.Expr).
 * This permits us to eg reduce an lvalue once, and then use it
 * in lvalue OP= expr without duplicate computation.
 */
public class _WrappedFValue extends Expr {
    private final FValue _fValue;

    /**
     * Constructs a _WrappedFValue.
     *
     * @throws java.lang.IllegalArgumentException
     *          If any parameter to the constructor is null.
     */
    public _WrappedFValue(Span in_span, boolean in_is_parenthesized, FValue in_fValue) {
        super();
        if (in_fValue == null) {
            throw new java.lang.IllegalArgumentException("Parameter 'fValue' to the _WrappedFValue constructor was null");
        }
        _fValue = in_fValue;
    }

    /**
     * Empty constructor, for reflective access.  Clients are
     * responsible for manually instantiating each field.
     */
    protected _WrappedFValue() {
        _fValue = null;
    }

    final public FValue getFValue() {
        return _fValue;
    }

    public <RetType> RetType accept(NodeVisitor<RetType> visitor) {
        if (visitor instanceof Evaluator) {
            @SuppressWarnings ("unchecked") // RetType must be FValue
                    RetType result = (RetType) (((Evaluator) visitor).for_WrappedFValue(this));
            return result;
        } else {
            return InterpreterBug.<RetType>bug(
                    "_WrappedFValue is an intermediate node only for the evaluator. Visitor " +
                    visitor.getClass().getName() + " does not support visiting values of type " + getClass().getName());
        }
    }

    public <RetType> RetType accept(AbstractNodeVisitor<RetType> visitor) {
        return InterpreterBug.<RetType>bug("_WrappedFValue is an intermediate node only for the evaluator. Visitor " +
                                           visitor.getClass().getName() + " does not support visiting values of type " +
                                           getClass().getName());
    }

    public <RetType> RetType accept(ExprVisitor<RetType> visitor) {
        return InterpreterBug.<RetType>bug("_WrappedFValue is an intermediate node only for the evaluator. Visitor " +
                                           visitor.getClass().getName() + " does not support visiting values of type " +
                                           getClass().getName());
    }

    public void accept(NodeVisitor_void visitor) {
        InterpreterBug.bug("_WrappedFValue is an intermediate node only for the evaluator. Visitor " +
                           visitor.getClass().getName() + " does not support visiting values of type " +
                           getClass().getName());
    }

    public void accept(AbstractNodeVisitor_void visitor) {
        InterpreterBug.bug("_WrappedFValue is an intermediate node only for the evaluator. Visitor " +
                           visitor.getClass().getName() + " does not support visiting values of type " +
                           getClass().getName());
    }

    public void accept(ExprVisitor_void visitor) {
        InterpreterBug.bug("_WrappedFValue is an intermediate node only for the evaluator. Visitor " +
                           visitor.getClass().getName() + " does not support visiting values of type " +
                           getClass().getName());
    }

    public java.lang.String toString() {
        StringBuilder b = new StringBuilder("_WrappedFValue@");
        b.append(NodeUtil.getSpan(this));
        b.append("(");
        b.append(_fValue.toString());
        b.append(")");
        return b.toString();
    }

    /**
     * Generate a human-readable representation that can be deserialized.
     */
    public java.lang.String serialize() {
        java.io.StringWriter w = new java.io.StringWriter();
        serialize(w);
        return w.toString();
    }

    /**
     * Generate a human-readable representation that can be deserialized.
     */
    public void serialize(java.io.Writer writer) {
        walk(new LosslessStringWalker(writer, 2));
    }

    public void walk(TreeWalker w) {
        if (w.visitNode(this, "_WrappedFValue", 3)) {
            Span s = NodeUtil.getSpan(this);
            if (w.visitNodeField("span", s)) {
                w.visitUnknownObject(s);
                w.endNodeField("span", s);
            }
            boolean p = NodeUtil.isParenthesized(this);
            if (w.visitNodeField("is_parenthesized", p)) {
                w.visitBoolean(p);
                w.endNodeField("is_parenthesized", p);
            }
            FValue val = getFValue();
            if (w.visitNodeField("fValue", val)) {
                w.visitUnknownObject(val);
                w.endNodeField("fValue", val);
            }
            w.endNode(this, "_WrappedFValue", 3);
        }
    }

    /**
     * Implementation of equals that is based on the values of the fields of the
     * object. Thus, two objects created with identical parameters will be equal.
     */
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if ((obj.getClass() != this.getClass()) || (obj.hashCode() != this.hashCode())) {
            return false;
        } else {
            _WrappedFValue casted = (_WrappedFValue) obj;
            boolean temp_is_parenthesized = NodeUtil.isParenthesized(this);
            boolean casted_is_parenthesized = NodeUtil.isParenthesized(casted);
            if (!(temp_is_parenthesized == casted_is_parenthesized)) return false;
            FValue temp_fValue = getFValue();
            FValue casted_fValue = casted.getFValue();
            if (!(temp_fValue == casted_fValue || temp_fValue.equals(casted_fValue))) return false;
            return true;
        }
    }

    /**
     * Implementation of hashCode that is consistent with equals.  The value of
     * the hashCode is formed by XORing the hashcode of the class object with
     * the hashcodes of all the fields of the object.
     */
    public int generateHashCode() {
        int code = getClass().hashCode();
        boolean temp_is_parenthesized = NodeUtil.isParenthesized(this);
        code ^= temp_is_parenthesized ? 1231 : 1237;
        FValue temp_fValue = getFValue();
        code ^= temp_fValue.hashCode();
        return code;
    }
}
