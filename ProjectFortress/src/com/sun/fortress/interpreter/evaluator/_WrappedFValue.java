/*******************************************************************************
    Copyright 2007 Sun Microsystems, Inc.,
    4150 Network Circle, Santa Clara, California 95054, U.S.A.
    All rights reserved.

    U.S. Government Rights - Commercial software.
    Government users are subject to the Sun Microsystems, Inc. standard
    license agreement and applicable provisions of the FAR and its supplements.

    Use is subject to license terms.

    This distribution may include materials developed by third parties.

    Sun, Sun Microsystems, the Sun logo and Java are trademarks or registered
    trademarks of Sun Microsystems, Inc. in the U.S. and other countries.
 ******************************************************************************/

package com.sun.fortress.interpreter.evaluator;

import com.sun.fortress.nodes.*;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.nodes_util.*;
import com.sun.fortress.useful.*;

import static com.sun.fortress.interpreter.evaluator.InterpreterBug.bug;

/**
 * A _WrappedFValue permits the interpreter to incorporate intermediate
 * interpreter-computed values
 * (com.sun.fortress.interpreter.evaluator.values.FValue) into a
 * rewritten expression AST (com.sun.fortress.interpreter.nodes.Expr).
 * This permits us to eg reduce an lvalue once, and then use it
 * in lvalue OP= expr without duplicate computation.
 */
public class _WrappedFValue extends DelimitedExpr {
    private final FValue _fValue;

    /**
     * Constructs a _WrappedFValue.
     * @throws java.lang.IllegalArgumentException  If any parameter to the constructor is null.
     */
    public _WrappedFValue(Span in_span, boolean in_is_parenthesized, FValue in_fValue) {
        super(in_span, in_is_parenthesized);
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

    final public FValue getFValue() { return _fValue; }

    public <RetType> RetType accept(NodeVisitor<RetType> visitor) {
        if (visitor instanceof Evaluator) {
            @SuppressWarnings("unchecked") // RetType must be FValue
            RetType result = (RetType)(((Evaluator)visitor).for_WrappedFValue(this));
            return result;
        } else {
            return (RetType)bug("_WrappedFValue is an intermediate node only for the evaluator. Visitor " + visitor.getClass().getName() + " does not support visiting values of type " + getClass().getName());
        }
    }
    public void accept(NodeVisitor_void visitor) {
        bug("_WrappedFValue is an intermediate node only for the evaluator. Visitor " + visitor.getClass().getName() + " does not support visiting values of type " + getClass().getName());
    }

    /**
     * Implementation of toString that uses
     * {@link #output} to generate a nicely tabbed tree.
     */
    public java.lang.String toString() {
        java.io.StringWriter w = new java.io.StringWriter();
        output(w);
        return w.toString();
    }

    /**
     * Prints this object out as a nicely tabbed tree.
     */
    public void output(java.io.Writer writer) {
        outputHelp(new TabPrintWriter(writer, 2), false);
    }

    /** Generate a human-readable representation that can be deserialized. */
    public java.lang.String serialize() {
        java.io.StringWriter w = new java.io.StringWriter();
        serialize(w);
        return w.toString();
    }
    /** Generate a human-readable representation that can be deserialized. */
    public void serialize(java.io.Writer writer) {
        outputHelp(new TabPrintWriter(writer, 2), true);
    }

    public void outputHelp(TabPrintWriter writer, boolean lossless) {
        writer.print("_WrappedFValue:");
        writer.indent();

        Span temp_span = getSpan();
        writer.startLine();
        writer.print("span = ");
        if (lossless) {
            writer.printSerialized(temp_span);
            writer.print(" ");
            writer.printEscaped(temp_span);
        } else { writer.print(temp_span); }

        boolean temp_is_parenthesized = isParenthesized();
        writer.startLine();
        writer.print("is_parenthesized = ");
        writer.print(temp_is_parenthesized);

        FValue temp_fValue = getFValue();
        writer.startLine();
        writer.print("fValue = ");
        if (lossless) {
            writer.printSerialized(temp_fValue);
            writer.print(" ");
            writer.printEscaped(temp_fValue);
        } else { writer.print(temp_fValue); }
        writer.unindent();
    }

    /**
     * Implementation of equals that is based on the values of the fields of the
     * object. Thus, two objects created with identical parameters will be equal.
     */
    public boolean equals(java.lang.Object obj) {
        if (obj == null) return false;
        if ((obj.getClass() != this.getClass()) || (obj.hashCode() != this.hashCode())) {
            return false;
        } else {
            _WrappedFValue casted = (_WrappedFValue) obj;
            boolean temp_is_parenthesized = isParenthesized();
            boolean casted_is_parenthesized = casted.isParenthesized();
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
        boolean temp_is_parenthesized = isParenthesized();
        code ^= temp_is_parenthesized ? 1231 : 1237;
        FValue temp_fValue = getFValue();
        code ^= temp_fValue.hashCode();
        return code;
    }
}
