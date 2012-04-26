/*******************************************************************************
    Copyright 2012, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler.desugarer;

import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.NodeUpdateVisitor;
import com.sun.fortress.nodes.AsExpr;

public class TypeAscriptionDesugarer extends NodeUpdateVisitor {
	
	@Override
	public Node forAsExpr(AsExpr x) {
		
		return x.getExpr();
		
	}
	
}