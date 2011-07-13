/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.parser_util.precedence_opexpr;

/**
 * An abstract implementation of a visitor over Precedence that does not return a value.
 * * This visitor implements the visitor interface with methods that
 * * execute defaultCase.  These methods can be overriden
 * * in order to achieve different behavior for particular cases.
 */
public class PrecedenceAbstractVisitor_void implements PrecedenceVisitor_void {
    /* Methods to visit an item. */
    public void forNone(None that) {
        defaultCase(that);
    }

    public void forHigher(Higher that) {
        defaultCase(that);
    }

    public void forLower(Lower that) {
        defaultCase(that);
    }

    public void forEqual(Equal that) {
        defaultCase(that);
    }

    /**
     * This method is called by default from cases that do not
     * * override forCASEOnly.
     */
    protected void defaultCase(Precedence that) {
    }
}
