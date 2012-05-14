/*******************************************************************************
    Copyright 2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler

import com.sun.fortress.compiler.index.ComponentIndex
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
  var fndecl_list : List[FnHeader] = List()

  /** Walk the AST, recursively removing redundant declarations. */
  override def walk(node: Any) = node match {
    case c @ SComponent(info, name, imports, decls, comprises, is_native, exports) =>
      val new_decls = decls.foldRight(Nil.asInstanceOf[List[Decl]])((d,r) => desugarVar(d) ::: r)
      SComponent(info, name, imports, new_decls, comprises, is_native, exports)

    case t @ STraitDecl(t1, STraitTypeHeader(h1, h2, h3, h4, h5, contract, h7, params, decls),
                        t3, t4, t5, t6) =>
      val old_fndecl_list = fndecl_list
      val new_decls = decls.foldRight(Nil.asInstanceOf[List[Decl]])((d,r) => desugarVar(d) ::: r)
      fndecl_list = old_fndecl_list
      STraitDecl(t1, STraitTypeHeader(h1, h2, h3, h4, h5, contract, h7, params, new_decls),
                 t3, t4, t5, t6)

    case o @ SObjectDecl(o1, STraitTypeHeader(h1, h2, h3, h4, h5, contract, h7, params, decls), o3) =>
      val old_fndecl_list = fndecl_list
      val new_decls = decls.foldRight(Nil.asInstanceOf[List[Decl]])((d,r) => desugarVar(d) ::: r)
      fndecl_list = old_fndecl_list
      SObjectDecl(o1, STraitTypeHeader(h1, h2, h3, h4, h5, contract, h7, params, new_decls), o3)

    case b @ SBlock(info, loc, isAtomicBlock, isWithinDo, exprs) =>
      val old_fndecl_list = fndecl_list
      val new_exprs = exprs.foldRight(Nil.asInstanceOf[List[Expr]])((e,r) => desugarLocal(e) ::: r)
      fndecl_list = old_fndecl_list
      SBlock(info, walk(loc).asInstanceOf[Option[Expr]], isAtomicBlock, isWithinDo, new_exprs)

    case _ => super.walk(node)
  }

  def desugarVar(decl: Decl) : List[Decl] = decl match {
    case v @ SVarDecl(info, lhs, Some(init)) if (lhs.isEmpty && NU.isVoidExpr(init)) =>
      List()  
    // remove a duplicate FnDecl if any  
    case f@SFnDecl(info, h @ SFnHeader(sps, mods, name, where, throwsC, contract, params, returnType),
                   unambiguousname, body, implement) if(unambiguousname.getText == "Desugared") =>
      if(fndecl_list.exists(_.equals(h))) // in case of a duplicate, remove it.
        List()
      else {
        val span = NU.getSpan(f)
        val new_throwsC = throwsC match {
          case Some(ps) => Some(toJavaList(ps))
          case _ => None
        }
        val new_FnDecl = 
          NF.makeFnDecl(span, mods, name, toJavaList(sps), 
                        toJavaList(walk(params).asInstanceOf[List[Param]]), returnType, 
                        toJavaOption(new_throwsC), where, walk(contract).asInstanceOf[Option[Contract]], 
                        walk(body).asInstanceOf[Option[Expr]])
        fndecl_list = fndecl_list ::: List(h)
        List(new_FnDecl)
      }
    case _ => List(walk(decl).asInstanceOf[Decl])
  }

  def desugarLocal(exp: Expr): List[Expr] = exp match {
    case v @ SLocalVarDecl(info, body, lhs, Some(rhs)) if (lhs.isEmpty && NU.isVoidExpr(rhs) ) =>
      toList(body.getExprs)
    case f @ SLetFn(info, body, fndecl_list) =>
      val new_decls = fndecl_list.foldRight(Nil.asInstanceOf[List[Decl]])((d, r) => desugarVar(d) ::: r)
      List(SLetFn(info, walk(body).asInstanceOf[Block], new_decls.asInstanceOf[List[FnDecl]]))

    case _ => List(walk(exp).asInstanceOf[Expr])
  }

}
