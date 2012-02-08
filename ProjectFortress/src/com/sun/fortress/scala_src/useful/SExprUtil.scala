/*******************************************************************************
    Copyright 2009,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.scala_src.useful

import com.sun.fortress.nodes._
import com.sun.fortress.scala_src.nodes._
import com.sun.fortress.scala_src.useful.Lists._
import com.sun.fortress.scala_src.useful.Options._
import com.sun.fortress.useful.NI
import com.sun.fortress.scala_src.useful.STypesUtil._
import com.sun.fortress.nodes_util.{Span, NodeUtil => NU}
import com.sun.fortress.nodes_util.{NodeFactory => NF}
import com.sun.fortress.scala_src.types.TypeAnalyzer
import com.sun.fortress.scala_src.types.TypeSchemaAnalyzer

object SExprUtil {

  /**
   * Get the type previously inferred by the typechecker from an expression, if
   * it has one.
   */
  def getType(expr: Expr): Option[Type] = toOption(expr.getInfo.getExprType)

  /**
   * Is this expr checkable? An expr is not checkable iff it is a FnExpr with
   * not all of its parameters' types explicitly declared.
   */
  
  def isCheckable(expr: Expr): Boolean = expr match {
//    case t:TupleExpr => toList(t.getExprs).forall(isCheckable)
    case f:FnExpr => toListFromImmutable(f.getHeader.getParams).forall(p => p.getIdType.isSome)
    case _ => true   
  }
  
  def isFnExpr(e: Expr) = e match {
    case f:FnExpr => true
    case _ => false
  }
  
  /**
   * Determine if all of the given expressions have types previously inferred
   * by the typechecker.
   */
  def haveTypes(exprs: List[Expr]): Boolean =
    exprs.forall((e: Expr) => getType(e).isDefined)

  def haveTypesOrUncheckable(exprs: List[Expr]):Boolean =
    exprs.forall((e:Expr) => (getType(e).isDefined || !isCheckable(e)) )
  
  /**
   * Given an expression, return an identical expression with the given type
   * inserted into its ExprInfo.
   */
  def addType(expr: Expr, typ: Type): Expr = {
    object adder extends Walker {
      var swap = false

      override def walk(node: Any): Any = node match {
        case SExprInfo(a, b, _) if !swap =>
          swap = true
          SExprInfo(a, b, Some(typ))
        case _ if (!swap) => super.walk(node)
        case _ => node
      }
    }
    adder(expr).asInstanceOf[Expr]
  }
  
  /**
   * Given an expression, return an identical expression that is either
   * parenthesized or not.
   */
  def setParenthesized(expr: Expr, paren: Boolean): Expr = {
    object setter extends Walker {
      var swap = false

      override def walk(node: Any): Any = node match {
        case SExprInfo(a, _, b) if !swap =>
          swap = true
          SExprInfo(a, paren, b)
        case _ if (!swap) => super.walk(node)
        case _ => node
      }
    }
    setter(expr).asInstanceOf[Expr]
  }
  
  /**
   * Given an expression, return an identical expression with the given span
   * inserted into its ExprInfo.
   */
  def addSpan(expr: Expr, span: Span): Expr = {
    object adder extends Walker {
      var swap = false

      override def walk(node: Any): Any = node match {
        case SExprInfo(_, a, b) if !swap =>
          swap = true
          SExprInfo(span, a, b)
        case _ if (!swap) => super.walk(node)
        case _ => node
      }
    }
    adder(expr).asInstanceOf[Expr]
  }

  /**
   * Replaces the overloadings in a FunctionalRef with the given overloadings
   * 
   * When called from coercion code, skipOverloads is true, because the
   * list of overloads is empty (by construction) and thus the type and 
   * schema will be Any, which is not the right answer.  Instead, use the one
   * supplied in the fnRef, which is precalculated to be correct (apparently).
   */
  def addOverloadings(fnRef: FunctionalRef,
                      overs: List[Overloading],
                      skipOverloads : Boolean)
                     (implicit ta: TypeAnalyzer): FunctionalRef = {
    val typs = overs.map {
      case SOverloading(_, _, _, typ, schma) => (typ.get)
    }
    // Try Schema from old (this seems necessary).  whoops, no
    val schms = overs.map { // toListFromImmutable(fnRef.getNewOverloadings).map {
      case SOverloading(_, _, _, typ, schma) => (schma.get)
    }
    val tsa = new TypeSchemaAnalyzer()
    val ityps = tsa.duplicateFreeIntersection(typs)
    val ischms = tsa.duplicateFreeIntersection(schms)
    if (skipOverloads) 
    fnRef match {
      case SFnRef(a, b, c, d, e, f, _, g, h) => SFnRef(a, b, c, d, e, f, overs, g, h)
      case SOpRef(a, b, c, d, e, f, _, g, h) => SOpRef(a, b, c, d, e, f, overs, g, h)
      case _ => NI.nyi()
    }
    else
    fnRef match {
      case SFnRef(a, b, c, d, e, f, _, _, g) => SFnRef(a, b, c, d, e, f, overs, Some(ityps), Some(ischms))
      case SOpRef(a, b, c, d, e, f, _, _, g) => SOpRef(a, b, c, d, e, f, overs, Some(ityps), Some(ischms))
      case _ => NI.nyi()
    }
      
  }

  /**
   * Replaces the static args in a FunctionalRef with the given ones, but only
   * if the FunctionalRef didn't already have any.
   */
  def addStaticArgs(fnRef: FunctionalRef,
                    sargs: List[StaticArg]): FunctionalRef = fnRef match {
    case SFnRef(a, Nil, c, d, e, f, g, h, i) => SFnRef(a, sargs, c, d, e, f, g, h, i)
    case SOpRef(a, Nil, c, d, e, f, g, h, i) => SOpRef(a, sargs, c, d, e, f, g, h, i)
    case f:FnRef => f
    case f:OpRef => f
    case _ => NI.nyi()
  }

