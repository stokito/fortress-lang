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
 * * return the value of the defaultCase.  These methods can be overriden
 * * in order to achieve different behavior for particular cases.
 */
public abstract class PrecedenceAbstractVisitor<RetType> implements PrecedenceVisitor<RetType> {
    /**
     * This method is run for all cases by default, unless they are overridden by subclasses.
     */
    protected abstract RetType defaultCase(Precedence that);

    /* Methods to visit an item. */
    public RetType forNone(None that) {
        return defaultCase(that);
    }

    public RetType forHigher(Higher that) {
        return defaultCase(that);
    }

    public RetType forLower(Lower that) {
        return defaultCase(that);
    }

    public RetType forEqual(Equal that) {
        return defaultCase(that);
    }


}
