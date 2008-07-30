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

package com.sun.fortress.compiler.desugarer;

import static com.sun.fortress.exceptions.InterpreterBug.bug;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.sun.fortress.nodes.AmbiguousMultifixOpExpr;
import com.sun.fortress.nodes.Expr;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.NodeUpdateVisitor;
import com.sun.fortress.nodes.Op;
import com.sun.fortress.nodes.OpExpr;
import com.sun.fortress.nodes.OpName;
import com.sun.fortress.nodes.OpRef;
import com.sun.fortress.nodes.Param;
import com.sun.fortress.nodes_util.ExprFactory;
import com.sun.fortress.nodes_util.OprUtil;
import com.sun.fortress.nodes_util.Span;

import edu.rice.cs.plt.iter.IterUtil;

/**
 * Desugar conditional operators into operators that take thunks.
 * This desugaring is described in section 22.8 of the specification.
 * We find {@code e_1 AND: e_2}, for example, and change it into
 * {@code e_1 AND (fn () => e_2)}, for which an overloading must exist.
 * This desugaring must go before disambiguation, and is therefore called
 * by {@code PreDisambiguationDesugarer}.
 */
public class ConditionalOpDesugarer extends NodeUpdateVisitor {

	

	@Override
	public Node forAmbiguousMultifixOpExpr(AmbiguousMultifixOpExpr that) {
		// If there is a colon at all, the operator is no longer ambiguous:
		// It must be infix.
		OpName op_name = IterUtil.first(that.getInfix_op().getOps());
		boolean prefix = OprUtil.hasPrefixColon(op_name);
		boolean suffix = OprUtil.hasSuffixColon(op_name);
		
		if( prefix || suffix ) {
			OpExpr new_op = new OpExpr(that.getSpan(), that.isParenthesized(), that.getInfix_op(), that.getArgs());
			return recur(new_op);
		}
		else {
			return super.forAmbiguousMultifixOpExpr(that);
		}
	}

	@Override
	public Node forOpExpr(OpExpr that) {
        OpRef op_result = (OpRef) recur(that.getOp());
        List<Expr> args_result = recurOnListOfExpr(that.getArgs());
        
        OpExpr new_op;
        if( op_result == that.getOp() && args_result == that.getArgs() ) {
        	new_op = that;
        }
        else {
        	new_op = new OpExpr(that.getSpan(), that.isParenthesized(), op_result, args_result);
        }
        return cleanupOpExpr(new_op);
	}

    private static Expr thunk(Expr e) {
        return ExprFactory.makeFnExpr(e.getSpan(),
                                      Collections.<Param>emptyList(), e);
    }
    
	private Expr cleanupOpExpr(OpExpr opExp) {
		OpRef ref = opExp.getOp();

		List<Expr> args = opExp.getArgs();

		if (args.size() <= 1) return opExp;
		OpName qop = IterUtil.first(ref.getOps());

		if (OprUtil.isEnclosing(qop)) return opExp;
		if (OprUtil.isUnknownFixity(qop))
			return bug(opExp, "The operator fixity is unknown: " +
					((Op)qop).getText());
		boolean prefix = OprUtil.hasPrefixColon(qop);
		boolean suffix = OprUtil.hasSuffixColon(qop);
		if (!prefix && !suffix) return opExp;
		qop = OprUtil.noColon(qop);
		Iterator<Expr> i = args.iterator();
		Expr res = i.next();
		Span sp = opExp.getSpan();
		while (i.hasNext()) {
			Expr arg = (Expr)i.next();
			if (prefix) {
				res = thunk(res);
			}
			if (suffix) {
				arg = thunk(arg);
			}
			res = ExprFactory.makeOpExpr(sp,qop,res,arg);
		}
		return res;
	}

}