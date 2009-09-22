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

// This should be uncommented and used once LocalVarDecls allow for multiple
// names on the LHS and a tuple on the RHS.

//  /** Desugar the given ChainExpr. */
//  def desugarChainExpr(e: ChainExpr): Expr = {
//    val SChainExpr(info, first, links, _) = e
//    val andOp = addSpan(e.getAndOp, info.getSpan()).asInstanceOf[FunctionalRef]
//    
//    // Recursively desugar the constituent expressions.
//    val newFirst = walk(first).asInstanceOf[Expr]
//    val newLinks = links.map(walk(_).asInstanceOf[Link])
//    val linkExprs = newLinks.map(_.getExpr)
//    
//    // Create the decl to bind new vars to all exprs.
//    val firstVar = naming.makeVarRef(newFirst)
//    val linkVars = linkExprs.map(naming.makeVarRef)
//    val allVars = firstVar :: linkVars
//    val decl = TempVarDecl(allVars, newFirst :: linkExprs)
//    
//    // Create the conjuncts.
//    val varPairs = allVars.init zip allVars.tail
//    val conjuncts = List.map2(newLinks, varPairs) {
//      case (SLink(_, op, _), (left, right)) =>
//        EF.makeOpExpr(NU.spanTwo(left, right), BOOLEAN, op, left, right)
//    }
//    
//    // Build up the conjunction.
//    val conjunction = conjuncts.reduceLeft { (lhs: Expr, next: Expr) =>
//      EF.makeOpExpr(NU.spanTwo(lhs, next), BOOLEAN, andOp, lhs, next)
//    }
//    
//    // Wrap a declaring do block around the conjunction.
//    val block = decl.makeLocalVarDeclDo(NU.getSpan(e), conjunction)
//    
//    // Set parenthesized depending on if the ChainExpr was.
//    setParenthesized(block, NU.isParenthesized(e))
//  }

  /** Desugar the given ChainExpr. */
  def desugarChainExpr(e: ChainExpr): Expr = {
    val SChainExpr(info, first, links, _) = e
    val andOp = addSpan(e.getAndOp, info.getSpan()).asInstanceOf[FunctionalRef]
    
    // Recursively desugar the constituent expressions.
    val newFirst = walk(first).asInstanceOf[Expr]
    val newLinks = links.map(walk(_).asInstanceOf[Link])
    val linkExprs = newLinks.map(_.getExpr)

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
      val decl = TempVarDecl(nextVar, nextExpr).makeLocalVarDecl(NU.getSpan(e), bodyExpr)
      EF.makeDo(NU.getSpan(e), BOOLEAN, decl)
    }
    
    // If there's only one link, just create the OpExpr.
    val desugared = newLinks match {
      case List(SLink(nextInfo, nextFn, nextExpr)) =>
        EF.makeOpExpr(NU.spanTwo(NU.getSpan(first), nextInfo.getSpan),
                      BOOLEAN,
                      nextFn,
                      newFirst,
                      nextExpr)
        
      case _ =>
        // Declare a temp var for the first expr and wrap its decl around the
        // whole desugared expr for the remaining links.
        val firstVar = naming.makeVarRef(first)
        val desugaredLinkExpr = recur(firstVar, newLinks)
        val decl = TempVarDecl(firstVar, newFirst).makeLocalVarDecl(NU.getSpan(e), desugaredLinkExpr)
        EF.makeDo(info.getSpan, BOOLEAN, decl)
    }
    
    // Set parenthesized depending on if the ChainExpr was.
    setParenthesized(desugared, NU.isParenthesized(e))
  }

}