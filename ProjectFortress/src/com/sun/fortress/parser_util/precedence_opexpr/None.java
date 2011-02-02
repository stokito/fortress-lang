/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.parser_util.precedence_opexpr;


/**
 * Class None, a component of the Precedence composite hierarchy.
 * Note: null is not allowed as a value for any field.
 */
public class None extends Object implements Precedence {
    private int _hashCode;
    private boolean _hasHashCode = false;

    /**
     * Constructs a None.
     *
     * @throws java.lang.IllegalArgumentException
     *          if any parameter to the constructor is null.
     */
    public None() {
        super();
    }


    public <RetType> RetType accept(PrecedenceVisitor<RetType> visitor) {
        return visitor.forNone(this);
    }

    public void accept(PrecedenceVisitor_void visitor) {
        visitor.forNone(this);
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
        writer.print("None" + ":");
        writer.indent();
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
