/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.parser_util.precedence_opexpr;

import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes_util.NodeUtil;

/**
 * Class RealType, a component of the OpExpr composite hierarchy.
 * Note: null is not allowed as a getVal for any field.
 */
public class RealType extends Object implements InfixOpExpr {
    private final Type _type;
    private int _hashCode;
    private boolean _hasHashCode = false;

    /**
     * Constructs a RealType.
     *
     * @throws java.lang.IllegalArgumentException
     *          if any parameter to the constructor is null.
     */
    public RealType(Type in_type) {
        super();

        if (in_type == null) {
            throw new java.lang.IllegalArgumentException(
                    "Parameter 'type' to the RealType constructor was null. This class may not have null field values.");
        }
        _type = in_type;
    }

    final public Type getType() {
        return _type;
    }

    public <RetType> RetType accept(OpExprVisitor<RetType> visitor) {
        return visitor.forRealType(this);
    }

    public void accept(OpExprVisitor_void visitor) {
        visitor.forRealType(this);
    }

    /**
     * Implementation of toString that uses
     * {@link #output} to generated nicely tabbed tree.
     */
    public java.lang.String toString() {
        java.io.StringWriter w = new java.io.StringWriter();
        output(w);
        return w.toString() + " at " + NodeUtil.getSpan(_type);
    }

    /**
     * Prints this object out as a nicely tabbed tree.
     */
    public void output(java.io.Writer writer) {
        outputHelp(new TabPrintWriter(writer, 2));
    }

    public void outputHelp(TabPrintWriter writer) {
        writer.print("RealType" + ":");
        writer.indent();

        writer.print(" ");
        writer.print("type = ");
        Type temp_type = getType();
        if (temp_type == null) {
            writer.print("null");
        } else {
            writer.print(temp_type);
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
            RealType casted = (RealType) obj;
            if (!(getType().equals(casted.getType()))) return false;
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
        code ^= getType().hashCode();
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
