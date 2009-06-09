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

object SExprUtil {

  /**
   * Get the type previously inferred by the typechecker from an expression, if
   * it has one.
   */
  def getType(expr: Expr): Option[Type] = toOption(expr.getInfo.getExprType)

  /**
   * Determine if all of the given expressions have types previously inferred
   * by the typechecker.
   */
  def haveTypes(exprs: List[Expr]): Boolean =
    exprs.forall((e:Expr) => getType(e).isDefined)
  
  
  
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
        case _ if(!swap) => super.walk(node)
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
  
  
}
