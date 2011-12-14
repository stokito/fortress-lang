/*******************************************************************************
    Copyright 2009,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler.desugarer

import com.sun.fortress.compiler.GlobalEnvironment
import com.sun.fortress.compiler.Types
import com.sun.fortress.exceptions.InterpreterBug.bug
import com.sun.fortress.nodes._
import com.sun.fortress.nodes_util.{ExprFactory => EF}
import com.sun.fortress.nodes_util.{NodeFactory => NF}
import com.sun.fortress.nodes_util.{NodeUtil => NU}
import com.sun.fortress.nodes_util.Span
import com.sun.fortress.scala_src.nodes._
import com.sun.fortress.scala_src.useful.Lists._
import com.sun.fortress.scala_src.useful.Options._
import com.sun.fortress.scala_src.useful.SExprUtil._
import com.sun.fortress.scala_src.useful.STypesUtil._

/**
 * Desugars all ChainExprs into compound OpExprs.
 *
 * Example: `a < b = c <= d < e` desugars into:
 *   do
 *      (t1, t2, t3, t4, t5) = (a, b, c, d, e)
 *      (((t1 < t2) AND (t2 = t3)) AND (t3 = t4)) AND (t4 < t5)
 *   end
 */
class ChainExprDesugarer extends Walker {

  /** Oracle used for generating names. */
  private val naming = new NameOracle(this)

  /** Java Option of boolean type. */
  private val BOOLEAN = some[Type](Types.BOOLEAN)

  /** Walk the AST, recursively desugaring any ChainExpr nodes. */
  override def walk(node: Any) = node match {
    case SChainExpr(info, first, links, andop) => 
        // Recursively desugar the constituent expressions.
	val recurredFirst = walk(first).asInstanceOf[Expr]
        val recurredLinks = links.map(walk(_).asInstanceOf[Link])
	desugarChainExpr(SChainExpr(info, recurredFirst, recurredLinks, andop))
    case _ => super.walk(node)
  }

  /** Desugar the given ChainExpr. */
  def desugarChainExpr(ce: ChainExpr): Expr = {
    val SChainExpr(info, first, links, _) = ce
    val andOp = addSpan(ce.getAndOp, info.getSpan()).asInstanceOf[FunctionalRef]

    val result = links match {
        case SLink(info, op, expr) :: Nil =>
	    // Common case: just 1 operator, so no temporary variables needed.
            EF.makeOpExpr(NU.getSpan(ce), BOOLEAN, op, first, expr);
        case _ =>
	    // Create the decl to bind new vars to all exprs.
	    val linkExprs = links.map(_.getExpr)
	    val firstVar = naming.makeVarRef(first)
	    val linkVars = linkExprs.map(naming.makeVarRef)
	    val allVars = firstVar :: linkVars
	    val decl = TempVarDecl(allVars, first :: linkExprs)

	    // Create the conjuncts.
	    val conjuncts = (allVars, links, linkVars).zipped.map {
	      case (left, SLink(_, op, _), right) =>
		setParenthesized(EF.makeOpExpr(NU.spanTwo(left, right), BOOLEAN, op, left, right), true)
	    }

	    // Build up the conjunction.
	    val conjunction = conjuncts.reduceLeft { (lhs: Expr, next: Expr) =>
	      setParenthesized(EF.makeOpExpr(NU.spanTwo(lhs, next), BOOLEAN, andOp, lhs, next), true)
	    }

	    // Wrap a declaring do block around the conjunction.
	    val block = decl.makeLocalVarDeclDo(NU.getSpan(ce), conjunction)
	    // System.err.println("desugarChainExpr:\n"+ce.toStringReadable()+"\nto:\n"+block.toStringReadable());
	    block
    }
    // Set parenthesized depending on if the ChainExpr was.
    setParenthesized(result, NU.isParenthesized(ce))
  }

}
