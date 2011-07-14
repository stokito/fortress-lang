/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.parser_util.precedence_resolver;

import com.sun.fortress.nodes.Op;
import com.sun.fortress.parser_util.precedence_opexpr.PostfixOpExpr;
import com.sun.fortress.useful.PureList;


public class Layer extends EnclosingStack {
    private final Op op;
    private final EnclosingStack next;

    public Layer(Op _op, EnclosingStack _next) {
        super(EMPTY);
        op = _op;
        next = _next;
    }

    public Layer(Op _op, PureList<PostfixOpExpr> _list, EnclosingStack _next) {
        super(_list);
        op = _op;
        next = _next;
    }

    public Op getOp() {
        return op;
    }

    public EnclosingStack getNext() {
        return next;
    }
}
