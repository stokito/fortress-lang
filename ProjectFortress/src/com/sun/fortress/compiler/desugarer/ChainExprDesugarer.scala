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
 */
class ChainExprDesugarer extends Walker {

  /** Oracle used for generating names. */
  private val naming = new NameOracle(this)

  /** Java Option of boolean type. */
  private val BOOLEAN = some[Type](Types.BOOLEAN)

  /** Walk the AST, recursively desugaring any ChainExprs. */
  override def walk(node: Any) = node match {
    case e:ChainExpr => desugarChainExpr(e)
    case _ => super.walk(node)
  }

  /** Desugar the given ChainExpr. */
  def desugarChainExpr(e: ChainExpr): Expr = {
    val SChainExpr(info, first, links, _) = e
    val andOp = addSpan(e.getAndOp, info.getSpan()).asInstanceOf[FunctionalRef]

    // Recursively desugar the constituent expressions.
    val newFirst = walk(first).asInstanceOf[Expr]
    val newLinks = links.map(walk(_).asInstanceOf[Link])

    // Common case: 1 operator.  Written rather imperatively
    // to bail out without complicating the rest.
    newLinks match {
        case SLink(info, op, expr) :: Nil =>
            var res = EF.makeOpExpr(NU.getSpan(e), BOOLEAN, op, newFirst, expr);
            return setParenthesized(res, NU.isParenthesized(e));
        case _ => ()
    }

    // Create the decl to bind new vars to all exprs.
    val linkExprs = newLinks.map(_.getExpr)
    val firstVar = naming.makeVarRef(newFirst)
    val linkVars = linkExprs.map(naming.makeVarRef)
    val allVars = firstVar :: linkVars
    val decl = TempVarDecl(allVars, newFirst :: linkExprs)

    // Create the conjuncts.
    val conjuncts = (allVars, newLinks, linkVars).zipped.map {
      case (left, SLink(_, op, _), right) =>
        setParenthesized(EF.makeOpExpr(NU.spanTwo(left, right), BOOLEAN, op, left, right), true)
    }

    // Build up the conjunction.
    val conjunction = conjuncts.reduceLeft { (lhs: Expr, next: Expr) =>
      setParenthesized(EF.makeOpExpr(NU.spanTwo(lhs, next), BOOLEAN, andOp, lhs, next), true)
    }

    // Wrap a declaring do block around the conjunction.
    val block = decl.makeLocalVarDeclDo(NU.getSpan(e), conjunction)

    // System.err.println("desugarChainExpr:\n"+e.toStringReadable()+"\nto:\n"+block.toStringReadable());

    // Set parenthesized depending on if the ChainExpr was.
    setParenthesized(block, NU.isParenthesized(e))
  }

}
