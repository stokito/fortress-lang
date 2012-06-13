/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.parser_util.precedence_opexpr;

/**
 * A parametric abstract implementation of a visitor over Precedence that return a value.
 * * This visitor implements the visitor interface with methods that
 * * first visit children, and then call visitCASEOnly(), passing in
 * * the values of the visits of the children. (CASE is replaced by the case name.)
 */
public abstract class PrecedenceDepthFirstVisitor<RetType> implements PrecedenceVisitor<RetType> {
    protected abstract RetType[] makeArrayOfRetType(int len);

    /**
     * This method is called by default from cases that do not
     * override forCASEOnly.
     */
    protected abstract RetType defaultCase(Precedence that);

    /* Methods to visit an item. */
    public RetType forNoneOnly(None that) {
        return defaultCase(that);
    }

    public RetType forHigherOnly(Higher that) {
        return defaultCase(that);
    }

    public RetType forLowerOnly(Lower that) {
        return defaultCase(that);
    }

    public RetType forEqualOnly(Equal that) {
        return defaultCase(that);
    }


    /**
     * Implementation of PrecedenceVisitor methods to implement depth-first traversal.
     */
    public RetType forNone(None that) {
        return forNoneOnly(that);
    }

    public RetType forHigher(Higher that) {
        return forHigherOnly(that);
    }

    public RetType forLower(Lower that) {
        return forLowerOnly(that);
    }

    public RetType forEqual(Equal that) {
        return forEqualOnly(that);
    }

}
