/*******************************************************************************
    Copyright 2008, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.exceptions;

import com.sun.fortress.nodes_util.Span;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.nodes.ASTNode;
import static com.sun.fortress.exceptions.InterpreterBug.bug;

public class MacroError extends CompilerError {

    /**
     * Make Eclipse happy
     */
    private static final long serialVersionUID = 4547207829531871269L;

    public MacroError(String msg) {
        super(msg);
    }

    public MacroError(String msg, Exception e) {
        super(msg, e);
    }

    public MacroError(Exception e) {
        super(e);
    }

    public MacroError(Span span, String msg) {
        super(span, msg);
    }

    public MacroError(ASTNode node, String msg) {
        super(NodeUtil.getSpan(node), msg);
    }
}
