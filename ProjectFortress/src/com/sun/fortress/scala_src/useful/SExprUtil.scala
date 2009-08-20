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

package com.sun.fortress.scala_src.useful

import com.sun.fortress.nodes._
import com.sun.fortress.scala_src.nodes._
import com.sun.fortress.scala_src.useful.Lists._
import com.sun.fortress.scala_src.useful.Options._
import com.sun.fortress.useful.NI
import com.sun.fortress.scala_src.useful.STypesUtil._
import nodes_util.{Span, NodeUtil => NU}

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
    case f:FnExpr => fnExprHasParams(f)
    case _ => true   
  }
  
  def fnExprHasParams(f: FnExpr): Boolean = 
    toList(f.getHeader.getParams).forall(p => p.getIdType.isSome)
  
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
   * Replaces the overloadings in a FunctionalRef with the given overloadings
   */
  def addOverloadings(fnRef: FunctionalRef,
                      overs: List[Overloading]): FunctionalRef = fnRef match {
    case SFnRef(a, b, c, d, e, f, _, h) => SFnRef(a, b, c, d, e, f, overs, h)
    case SOpRef(a, b, c, d, e, f, _, h) => SOpRef(a, b, c, d, e, f, overs, h)
    case _ => NI.nyi()
  }

  /**
   * Replaces the static args in a FunctionalRef with the given ones.
   */
  def addStaticArgs(fnRef: FunctionalRef,
                    sargs: List[StaticArg]): FunctionalRef = fnRef match {
    case SFnRef(a, _, c, d, e, f, g, h) => SFnRef(a, sargs, c, d, e, f, g, h)
    case SOpRef(a, _, c, d, e, f, g, h) => SOpRef(a, sargs, c, d, e, f, g, h)
    case _ => NI.nyi()
  }

  /** Create a coercion invocation from t to u. */
  def makeCoercion(t: Type, u: Type, arg: Expr): CoercionInvocation = {
    val SExprInfo(span, paren, _) = arg.getInfo
    SCoercionInvocation(SExprInfo(span, paren, Some(u)),
                        u,
                        List[StaticArg](),
                        arg)
  }

  /** Create an identical coercion but wrapped around `onto`. */
  def copyCoercion(c: CoercionInvocation, onto: Expr): CoercionInvocation = {
    val SCoercionInvocation(v1, v2, v3, _) = c
    SCoercionInvocation(v1, v2, v3, onto)
  }

  /**
   * Finds static args explicitly provided for the given application. If this
   * is not actually an application node, the result is None.
   */
  def getStaticArgsFromApp(app: Expr): Option[List[StaticArg]] =
    app match {
      case t:_RewriteFnApp => t.getFunction match {
        case f:FunctionalRef => Some(toList(f.getStaticArgs))
        case _ => None
      }
      case t:OpExpr => Some(toList(t.getOp.getStaticArgs))
      case t:MethodInvocation => Some(toList(t.getStaticArgs))
      case _ => None
    }

  /** Make a dummy expression for the given type. */
  def makeDummyFor(typ: Type, span: Span): Expr =
    SDummyExpr(SExprInfo(span, false, Some(typ)))

  /** Make a dummy expression that copies the given expression info. */
  def makeDummyFor(expr: Expr): Expr = SDummyExpr(expr.getInfo)
}
