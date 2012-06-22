/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.parser_util.precedence_opexpr;

/**
 * A parametric interface for visitors over Precedence that return a value.
 */
public interface PrecedenceVisitor<RetType> {

    /**
     * Process an instance of None.
     */
    public RetType forNone(None that);

    /**
     * Process an instance of Higher.
     */
    public RetType forHigher(Higher that);

    /**
     * Process an instance of Lower.
     */
    public RetType forLower(Lower that);

    /**
     * Process an instance of Equal.
     */
    public RetType forEqual(Equal that);
}
