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
class PatternMatchingDesugarer(component: ComponentIndex,
                               globalEnv: GlobalEnvironment) extends Walker {

  val typeConses = component.typeConses
  def desugar() = walk(component.ast)

  /** Walk the AST, recursively desugaring any patterns. */
  override def walk(node: Any) = node match {

    case c @ SComponent(info, name, imports, decls, comprises, is_native, exports) =>
      val new_decls = decls.foldRight(Nil.asInstanceOf[List[Decl]])((d,r) => desugarVar(d) ::: r)
      SComponent(info, name, imports, new_decls, comprises, is_native, exports)

    // Desugars trait value parameters as abstract fields
    case t @ STraitDecl(t1, STraitTypeHeader(h1, h2, h3, h4, h5, h6, h7, params, decls),
                        t3, t4, t5, t6) =>
      val new_decls = params match {
        case Some(ps) => ps.map(paramToDecl) ::: decls
        case _ => decls
      }
      STraitDecl(t1, STraitTypeHeader(h1, h2, h3, h4, h5, h6, h7,
                                      None, new_decls.map(walk(_).asInstanceOf[Decl])), t3, t4, t5, t6)

    // Desugars patterns in local variable declarations
    case b @ SBlock(info, loc, isAtomicBlock, isWithinDo, exprs) =>
      val new_exprs = exprs.foldRight(Nil.asInstanceOf[List[Expr]])((e,r) => desugarLocal(e) :: r)
      SBlock(info, walk(loc).asInstanceOf[Option[Expr]], isAtomicBlock, isWithinDo, new_exprs)

    // Desugars patterns in function expressions
    case SFnExpr(info,
                 SFnHeader(sps, mods, name, where, throwsC, contract, params, returnType),
                 body) =>
      val desugaredParams = params.map(desugarParam)
      val left = desugaredParams.map(_._2).flatten
      val new_body = walk(body).asInstanceOf[Expr]
      if (left.isEmpty)
        SFnExpr(info, SFnHeader(sps, mods, name, where, throwsC,
                                walk(contract).asInstanceOf[Option[Contract]],
                                walk(params).asInstanceOf[List[Param]], returnType),
                new_body)
      else {
        val right = EF.makeMaybeTupleExpr(NU.getSpan(body),
                                          toJavaList(desugaredParams.map(_._3).flatten))
        val desugared = SBlock(info, None, false, false, List(new_body))
        val new_decl = SLocalVarDecl(info, desugared, left, Some(right))
        val result = SFnExpr(info, SFnHeader(sps, mods, name, where, throwsC,
                                             walk(contract).asInstanceOf[Option[Contract]],
                                             desugaredParams.map(_._1), returnType),
                             SBlock(info, None, false, false, List(new_decl)))
        if (left.exists(p => isPattern(p.getIdType))) walk(result).asInstanceOf[FnExpr]
        else result
      }

    case _ => super.walk(node)
  }

  def paramToDecl(param: Param) = toOption(param.getIdType) match {
    case Some(ty) if ty.isInstanceOf[Type] =>
      NF.makeVarDecl(NU.getSpan(param),
                     Useful.list(NF.makeLValue(param.getName, ty, param.getMods)), None)
    case _ =>
      bug("Trait value parameters should be declared with their types.")
  }

  def desugarLValue(lv: LValue) = lv match {
    case SLValue(i, name, mods, tp, isMutable) if isPattern(tp) =>
      val pattern = tp.get.asInstanceOf[Pattern]
      toOption(pattern.getName) match {
        case Some(ty) =>
          val new_lv = SLValue(i, name, mods, Some(ty), isMutable)
          val ps = toList(pattern.getPatterns.getPatterns)
          val recv = EF.makeVarRef(name)
          val left = ps.map(patternBindingToLValue(_, mods))
          val right = ps.zipWithIndex.map(patternBindingToExpr(_, recv, ty))
          (new_lv, left, right)
        case None => (lv, Nil, Nil)
      }
    case _ => (lv, Nil, Nil)
  }

  def desugarParam(p: Param) = p match {
    case SParam(i, name, mods, tp, e, varargs) if isPattern(tp) =>
      val pattern = tp.get.asInstanceOf[Pattern]
      toOption(pattern.getName) match {
        case Some(ty) =>
          val new_p = SParam(i, name, mods, Some(ty),
                             walk(e).asInstanceOf[Option[Expr]], varargs)
          val ps = toList(pattern.getPatterns.getPatterns)
          val recv = EF.makeVarRef(name)
          val left = ps.map(patternBindingToLValue(_, mods))
          val right = ps.zipWithIndex.map(patternBindingToExpr(_, recv, ty))
          (new_p, left, right)
        case None => (p, Nil, Nil)
      }
    case _ => (p, Nil, Nil)
  }

  def desugarVar(decl: Decl): List[Decl] = decl match {
    case v @ SVarDecl(info, lhs, init) =>
      val desugaredLValues = lhs.map(desugarLValue)
      val left = desugaredLValues.map(_._2).flatten
      val new_decl = SVarDecl(info, desugaredLValues.map(_._1),
                              walk(init).asInstanceOf[Option[Expr]])
      if (left.isEmpty) List(new_decl)
      else {
        val right = EF.makeMaybeTupleExpr(NU.getSpan(decl),
                                          toJavaList(desugaredLValues.map(_._3).flatten))
        val added = SVarDecl(info, left, Some(right))
        if (left.exists(p => isPattern(p.getIdType))) new_decl :: desugarVar(added)
        else List(new_decl, added)
      }
    case _ => List(walk(decl).asInstanceOf[Decl])
  }

  def desugarLocal(exp: Expr): Expr = exp match {
    case v @ SLocalVarDecl(info, body, lhs, rhs) =>
      val desugaredLValues = lhs.map(desugarLValue)
      val left = desugaredLValues.map(_._2).flatten
      val new_rhs = walk(rhs).asInstanceOf[Option[Expr]]
      if (left.isEmpty) {
        SLocalVarDecl(info, walk(body).asInstanceOf[Block], lhs, new_rhs)
      } else {
        val right = EF.makeMaybeTupleExpr(NU.getSpan(exp),
                                          toJavaList(desugaredLValues.map(_._3).flatten))
        val new_decl = SLocalVarDecl(info, walk(body).asInstanceOf[Block], left, Some(right))
        val new_body = SBlock(info, None, false, false, List(new_decl))
        val result = SLocalVarDecl(info, new_body, desugaredLValues.map(_._1), new_rhs)
        if (left.exists(p => isPattern(p.getIdType))) desugarLocal(result)
        else result
      }
    case _ => walk(exp).asInstanceOf[Expr]
  }

  def isPattern(pt: Option[TypeOrPattern]) = pt match {
    case Some(tp) => tp.isInstanceOf[Pattern]
    case _ => false
  }

  def patternBindingToLValue(pb: PatternBinding, mods: Modifiers) = {
    val span = NU.getSpan(pb)
    pb match {
      case SPlainPattern(_, _, name, _, Some(idType)) =>
        NF.makeLValue(name, idType, mods)
      case SPlainPattern(_, _, name, _, None) =>
        NF.makeLValue(span, name, mods, toJavaOption(None), false)
      case STypePattern(_, _, typ) =>
        NF.makeLValue(NF.makeId(span, "_"))
      case SNestedPattern(_, _, pat) =>
        NF.makeLValue(span, NF.makeId(span, DU.gensym("temp")), mods,
                      toJavaOption(Some(pat)), false)
    }
  }

  def patternBindingToExpr(pbi: (PatternBinding, Int),
                           recv: VarRef, ty: Type) = {
    val (pb, i) = pbi
    pb match {
      case t:TypePattern => EF.makeVoidLiteralExpr(NU.getSpan(pb))
      case SPlainPattern(_, field, _, _, _) => pbTe(field, i, recv, ty)
      case SNestedPattern(_, field, _) => pbTe(field, i, recv, ty)
    }
  }

  def pbTe(field: Option[Id], i: Int, recv: VarRef, ty: Type) = field match {
    case Some(id) => EF.makeFieldRef(recv, id)
    case None => ty match {
      case t:TraitType if typeConses.keySet.contains(t.getName) =>
        toOption(typeConses.get(t.getName).ast.asInstanceOf[TraitObjectDecl].getHeader.getParams) match {
          case Some(ps) =>
            EF.makeFieldRef(recv, toList(ps).apply(i).getName)
          case _ =>
            bug("A trait is expected to have value parameters for patterns.")
        }
      case _ => bug("A trait type is expected in a pattern.")
    }
  }
}
