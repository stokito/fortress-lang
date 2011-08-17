/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.parser_util.precedence_opexpr;

/**
 * An interface for visitors over Precedence that do not return a value.
 */
public interface PrecedenceVisitor_void {

    /**
     * Process an instance of None.
     */
    public void forNone(None that);

    /**
     * Process an instance of Higher.
     */
    public void forHigher(Higher that);

    /**
     * Process an instance of Lower.
     */
    public void forLower(Lower that);

    /**
     * Process an instance of Equal.
     */
    public void forEqual(Equal that);
}
