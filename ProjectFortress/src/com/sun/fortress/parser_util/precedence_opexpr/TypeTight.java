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
 * Class Tight, a component of the InfixFrame composite hierarchy.
 * Note: null is not allowed as a value for any field.
 */
public class TypeTight extends TypeInfixFrame {

    /**
     * Constructs a TypeTight.
     *
     * @throws java.lang.IllegalArgumentException
     *          if any parameter to the constructor is null.
     */
    public TypeTight(Op in_op, Effect in_effect, Type in_arg) {
        super(in_op, in_effect, in_arg);
    }


    public <RetType> RetType accept(InfixFrameVisitor<RetType> visitor) {
        return visitor.forTypeTight(this);
    }

    public void accept(InfixFrameVisitor_void visitor) {
        visitor.forTypeTight(this);
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
        writer.print("TypeTight" + ":");
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
            TypeTight casted = (TypeTight) obj;
            if (!(getOp().equals(casted.getOp()))) return false;
            if (!(getEffect().equals(casted.getEffect()))) return false;
            return true;
        }
    }

    /**
     * Implementation of hashCode that is consistent with
     * equals. The value of the hashCode is formed by
     * XORing the hashcode of the class object with
     * the hashcodes of all the fields of the object.
     */
    protected int generateHashCode() {
        int code = getClass().hashCode();
        code ^= getOp().hashCode();
        code ^= getEffect().hashCode();
        return code;
    }
}
