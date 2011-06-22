/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.parser_util.precedence_opexpr;

import com.sun.fortress.nodes.Expr;
import com.sun.fortress.nodes.IntLiteralExpr;
import com.sun.fortress.nodes.VarRef;
import com.sun.fortress.nodes_util.NodeUtil;

/**
 * Class RealExpr, a component of the OpExpr composite hierarchy.
 * Note: null is not allowed as a getVal for any field.
 */
public class RealExpr extends Object implements InfixOpExpr {
    private final Expr _expr;
    private int _hashCode;
    private boolean _hasHashCode = false;

    /**
     * Constructs a RealExpr.
     *
     * @throws java.lang.IllegalArgumentException
     *          if any parameter to the constructor is null.
     */
    public RealExpr(Expr in_expr) {
        super();

        if (in_expr == null) {
            throw new java.lang.IllegalArgumentException(
                    "Parameter 'expr' to the RealExpr constructor was null. This class may not have null field values.");
        }
        _expr = in_expr;
    }

    final public Expr getExpr() {
        return _expr;
    }

    public <RetType> RetType accept(OpExprVisitor<RetType> visitor) {
        return visitor.forRealExpr(this);
    }

    public void accept(OpExprVisitor_void visitor) {
        visitor.forRealExpr(this);
    }

    /**
     * Implementation of toString that uses
     * {@link #output} to generated nicely tabbed tree.
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
        outputHelp(new TabPrintWriter(writer, 2));
    }

    public void outputHelp(TabPrintWriter writer) {
        writer.print("RealExpr" + ":");
        writer.indent();

        writer.print(" ");
        writer.print("expr = ");
        Expr temp_expr = getExpr();
        if (temp_expr == null) {
            writer.print("null");
        } else {
            if (temp_expr instanceof IntLiteralExpr) {
                writer.print(((IntLiteralExpr) temp_expr).getIntVal().intValue());
            } else if (temp_expr instanceof VarRef) {
                writer.print(NodeUtil.nameString(((VarRef) temp_expr).getVarId()));
            } else {
                writer.print(temp_expr);
            }
        }
        writer.unindent();
    }

    /**
     * Implementation of equals that is based on the values
     * of the fields of the object. Thus, two objects
     * created with identical parameters will be equal.
     */
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if ((obj.getClass() != this.getClass()) || (obj.hashCode() != this.hashCode())) {
            return false;
        } else {
            RealExpr casted = (RealExpr) obj;
            if (!(getExpr().equals(casted.getExpr()))) return false;
            return true;
        }
    }

    /**
     * Implementation of hashCode that is consistent with
     * equals. The getVal of the hashCode is formed by
     * XORing the hashcode of the class object with
     * the hashcodes of all the fields of the object.
     */
    protected int generateHashCode() {
        int code = getClass().hashCode();
        code ^= getExpr().hashCode();
        return code;
    }

    public final int hashCode() {
        if (!_hasHashCode) {
            _hashCode = generateHashCode();
            _hasHashCode = true;
        }
        return _hashCode;
    }
}
