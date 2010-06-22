/*******************************************************************************
    Copyright 2010 Sun Microsystems, Inc.,
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

package com.sun.fortress.compiler

import com.sun.fortress.compiler.index.ComponentIndex
import com.sun.fortress.exceptions.InterpreterBug.bug
import com.sun.fortress.nodes._
import com.sun.fortress.nodes_util.{DesugarerUtil => DU}
import com.sun.fortress.nodes_util.{ExprFactory => EF}
import com.sun.fortress.nodes_util.Modifiers
import com.sun.fortress.nodes_util.{NodeFactory => NF}
import com.sun.fortress.nodes_util.{NodeUtil => NU}
import com.sun.fortress.scala_src.nodes._
import com.sun.fortress.scala_src.useful.Lists._
import com.sun.fortress.scala_src.useful.Options._
import com.sun.fortress.useful.Useful

/**
 * Desugars all patterns.
 */
class PatternMatchingDesugaredSimplifier(component: Component) extends Walker {

  def simplifier() = walk(component)

  /** Walk the AST, recursively removing redundant declarations. */
  override def walk(node: Any) = node match {

    case c @ SComponent(info, name, imports, decls, comprises, is_native, exports) =>
      val new_decls = decls.foldRight(Nil.asInstanceOf[List[Decl]])((d,r) => desugarVar(d) ::: r)
      SComponent(info, name, imports, new_decls, comprises, is_native, exports)

    case t @ STraitDecl(t1, STraitTypeHeader(h1, h2, h3, h4, h5, contract, h7, params, decls),
                        t3, t4, t5, t6) =>
      val new_decls = decls.foldRight(Nil.asInstanceOf[List[Decl]])((d,r) => desugarVar(d) ::: r)
      STraitDecl(t1, STraitTypeHeader(h1, h2, h3, h4, h5, contract, h7, params, new_decls),
                 t3, t4, t5, t6)

    case o @ SObjectDecl(o1, STraitTypeHeader(h1, h2, h3, h4, h5, contract, h7, params, decls), o3) =>
      val new_decls = decls.foldRight(Nil.asInstanceOf[List[Decl]])((d,r) => desugarVar(d) ::: r)
      SObjectDecl(o1, STraitTypeHeader(h1, h2, h3, h4, h5, contract, h7, params, new_decls), o3)

    case b @ SBlock(info, loc, isAtomicBlock, isWithinDo, exprs) =>
      val new_exprs = exprs.foldRight(Nil.asInstanceOf[List[Expr]])((e,r) => desugarLocal(e) ::: r)
      SBlock(info, walk(loc).asInstanceOf[Option[Expr]], isAtomicBlock, isWithinDo, new_exprs)

    case _ => super.walk(node)
  }

  def desugarVar(decl: Decl) : List[Decl] = decl match {
    case v @ SVarDecl(info, lhs, Some(init)) if (lhs.isEmpty && NU.isVoidExpr(init)) =>
      List()
    case _ => List(walk(decl).asInstanceOf[Decl])
  }

  def desugarLocal(exp: Expr): List[Expr] = exp match {
    case v @ SLocalVarDecl(info, body, lhs, Some(rhs)) if (lhs.isEmpty && NU.isVoidExpr(rhs) ) =>
      toList(body.getExprs)
    case _ => List(walk(exp).asInstanceOf[Expr])
  }

}
