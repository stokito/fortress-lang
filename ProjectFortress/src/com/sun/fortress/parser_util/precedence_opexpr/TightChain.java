/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.parser_util.precedence_opexpr;

import com.sun.fortress.parser_util.precedence_resolver.ExprOpPair;
import com.sun.fortress.useful.PureList;


/**
 * Class TightChain, a component of the InfixFrame composite hierarchy.
 * Note: null is not allowed as a value for any field.
 */
public class TightChain extends Chain {

    /**
     * Constructs a TightChain.
     *
     * @throws java.lang.IllegalArgumentException
     *          if any parameter to the constructor is null.
     */
    public TightChain(PureList<ExprOpPair> in_links) {
        super(in_links);
    }


    public <RetType> RetType accept(InfixFrameVisitor<RetType> visitor) {
        return visitor.forTightChain(this);
    }

    public void accept(InfixFrameVisitor_void visitor) {
        visitor.forTightChain(this);
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
        writer.print("TightChain" + ":");
        writer.indent();

        writer.print(" ");
        writer.print("links = ");
        PureList<ExprOpPair> temp_links = getLinks();
        if (temp_links == null) {
            writer.print("null");
        } else {
            writer.print(temp_links);
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
            TightChain casted = (TightChain) obj;
            if (!(getLinks().equals(casted.getLinks()))) return false;
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
        code ^= getLinks().hashCode();
        return code;
    }
}
