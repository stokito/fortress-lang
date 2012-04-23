/*******************************************************************************
    Copyright 2012, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler.desugarer;

import java.util.ArrayList;
import java.util.List;

import com.sun.fortress.Shell;
import com.sun.fortress.nodes.Block;
import com.sun.fortress.nodes.TypecaseClause;
import com.sun.fortress.nodes.Typecase;
import com.sun.fortress.nodes.Expr;
import com.sun.fortress.nodes.Throw;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.NodeUpdateVisitor;
import com.sun.fortress.nodes_util.ExprFactory;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.scala_src.typechecker.STypeChecker;
import com.sun.fortress.compiler.WellKnownNames;

import edu.rice.cs.plt.tuple.Option;




public class TypecaseExprDesugarer extends NodeUpdateVisitor {
	
    public TypecaseExprDesugarer() {}
    

    @Override
    public Node forTypecase(Typecase x) {
    	    	
    	if (x.getElseClause().isSome()) {return x;}
    	else {
    		Expr e = ExprFactory.makeThrow(NodeFactory.desugarerSpan, "MatchFailure");
    		Block b = ExprFactory.makeBlock(e);
    		Option<Block> o = Option.some(b);
    		return new Typecase(x.getInfo(),x.getBindExpr(),x.getClauses(),o);    		
    	}
	
}
	
}