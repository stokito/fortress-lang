/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.nodes_util;

import com.sun.fortress.nodes.ASTNode;

public class NodeSpanEquals<T extends ASTNode> {

	private T node;

	NodeSpanEquals(T node) {
		this.node = node;
	}

    public boolean equals(Object other) {
        if (other == null) return false;
        if (!(other instanceof ASTNode)) {
        	return false;
        }
        ASTNode oNode = (ASTNode) other;
        return (node.equals(other) && NodeUtil.getSpan(node).equals(NodeUtil.getSpan(oNode)));
    }

    public int hashCode() {
        return super.hashCode();
    }

    public T getNode() {
    	return node;
    }

}
