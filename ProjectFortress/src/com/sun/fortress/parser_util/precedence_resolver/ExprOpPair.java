/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.parser_util.precedence_resolver;

import com.sun.fortress.nodes.Expr;
import com.sun.fortress.nodes.Op;
import com.sun.fortress.useful.Pair;


public class ExprOpPair extends Pair<Expr, Op> {
    ExprOpPair(Expr _expr, Op _op) {
        super(_expr, _op);
    }
}