//  /** Create a coercion invocation from t to u. */
//  def makeCoercion(u: Type, arg: Expr): CoercionInvocation = {
//    val SExprInfo(span, paren, _) = arg.getInfo
//    SCoercionInvocation(SExprInfo(span, paren, Some(u)),
//                        u,
//                        List[StaticArg](),
//                        arg)
//  }

  /** Create an identical coercion but wrapped around `onto`. */
  def copyCoercion(c: CoercionInvocation, onto: Expr): CoercionInvocation =
    c match {
      case STraitCoercionInvocation(v1, _, v2, v3) =>
        STraitCoercionInvocation(v1, onto, v2, v3)
      case STupleCoercionInvocation(v1, _, v2, v3, v4) =>
        STupleCoercionInvocation(v1, onto, v2, v3, v4)
      case SArrowCoercionInvocation(v1, _, v2, v3, v4) =>
        SArrowCoercionInvocation(v1, onto, v2, v3, v4)
    }

  /**
   * Finds static args explicitly provided for the given application. If this
   * is not actually an application node, the result is None.
   */
  def getStaticArgsFromApp(app: Expr): Option[List[StaticArg]] =
    app match {
      case t:_RewriteFnApp => t.getFunction match {
        case f:FunctionalRef => Some(toListFromImmutable(f.getStaticArgs))
        case _ => None
      }
      case t:OpExpr => Some(toListFromImmutable(t.getOp.getStaticArgs))
      case t:MethodInvocation => Some(toListFromImmutable(t.getStaticArgs))
      case _ => None
    }

  /** Make a dummy expression for the given type and span. */
  def makeDummyFor(typ: Type, span: Span): Expr =
    SDummyExpr(SExprInfo(span, false, Some(typ)))

  /** Make a dummy expression for the given type. */
  def makeDummyFor(typ: Type): Expr =
    SDummyExpr(SExprInfo(NU.getSpan(typ), false, Some(typ)))

  /** Make a dummy expression that copies the given expression info. */
  def makeDummyFor(expr: Expr): Expr = SDummyExpr(expr.getInfo)
}
