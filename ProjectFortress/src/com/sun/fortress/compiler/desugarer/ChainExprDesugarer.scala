/*******************************************************************************
    Copyright 2009 Sun Microsystems, Inc.,
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

package com.sun.fortress.compiler.desugarer

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

import _root_.java.util.Arrays

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
    case SChainExpr(v1, first, links, v2) =>
      val newFirst = walk(first).asInstanceOf[Expr]
      val newLinks = links.map(walk(_).asInstanceOf[Link])
      val newChain = SChainExpr(v1, newFirst, newLinks, v2)
      desugarChainExpr(newChain)
    case _ => super.walk(node)
  }

  /** Desugar the given ChainExpr. */
  def desugarChainExpr(e: ChainExpr): Expr = {
    val SChainExpr(info, first, links, andOp) = e 

    // Recursively build up the nested Do's for each link. 
    def recur(lastVar: VarRef, links: List[Link]): Expr = {
      
      // Peel the next link off.
      val (SLink(_, nextFn, nextExpr) :: tailLinks) = links
      
      // Declare a temp var for the next expr.
      val nextVar = naming.makeVarRef(nextExpr)
      val linkSpan = NU.spanTwo(lastVar, nextVar)
      
      // Make the OpExpr.
      val opExpr = EF.makeOpExpr(linkSpan,
                                 BOOLEAN,
                                 nextFn,
                                 lastVar,
                                 nextVar)
      
      // If there are no more links, that's it.
      val bodyExpr = if (tailLinks.isEmpty) {
        opExpr
      } else {
        // If another link, make the conjunct.
        EF.makeOpExpr(linkSpan,
                      BOOLEAN,
                      andOp,
                      opExpr,
                      recur(nextVar, tailLinks))
      }
      
      // Make the LocalVarDecl and wrap it in a Do.
      val decl = TempVarDecl(nextVar, nextExpr).makeLocalVarDecl(bodyExpr)
      EF.makeDo(linkSpan, BOOLEAN, decl)
    }
    
    // If there's only one link, just create the OpExpr.
    val desugared = links match {
      case List(SLink(nextInfo, nextFn, nextExpr)) =>
        EF.makeOpExpr(NU.spanTwo(NU.getSpan(first), nextInfo.getSpan),
                      BOOLEAN,
                      nextFn,
                      first,
                      nextExpr)
        
      case _ =>
        // Declare a temp var for the first expr and wrap its decl around the
        // whole desugared expr for the remaining links.
        val firstVar = naming.makeVarRef(first)
        val desugaredLinkExpr = recur(firstVar, links)
        val decl = TempVarDecl(firstVar, first).makeLocalVarDecl(desugaredLinkExpr)
        EF.makeDo(info.getSpan, BOOLEAN, decl)
    }
    
    // Set parenthesized depending on if the ChainExpr was.
    setParenthesized(desugared, NU.isParenthesized(e))
  }
}