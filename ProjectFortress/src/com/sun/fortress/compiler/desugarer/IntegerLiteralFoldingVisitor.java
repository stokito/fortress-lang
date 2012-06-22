/*******************************************************************************
    Copyright 2012, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler.desugarer;

import com.sun.fortress.compiler.Types;
import com.sun.fortress.compiler.WellKnownNames;
import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.*;
import com.sun.fortress.useful.*;
import com.sun.fortress.exceptions.CompilerError;
import edu.rice.cs.plt.collect.IndexedRelation;
import edu.rice.cs.plt.collect.Relation;
import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.tuple.Option;
import edu.rice.cs.plt.tuple.Pair;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.math.BigInteger;
import static com.sun.fortress.exceptions.InterpreterBug.bug;

public class IntegerLiteralFoldingVisitor extends NodeUpdateVisitor {
	
	public IntegerLiteralFoldingVisitor() { }
	
	private BigInteger factorial_aux(BigInteger n, BigInteger acc) {
		if (n.compareTo(BigInteger.ZERO) == -1)
			throw new CompilerError("Integer Literal folding: Need to do something for factorial of negative values");
		if (n.equals(BigInteger.ZERO))
		   	return acc;
		else
			return factorial_aux(n.subtract(BigInteger.ONE),n.multiply(acc)); 
	}
	
	private BigInteger factorial(BigInteger n) {
		return factorial_aux(n,BigInteger.ONE);
	}
	
	private BigInteger choose(BigInteger n, BigInteger k) {
		BigInteger n_fact = factorial(n);
		BigInteger k_fact = factorial(k);
		BigInteger nminusk_fact = factorial(n.subtract(k));
		return n_fact.divide(k_fact.multiply(nminusk_fact));
	}
	
	private Node foldBinaryOperator(FunctionalRef op_result, ExprInfo info_result, Expr arg0, Expr arg1) {
		
		BigInteger v0 = ((IntLiteralExpr)arg0).getIntVal();
		BigInteger v1 = ((IntLiteralExpr)arg1).getIntVal();
		String op = op_result.toString();
		if (op.equals("+") || op.equals("\u229E") || op.equals("\u2214"))  // + BOXPLUS DOTPLUS 
			return ExprFactory.makeIntLiteralExpr(info_result,v0.add(v1));
		if (op.equals("-") || op.equals("\u229F") || op.equals("\u2238")) // - BOXMINUS DOTMINUS
			return ExprFactory.makeIntLiteralExpr(info_result,v0.subtract(v1));
		if (op.equals("BY") || op.equals("BOXCROSS") || op.equals("DOTCROSS") || op.equals("DOT") || op.equals("BOXDOT") || op.equals("juxtaposition"))
			return ExprFactory.makeIntLiteralExpr(info_result,v0.multiply(v1));
		if (op.equals("DIV") && v0.equals(BigInteger.ZERO) && v1.equals(BigInteger.ZERO)) 
			return ExprFactory.makeVarRef(info_result.getSpan(),WellKnownNames.compilerBuiltin(),WellKnownNames.UndefinedNumber());
		if (op.equals("DIV") && v0.compareTo(BigInteger.ZERO) == -1 && v1.equals(BigInteger.ZERO)) 
			return ExprFactory.makeVarRef(info_result.getSpan(),WellKnownNames.compilerBuiltin(),WellKnownNames.NegativeInfinity());
		if (op.equals("DIV") && v1.equals(BigInteger.ZERO)) 
			return ExprFactory.makeVarRef(info_result.getSpan(),WellKnownNames.compilerBuiltin(),WellKnownNames.Infinity());
		if (op.equals("DIV")) 
			return ExprFactory.makeIntLiteralExpr(info_result,v0.divide(v1));
		if (op.equals("AND"))
			return ExprFactory.makeIntLiteralExpr(info_result,v0.and(v1));
		if (op.equals("OR"))
			return ExprFactory.makeIntLiteralExpr(info_result,v0.or(v1));
		if (op.equals("XOR"))
			return ExprFactory.makeIntLiteralExpr(info_result,v0.xor(v1));
		if (op.equals("MIN"))
			return ExprFactory.makeIntLiteralExpr(info_result,v0.min(v1));
		if (op.equals("MAX"))
			return ExprFactory.makeIntLiteralExpr(info_result,v0.max(v1));
		if(op.equals("MINMAX"))
			return ExprFactory.makeTupleExpr(info_result.getSpan(), ExprFactory.makeIntLiteralExpr(NodeUtil.getSpan(arg0),v0.min(v1)),ExprFactory.makeIntLiteralExpr(NodeUtil.getSpan(arg1),v0.max(v1)));
		if(op.equals("CHOOSE"))
			return ExprFactory.makeIntLiteralExpr(info_result.getSpan(),choose(v0,v1));
		boolean lt = v0.compareTo(v1) == -1 ? true : false;
		boolean gt = v0.compareTo(v1) == 1 ? true : false; 
		boolean eq = v0.equals(v1);
		if (op.equals("<"))
			return ExprFactory.makeBooleanLiteralExpr(info_result,lt);
		if (op.equals("<="))
			return ExprFactory.makeBooleanLiteralExpr(info_result,lt || eq);
		if (op.equals(">"))
			return ExprFactory.makeBooleanLiteralExpr(info_result,gt);
		if (op.equals(">="))
			return ExprFactory.makeBooleanLiteralExpr(info_result,gt || eq);
		if (op.equals("=")) 
			return ExprFactory.makeBooleanLiteralExpr(info_result, eq);
		if (op.equals("NE"))
			return ExprFactory.makeBooleanLiteralExpr(info_result, lt || gt);
		List<Expr> args = Arrays.asList(arg0,arg1);
		return new OpExpr(info_result, op_result, args);
		
	}
	
	@Override
    public Node forChainExprOnly(ChainExpr that, ExprInfo info_result, Expr first_result, List<Link> links_result) {

		if (first_result instanceof IntLiteralExpr) {
			if (links_result.size() == 0) throw new CompilerError("The list of links is empty");
			Link arg = links_result.get(0);
			if (arg.getExpr() instanceof IntLiteralExpr) {
				Node folded = foldBinaryOperator(arg.getOp(),info_result,first_result,arg.getExpr());
				if (links_result.size() == 1)	
					return folded;
				else {
					if (folded instanceof BooleanLiteralExpr)	{
						if (((BooleanLiteralExpr) folded).getBooleanVal() == 0) 
							return ExprFactory.makeBooleanLiteralExpr(info_result,false);
						else {
							Expr e = links_result.remove(0).getExpr(); 
						    return forChainExprOnly(that,info_result,e,links_result);
						}
					}
					else 
						throw new CompilerError("One of the link in a chained expression produced something other than a boolean literal during folding.");
					
				}
			}	
			
		}
		
		if (that.getInfo() == info_result && that.getFirst() == first_result && that.getLinks() == links_result) return that;
        return new ChainExpr(info_result, first_result, links_result);
    }
    	
	@Override
	public Node forOpExprOnly(OpExpr that, ExprInfo info_result, FunctionalRef op_result, List<Expr> args_result) {
		
		if (args_result.size()==1) {    

			Expr arg = args_result.get(0);
			if (arg instanceof IntLiteralExpr) {
				
				BigInteger v = ((IntLiteralExpr)args_result.get(0)).getIntVal();
				String op = op_result.toString();
				if (op.equals("prefix -") || op.equals("prefix BOXMINUS") || op.equals("prefix DOTMINUS")) 
					return ExprFactory.makeIntLiteralExpr(info_result, v.negate());
				if (op.equals("|_|")) 
					return ExprFactory.makeIntLiteralExpr(info_result, v.abs());
				if (op.equals("prefix NOT"))
					return ExprFactory.makeIntLiteralExpr(info_result,v.not());
				
			}
			
		}	

		if (args_result.size() ==2) {
			
			Expr arg0 = args_result.get(0);
			Expr arg1 = args_result.get(1);
			if (arg0 instanceof IntLiteralExpr && arg1 instanceof IntLiteralExpr) 
				return foldBinaryOperator(op_result,info_result,arg0,arg1);
							
		}

		if (that.getInfo() == info_result && that.getOp() == op_result && that.getArgs() == args_result) return that;
		return new OpExpr(info_result, op_result, args_result);
	}

}


