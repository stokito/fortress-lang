package com.sun.fortress.nodes_util;

import com.sun.fortress.nodes.Node;

public class NodeSpanEquals<T extends Node> {

	private T node;

	NodeSpanEquals(T node) {
		this.node = node;
	}
	
    public boolean equals(Object other) {
        if (other == null) return false;
        if (!(other instanceof Node)) {
        	return false;
        }
        Node oNode = (Node) other;
        return (node.equals(other) && node.getSpan().equals(oNode.getSpan()));
    }
	
    public T getNode() {
    	return node;
    }
	
}
