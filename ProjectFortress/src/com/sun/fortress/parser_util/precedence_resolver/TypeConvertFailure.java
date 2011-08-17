/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.parser_util.precedence_resolver;

import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.Span;

public class TypeConvertFailure extends Exception {
    Span span;

    public TypeConvertFailure(Span in_span, String message) {
        super(in_span.getBegin().at() + ": " + message);
        span = in_span;
    }

    public TypeConvertFailure(String message) {
        super(message);
        span = NodeFactory.parserSpan;
    }

    public Span getSpan() {
        return span;
    }
}
