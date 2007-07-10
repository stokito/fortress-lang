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

package com.sun.fortress.interpreter.nodes_util;

import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.interpreter.nodes.*;

/**
 * A WrappedFValue permits the interpreter to incorporate intermediate
 * interpreter-computed values (com.sun.fortress.interpreter.evaluator.values.FValue) into a
 * rewritten expression AST (com.sun.fortress.interpreter.nodes.Expr).  This permits us to eg
 * reduce an lvalue once, and then use it in lvalue OP= expr without
 * duplicate computation.
 */
public class WrappedFValue extends Expr {

    Expr original;
    FValue fvalue;

    public WrappedFValue(Expr original, FValue fvalue) {
        super(original.getSpan());
        this.original = original;
        this.fvalue = fvalue;
    }

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forWrappedFValue(this);
    }

    WrappedFValue(Span span) {
        super(span);
    }

    /**
     * @return Returns the wrapped FValue.
     */
    public FValue getFValue() {
        return fvalue;
    }

    public <RetType> RetType visit(NodeVisitor<RetType> visitor) {
        return accept(visitor);
    }
    public void visit(NodeVisitor_void visitor) {}
    public void outputHelp(TabPrintWriter writer, boolean lossless) {}
    public int generateHashCode() { return hashCode(); }
}
