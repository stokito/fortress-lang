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
import com.sun.fortress.nodes.CaseClause;
import com.sun.fortress.nodes.CaseExpr;
import com.sun.fortress.nodes.Expr;
import com.sun.fortress.nodes.FunctionalRef;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.If;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.NodeUpdateVisitor;
import com.sun.fortress.nodes_util.ExprFactory;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.scala_src.typechecker.STypeChecker;

import edu.rice.cs.plt.tuple.Option;




public class CaseExprDesugarer extends NodeUpdateVisitor {

    private STypeChecker typeChecker;
	
    public CaseExprDesugarer(STypeChecker _typeChecker) {
    	
    	typeChecker = _typeChecker;
    	
    }
    
    private Block forCaseClauses(Expr e1, List<CaseClause> l, Option<Block> def) {
    	
    	int size = l.size();
	
    	if (size == 0) 
    		throw new Error("Desugarer found no case clauses in a case expression, please report.");
		
	
    	CaseClause c = l.get(0);
    	l.remove(0);    	
    	
    	FunctionalRef op = c.getOp().unwrap();

    	
    	Expr e2 = c.getMatchClause();
    	List<Expr> tuple = new ArrayList<Expr>();
    	tuple.add(e1);
    	tuple.add(e2);
    	Expr arg = ExprFactory.makeTupleExpr(NodeFactory.desugarerSpan, tuple);
    	Expr cond = ExprFactory.make_RewriteFnApp(op, arg);
    	If i;
	
    	if (size > 1) {
    		i = ExprFactory.makeIf(NodeFactory.desugarerSpan, cond, c.getBody(), forCaseClauses(e1,l,def));
    	} else if (def.isSome()) {
    		i = ExprFactory.makeIf(NodeFactory.desugarerSpan, cond, c.getBody(), def.unwrap());
    	} else {
    		Expr ex = ExprFactory.makeThrow(NodeFactory.desugarerSpan, "MatchFailure");
    		i = ExprFactory.makeIf(NodeFactory.desugarerSpan, cond, c.getBody(),ExprFactory.makeBlock(ex));
    	}
	    	
    	return ExprFactory.makeBlock(i);

    }

    /*
     * Things to know about the desugaring of case expressions:
     * 1. It must take place *after type checking*, and the operator for individual case clauses must have been set;
     * 2. It must takes place *before* desugaring of coercion;
     * 3. It calls the typecheckers that are handed by the type checking phase. We are assuming that it is fine to
     * 	  reuse the typechecker in this context. This may be a bad assumption.
     */
    @Override
    public Node forCaseExpr(CaseExpr x) {
    	    	
    	if (x.getParam().isNone())
    		throw new Error("Desugaring failed for case expression. The case expression has no conditional expression. Please report.");

    	NameOracle naming = new NameOracle(this);
    	Id fresh = naming.makeId();
	    	    	//x.getParam().unwrap().getInfo().getExprType().unwrap()
    	Block cascade = forCaseClauses(ExprFactory.makeVarRef(NodeFactory.desugarerSpan,fresh),x.getClauses(),x.getElseClause());
	
    	Expr flat_case = ExprFactory.makeLocalVarDecl(fresh ,x.getParam().unwrap(), cascade);
		
    	return 	typeChecker.checkExpr(flat_case);
	
}
	
}