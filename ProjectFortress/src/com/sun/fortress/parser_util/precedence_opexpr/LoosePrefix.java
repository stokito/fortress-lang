/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.parser_util.precedence_opexpr;

import com.sun.fortress.nodes.Op;

/**
 * Class LoosePrefix, a component of the OpExpr composite hierarchy.
 * Note: null is not allowed as a value for any field.
 */
public class LoosePrefix extends Prefix implements PrefixOpExpr {

    /**
     * Constructs a LoosePrefix.
     *
     * @throws java.lang.IllegalArgumentException
     *          if any parameter to the constructor is null.
     */
    public LoosePrefix(Op in_op) {
        super(in_op);
    }
}
