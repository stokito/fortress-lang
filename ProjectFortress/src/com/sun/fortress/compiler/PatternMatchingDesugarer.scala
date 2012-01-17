/*******************************************************************************
    Copyright 2010,2011, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler

import _root_.java.util.ArrayList
import com.sun.fortress.compiler.index.ComponentIndex
import com.sun.fortress.exceptions.StaticError
import com.sun.fortress.exceptions.TypeError
import com.sun.fortress.nodes._
import com.sun.fortress.nodes_util.{DesugarerUtil => DU}
import com.sun.fortress.nodes_util.{ExprFactory => EF}
import com.sun.fortress.nodes_util.Modifiers
import com.sun.fortress.nodes_util.{NodeFactory => NF}
import com.sun.fortress.nodes_util.{NodeUtil => NU}
import com.sun.fortress.scala_src.nodes._
import com.sun.fortress.scala_src.useful.ErrorLog;
import com.sun.fortress.scala_src.useful.Lists._
import com.sun.fortress.scala_src.useful.Options._
import com.sun.fortress.useful.HasAt;
import com.sun.fortress.useful.Useful

/**
 * Desugars all patterns.
 */
class PatternMatchingDesugarer(component: ComponentIndex,
                               globalEnv: GlobalEnvironment) extends Walker {

  val typeConses = component.typeConses
  def desugar() = walk(component.ast)

  val errors = new ArrayList[StaticError]()
  def getErrors() = errors
  private def signal(hasAt:HasAt, msg:String) = errors.add(TypeError.make(msg,hasAt))

  private var nested = 0

  /** Walk the AST, recursively desugaring any patterns. */
  override def walk(node: Any) = node match {

    case c @ SComponent(info, name, imports, decls, comprises, is_native, exports) =>
      val new_decls = decls.foldRight(Nil.asInstanceOf[List[Decl]])((d,r) => desugarVar(d) ::: r)
      SComponent(info, name, imports, new_decls, comprises, is_native, exports)

    // Desugars trait value parameters as abstract fields
    case t @ STraitDecl(t1, STraitTypeHeader(h1, h2, h3, h4, h5, contract, h7, params, decls),
                        t3, t4, t5, t6) =>
      val new_decls = params match {
        case Some(ps) => ps.map(paramToDecl) ::: decls
        case _ => decls
      }
      val desugared_decls = new_decls.foldRight(Nil.asInstanceOf[List[Decl]])((d,r) => desugarVar(d) ::: r)
      STraitDecl(t1, STraitTypeHeader(h1, h2, h3, h4, h5, walk(contract).asInstanceOf[Option[Contract]], h7,
                                      None, desugared_decls), t3, t4, t5, t6)

    case o @ SObjectDecl(o1, STraitTypeHeader(h1, h2, h3, h4, h5, contract, h7, params, decls), o3) =>
      val desugared_decls = decls.foldRight(Nil.asInstanceOf[List[Decl]])((d,r) => desugarVar(d) ::: r)
      SObjectDecl(o1, STraitTypeHeader(h1, h2, h3, h4, h5, walk(contract).asInstanceOf[Option[Contract]], h7,
                                       walk(params).asInstanceOf[Option[List[Param]]], desugared_decls), o3)

    // Desugars patterns in local variable declarations
    case b @ SBlock(info, loc, isAtomicBlock, isWithinDo, exprs) =>
      val new_exprs = exprs.foldRight(Nil.asInstanceOf[List[Expr]])((e,r) => desugarLocal(e) :: r)
      SBlock(info, walk(loc).asInstanceOf[Option[Expr]], isAtomicBlock, isWithinDo, new_exprs)

    // Desugars patterns in function expressions
    case SFnExpr(info,
                 SFnHeader(sps, mods, name, where, throwsC, contract, params, returnType),
                 body) =>
      val desugaredParams = params.map(desugarParam)
	  val left = desugaredParams.map(_._2)

      val new_body = walk(body).asInstanceOf[Expr]
      if (left.flatten.isEmpty) {
        SFnExpr(info, SFnHeader(sps, mods, name, where, throwsC,
                                walk(contract).asInstanceOf[Option[Contract]],
                                walk(params).asInstanceOf[List[Param]], returnType),
                if (nested > 0) adjustTypes(new_body) else new_body)
      } else {
        val span = NU.getSpan(body)
        val right = desugaredParams.map(_._3)
        val new_decl =
            (left zip right).foldRight(new_body)((pair, current_expr) => {
                                                 val inner_body = SBlock(info, None, false, false,
                                                                         List(current_expr))
                                                 SLocalVarDecl(info, inner_body, pair._1,
                                                               Some(EF.makeMaybeTupleExpr(span,
                                                                                          toJavaList(pair._2))))
                                                 })
        val result = SFnExpr(info, SFnHeader(sps, mods, name, where, throwsC,
                                             walk(contract).asInstanceOf[Option[Contract]],
                                             desugaredParams.map(_._1), returnType),
                             EF.makeDo(span,
                                       Useful.list(SBlock(info, None, false, true, List(new_decl)))))
        if (left.flatten.exists(p => isPattern(p.getIdType))) {
          nested += 1
          val res = walk(result).asInstanceOf[FnExpr]
          nested -= 1
          res
        } else result
      }

    case _ => super.walk(node)
  }

  def paramToDecl(param: Param) = toOption(param.getIdType) match {
    case Some(ty) if ty.isInstanceOf[Type] =>
      NF.makeVarDecl(NU.getSpan(param),
                     Useful.list(NF.makeLValue(param.getName, ty, param.getMods)), None)
    case _ =>
      signal(param, "Trait value parameters should be declared with their types.")
      NF.makeVarDecl(NU.getSpan(param), toJavaList(List[LValue]()), None)
  }

  def desugarLValue(lv: LValue) : (LValue, List[LValue], List[Expr]) = lv match {
    case SLValue(i, name, mods, tp, isMutable) if isPattern(tp) =>
      val span = NU.getSpan(lv)
      val pattern = tp.get.asInstanceOf[Pattern]
      val ps = toList(pattern.getPatterns.getPatterns)
      val new_name = if (NU.isUnderscore(name))
                       NF.makeId(span, DU.gensym("temp"))
                     else name
      toOption(pattern.getName) match {
        case Some(ty) =>
          val new_lv = SLValue(i, new_name, mods, Some(ty), isMutable)
          val recv = EF.makeVarRef(new_name)
          val left = ps.map(patternBindingToLValue(_, mods))

          /* error handling in case that a pattern has an incorrect structure. */
          ty match {
            case t: TraitType if typeConses.keySet.contains(t.getName) =>
               val params = typeConses.get(t.getName).ast.asInstanceOf[TraitObjectDecl].getHeader.getParams
               val numParams = toOption(params) match {
                                 case Some(ps) => ps.size
                                 case _ => 0
                               }
               val paramIdlist = toOption(params) match {
                                   case Some(ps) => toList(ps).map(_.getName)
                                   case _ => List()
                                 }
               /* check whether a given pattern is a keyword pattern or not */
               def isKeywordPattern(pattern : PatternBinding) : Boolean = {
                 toOption(pattern.getField) match {
                   case Some(kw) => !(paramIdlist.contains(kw))
                   case _ => false
                 }
               }
               if(ps.filter(! isKeywordPattern(_)).size != numParams){
                 signal(ty, "The number of patterns to bind should be greater than or equal to " + numParams)
                 return (lv, Nil, Nil)
               }
             case _ =>
               signal(ty, "Type " + ty + " not found.")
               return (lv, Nil, Nil)
          }
          val right = ps.zipWithIndex.map(patternBindingToExpr(_, recv, ty))
          (new_lv, left, right)
        case None => /* pattern.patterns: tuple of patterns */
          val tylist = ps.map(patternBindingToType)
          val new_lv = SLValue(i, new_name, mods,
                               Some(NF.makeMaybeTupleType(span, toJavaList(tylist))),
                               isMutable)
          val left = ps.map(patternBindingToLValue(_, mods))
          val right = List(EF.makeVarRef(new_name))
          (new_lv, left, right)
      }
    case _ => (lv, Nil, Nil)
  }

  def desugarParam(p: Param) : (Param, List[LValue], List[Expr]) = p match {
    case SParam(i, name, mods, tp, e, varargs) if isPattern(tp) =>
      val span = NU.getSpan(p)
      val pattern = tp.get.asInstanceOf[Pattern]
      val ps = toList(pattern.getPatterns.getPatterns)
      val new_name = if (NU.isUnderscore(name))
                       NF.makeId(span, DU.gensym("temp"))
                     else name
      toOption(pattern.getName) match {
        case Some(ty) =>
          /* error handling in case that a pattern has an incorrect structure. */
          ty match {
            case t: TraitType if typeConses.keySet.contains(t.getName) =>
               val params = typeConses.get(t.getName).ast.asInstanceOf[TraitObjectDecl].getHeader.getParams
               val numParams = toOption(params) match {
                                 case Some(ps) => ps.size
                                 case _ => 0
                               }
               val paramIdlist = toOption(params) match {
                                   case Some(ps) => toList(ps).map(_.getName)
                                   case _ => List()
                                 }
               /* check whether a given pattern is a keyword pattern or not */
               def isKeywordPattern(pattern : PatternBinding) : Boolean = {
                 toOption(pattern.getField) match {
                   case Some(kw) => !(paramIdlist.contains(kw))
                   case _ => false
                 }
               }
               if(ps.filter(! isKeywordPattern(_)).size != numParams) { // error
                 signal(p, "The number of patterns to bind should be greater than or equal to " + numParams)
                 return (p, Nil, Nil)
               }
            case _ => // error
              signal(ty, "Type " + ty + " not found.")
              return (p, Nil, Nil)
          }
          val new_p = SParam(i, new_name, mods, Some(ty),
                             walk(e).asInstanceOf[Option[Expr]], varargs)
          val recv = EF.makeVarRef(new_name)
          val left = ps.map(patternBindingToLValue(_, mods))
          val right = ps.zipWithIndex.map(patternBindingToExpr(_, recv, ty))
          (new_p, left, right)
        case None =>
          val tylist = ps.map(patternBindingToType)
          val new_p = SParam(i, new_name, mods,
                             Some(NF.makeMaybeTupleType(span, toJavaList(tylist))),
                             walk(e).asInstanceOf[Option[Expr]], varargs)
          val left = ps.map(patternBindingToLValue(_, mods))
          val right = List(EF.makeVarRef(new_name))
          (new_p, left, right)
      }
    case _ => (p, Nil, Nil)
  }

  def desugarFnParam(p : Param) : (Param, List[Param], List[Expr], (List[LValue], List[Expr])) = p match {
    case SParam(i, name, mods, tp, e, varargs) if isPattern(tp) =>
      val span = NU.getSpan(p)
      val pattern = tp.get.asInstanceOf[Pattern]
      val ps = toList(pattern.getPatterns.getPatterns)
      val new_name = if (NU.isUnderscore(name))
                       NF.makeId(span, DU.gensym("temp"))
                     else name
      val recv = EF.makeVarRef(new_name)
      toOption(pattern.getName) match {
        case Some(ty) =>
          /* error handling in case that a pattern has an incorrect structure. */
          ty match {
            case t: TraitType if typeConses.keySet.contains(t.getName) =>
               val params = typeConses.get(t.getName).ast.asInstanceOf[TraitObjectDecl].getHeader.getParams
               val numParams = toOption(params) match {
                                 case Some(ps) => ps.size
                                 case _ => 0
                               }
               val paramIdlist = toOption(params) match {
                                   case Some(ps) => toList(ps).map(_.getName)
                                   case _ => List()
                                 }
               /* check whether a given pattern is a keyword pattern or not */
               def isKeywordPattern(pattern : PatternBinding) : Boolean = {
                 toOption(pattern.getField) match {
                   case Some(kw) => !(paramIdlist.contains(kw))
                   case _ => false
                 }
               }
              if (ps.filter(! isKeywordPattern(_)).size != numParams) { // error
                signal(p, "The number of patterns to bind should be greater than or equal to " + numParams)
                return (p, Nil, Nil, (Nil, Nil))
              }
            case _ =>
              signal(ty, "Type " + ty + " not found.")
              return (p, Nil, Nil, (Nil, Nil))
          }

          val new_p = SParam(i, new_name, mods, Some(ty),
                             walk(e).asInstanceOf[Option[Expr]], varargs)
          val expr_list = ps.zipWithIndex.map(patternBindingToExpr(_, recv, ty))
          val ty_list = expr_list.map(p => fieldToType(p.getField, ty))
          val param_list = (ps zip ty_list).map(patternBindingToParam(_, mods))
                    (new_p, param_list, expr_list, (Nil, Nil))
        case None =>
          /* (tuple pattern parameter) */
          val tylist = ps.map(patternBindingToType)
          val new_p = SParam(i, new_name, mods,
                             Some(NF.makeMaybeTupleType(span, toJavaList(tylist))),
                             walk(e).asInstanceOf[Option[Expr]], varargs)
          val ty_list = tylist.map(Some(_))
          val param_list = (ps zip ty_list).map(patternBindingToParam(_, mods))
          val expr_list = param_list.map(p => EF.makeVarRef(p.getName))
          val left = (param_list.map(_.getName) zip tylist).map(pair =>
                                                                NF.makeLValue(pair._1, pair._2, mods))
          val right = List(recv)
          (new_p, param_list, expr_list, (left, right))
      }
    case _ => (p, Nil, Nil, (Nil, Nil))
   }

  def desugarVar(decl: Decl) : List[Decl] = decl match {
    case v @ SVarDecl(info, lhs, init) =>
      val desugaredLValues = lhs.map(desugarLValue)
      val left = desugaredLValues.map(_._2)
      val new_decl = SVarDecl(info, desugaredLValues.map(_._1),
                              walk(init).asInstanceOf[Option[Expr]])
      if (left.flatten.isEmpty) List(new_decl)
      else {
        def makeNewVD(le: List[LValue], re: List[Expr]) =
          SVarDecl(info, le, Some(EF.makeMaybeTupleExpr(NU.getSpan(decl), toJavaList(re))))
        val right = desugaredLValues.map(_._3)
        new_decl :: ((left zip right).map(pair => {
                                                  val added = makeNewVD(pair._1, pair._2)
                                                  if (pair._1.exists(p => isPattern(p.getIdType))) {
                                                    nested += 1
                                                    val result = desugarVar(added)
                                                    nested -= 1
                                                    result
                                                  } else List(added)
                                                  }).flatten)
      }
    /* desugar a function declaration with patterns */
    case f@SFnDecl(info,
                   SFnHeader(sps, mods, name, where, throwsC, contract, params, returnType),
                   unambiguousname, body, implement) =>
      val desugaredParams = params.map(desugarFnParam)
      // original parameters
      val param_list = desugaredParams.map(_._1)
      // new parameters
      val pattern_params = desugaredParams.map(_._2)
      val new_body = walk(body).asInstanceOf[Option[Expr]]
      val new_contract =  walk(contract).asInstanceOf[Option[Contract]]
      if(pattern_params.flatten.isEmpty)
        List(SFnDecl(info,
                     SFnHeader(sps, mods, name, where, throwsC, new_contract,
                               walk(params).asInstanceOf[List[Param]], returnType),
                     unambiguousname, new_body, implement))

      else{
        val new_params = (param_list zip pattern_params).map(pair =>
                                                             pair._1::pair._2).flatten
        val span = NU.getSpan(f)
        val expr_info = NF.makeExprInfo(span, false, None)
        val new_FnName =  NF.makeId(span, DU.gensymFn(name.toString))
        // argument list for new function call
        val arg_list = desugaredParams.map(_._3)
        val args = (param_list zip 
                    arg_list).map(pair =>
                                  EF.makeVarRef(pair._1.getName)::pair._2).flatten
        // new declarations to be added in case of a tuple pattern 
        val bindings = desugaredParams.map(_._4)
        // should function be var ref or fn ref?
        val call_expr = EF.make_RewriteFnApp(span, EF.makeVarRef(span, new_FnName), 
                                             EF.makeMaybeTupleExpr(span, toJavaList(args)))
        // make a temporary unambiguousname to identify a desugared FnDecl later
        val ds_unambiname = NF.makeId(span, "Desugared")

        // a new function declaration for the original function
        val new_Fndecl =
          if(bindings.map(_._1).flatten.isEmpty)  // No tuple patterns
             SFnDecl(info, SFnHeader(sps, mods, name, where, throwsC, new_contract,
                                     param_list, returnType),
                     ds_unambiname, Some(call_expr), implement)
          else {
            val new_decls = 
              bindings.foldRight(call_expr:Expr)((pair, current_expr) => {
                                                  val inner_body = 
                                                    SBlock(expr_info, None, false, false,
                                                           List(current_expr))
                                                  SLocalVarDecl(expr_info, inner_body, pair._1,
                                                                Some(EF.makeMaybeTupleExpr(span,
                                                                toJavaList(pair._2))))
                                                  })
            val final_body = EF.makeDo(span, Useful.list(SBlock(expr_info, None, false,
                                                                true, List(new_decls))))
            SFnDecl(info, SFnHeader(sps, mods, name, where, throwsC, new_contract,
                                    param_list, returnType),
                    ds_unambiname, Some(final_body), implement)
   
          }
        val new_span = NF.makeSpan("PMdesugarer-generated")
        // a new function declaration
        val added_Fndecl = SFnDecl(NF.makeSpanInfo(new_span),
                                   SFnHeader(sps, mods, new_FnName, where, throwsC,
                                             new_contract, new_params, returnType),
                                   NF.makeId(new_span, "Desugared"), new_body, implement)
        if(pattern_params.flatten.exists(p => isPattern(p.getIdType))) {
          nested += 1
          val result = desugarVar(added_Fndecl) ::: List(new_Fndecl)
          nested -= 1
          result
        } else List(added_Fndecl, new_Fndecl)
      }

    case _ => List(walk(decl).asInstanceOf[Decl])
  }

  def adjustTypes(body: Block): Block = body match {
    case SBlock(info, loc, isAtomicBlock, isWithinDo, exprs) =>
      SBlock(info, loc, isAtomicBlock, isWithinDo, exprs.map(adjustTypes))
  }
  def adjustTypes(expr: Expr): Expr = expr match {
    case SLocalVarDecl(info, body, lhs, rhs) =>
      SLocalVarDecl(info, adjustTypes(body), lhs, adjustTypes(lhs, rhs))
    case _ => expr
  }
  def adjustTypes(lhs: List[LValue], rhs: Option[Expr]): Option[Expr] = rhs match {
    case Some(STupleExpr(info, exprs, varargs, keywords, isInApp)) if lhs.size == exprs.size =>
      Some(STupleExpr(info, (lhs zip exprs).map(adjustTypes), varargs, keywords, isInApp))
    case Some(STupleExpr(info, exprs, varargs, keywords, isInApp)) if lhs.size == 1 =>
      Some(adjustTypes(lhs.head, rhs.get))
    case _ => rhs
  }
  def adjustTypes(pair: (LValue, Expr)): Expr = {
    val right = pair._2
    pair._1 match {
      case SLValue(i, name, mods, tp, isMutable) if isType(tp) =>
        val span = NU.getSpan(right)
        EF.make_RewriteFnApp(EF.makeFnRef(span, NF.makeId(span, "cast"),
                                          toJavaList(List(NF.makeTypeArg(tp.get.asInstanceOf[Type])))),
                             right)
      case _ => right
    }
  }

  def desugarLocal(exp: Expr): Expr = exp match {
    case v @ SLocalVarDecl(info, body, lhs, rhs) =>
      val desugaredLValues = lhs.map(desugarLValue)
      val left = desugaredLValues.map(_._2)
      val new_rhs = walk(rhs).asInstanceOf[Option[Expr]]
      val new_body = walk(body).asInstanceOf[Block]
      if (left.flatten.isEmpty) {
        SLocalVarDecl(info, if (nested > 0) adjustTypes(new_body) else new_body, lhs, new_rhs)
      } else {
        val right = desugaredLValues.map(_._3)
        val final_body =
            (left zip right).foldRight(new_body)((pair:(List[LValue], List[Expr]), current_body:Block) => {
                                                 val decl =
                                                   SLocalVarDecl(info, current_body, pair._1,
                                                                 Some(EF.makeMaybeTupleExpr(NU.getSpan(exp),
                                                                                            toJavaList(pair._2))))
                                                 SBlock(info, None, false, false, List(decl))
                                                 })
        val result = SLocalVarDecl(info, final_body, desugaredLValues.map(_._1), new_rhs)
        if (left.flatten.exists(p => isPattern(p.getIdType))) {
          nested += 1
          val res = desugarLocal(result)
          nested -= 1
          res
        } else result
      }
    case f @ SLetFn(info, body, fndecl_list) =>
      val new_decls = fndecl_list.foldRight(Nil.asInstanceOf[List[Decl]])((d, r) => desugarVar(d) ::: r)
      SLetFn(info, walk(body).asInstanceOf[Block], new_decls.asInstanceOf[List[FnDecl]])
    case _ => walk(exp).asInstanceOf[Expr]
  }

  def isPattern(pt: Option[TypeOrPattern]) = pt match {
    case Some(tp) => tp.isInstanceOf[Pattern]
    case _ => false
  }

  def isType(pt: Option[TypeOrPattern]) = pt match {
    case Some(tp) => tp.isInstanceOf[Type]
    case _ => false
  }

  /* get a type information from a pattern */
  def patternBindingToType(pb : PatternBinding) = {
    pb match {
      case SPlainPattern(_, _, _, _, Some(idType)) =>
        if(idType.isInstanceOf[Pattern]){
          val pattern = idType.asInstanceOf[Pattern]
          toOption(pattern.getName) match {
            case Some(ty) => ty
            case None =>
              signal(pattern, "A tuple pattern is expected to have types for all elements")
              NF.makeVoidType(NU.getSpan(pb))
          }
        }
        else idType.asInstanceOf[Type]
      case SPlainPattern(_, _, _, _, None) =>
        signal(pb, "A tuple pattern is expected to have types for all elements")
        NF.makeVoidType(NU.getSpan(pb))
      case STypePattern(_, _, typ) => typ
      case SNestedPattern(_, _, pat) =>
        toOption(pat.getName) match {
          case Some(ty) => ty
          case None =>
            signal(pb, "A tuple pattern is expected to have types for all elements")
            NF.makeVoidType(NU.getSpan(pb))
        }
     }
  }

  def patternBindingToLValue(pb: PatternBinding, mods: Modifiers) = {
    val span = NU.getSpan(pb)
    pb match {
      case SPlainPattern(_, _, name, _, Some(idType)) =>
        NF.makeLValue(name, idType, mods)
      case SPlainPattern(_, _, name, _, None) =>
        NF.makeLValue(span, name, mods, toJavaOption(None), false)
      case STypePattern(_, _, typ) =>
        NF.makeLValue(span, NF.makeId(span, "_"), mods, toJavaOption(Some(typ)), false)
      case SNestedPattern(_, _, pat) =>
        NF.makeLValue(span, NF.makeId(span, DU.gensym("temp")), mods,
                      toJavaOption(Some(pat)), false)
    }
  }

  def patternBindingToParam(pbi : (PatternBinding, Option[TypeOrPattern]), mods : Modifiers) = {
    val (pb, ty) = pbi
    val span = NU.getSpan(pb)
    pb match {
      case SPlainPattern(_, _, name, _, Some(idType)) =>
        NF.makeParam(span, mods, name, idType)
      case SPlainPattern(_, _, name, _, None) =>
        NF.makeParam(span, mods, name, toJavaOption(ty))
      case STypePattern(_, _, typ) =>
        NF.makeParam(span, mods, NF.makeId(span, DU.gensym("temp")), typ)
      case SNestedPattern(_, _, pat) =>
        NF.makeParam(span, mods, NF.makeId(span, DU.gensym("temp")), pat)
    }
  }

  def patternBindingToExpr(pbi: (PatternBinding, Int),
                           recv: VarRef, ty: Type) = {
    val (pb, i) = pbi
    pb match {
      case t:TypePattern => pbTe(None, i, recv, ty)
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
            signal(ty, "A trait is expected to have value parameters for patterns.")
            EF.makeFieldRef(recv, NF.makeId(NU.getSpan(recv), "_"))
        }
      case _ =>
        signal(ty, "A trait type is expected in a pattern.")
        EF.makeFieldRef(recv, NF.makeId(NU.getSpan(recv), "_"))
    }
  }

  /* get a type infomation from a field of a trait or an object */
  def fieldToType(field : Id, ty : Type) = ty match {
    case t:TraitType if typeConses.keySet.contains(t.getName) =>
      val header = typeConses.get(t.getName).ast.asInstanceOf[TraitObjectDecl].getHeader
      val pair_list = (toOption(header.getParams) match {
                         case Some(ps) => toList(ps).map(p=>(p.getName.toString, toOption(p.getIdType)))
                         case _ => List((Nil.asInstanceOf[String], Nil.asInstanceOf[Option[TypeOrPattern]]))
                       }) ::: toList(header.getDecls).foldRight(Nil.asInstanceOf[List[(String, Option[TypeOrPattern])]])((d, r) => declToIdType(d) ::: r)
      pair_list.find(pair => pair._1 == field.toString) match {
        case Some(pa) => pa._2
        case _ =>
          signal(field, field + " : Not defined")
          None
      }
    case _ =>
      signal(field, "A trait type is expected in a pattern.")
      Some(ty)
  }

  def declToIdType(decl : Decl) = decl match {
    case SVarDecl(_, lhs, _) if(lhs.size >= 1)=>
      lhs.map(p => (p.getName.toString, toOption(p.getIdType)))
    case SFnDecl(_, SFnHeader(_, mods, name, _, _, _, _, returnType), _, _, _) if mods.isGetter =>
      List((name.toString, toOption(returnType)))
    case _ => List(("", None))
  }
}
