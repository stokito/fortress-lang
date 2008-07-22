/*******************************************************************************
    Copyright 2008 Sun Microsystems, Inc.,
    4150 Network Circle, Santa Clara, California 95054, U.S.A.
    All rights reserved.

    U.S. Government Rights - Commercial software.
    Government users are subject to the Sun Microsystems, Inc. standard
    license agreement and applicable provisions of the FAR and its supplements.

    Use is subject to license terms.

    This distribution may include materials developed by third parties.

    Sun, Sun Microsystems, the Sun logo and Java are trademarks or registered
    trademarks of Sun Microsystems, Inc. in the U.S. and other countries.
 ******************************************************************************/

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
