/*******************************************************************************
    Copyright 2009, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler.desugarer

import com.sun.fortress.nodes._
import com.sun.fortress.nodes_util.{ExprFactory => EF}
import com.sun.fortress.nodes_util.{NodeFactory => NF}
import com.sun.fortress.nodes_util.{NodeUtil => NU}
import com.sun.fortress.nodes_util.Span
import com.sun.fortress.scala_src.useful.Options._
import com.sun.fortress.scala_src.useful.SExprUtil._

/**
 * Generates fresh names for use by some desugaring.
 */
class NameOracle(desugarer: Object) {

  /** The prefix on all fresh names created by this oracle. */
  private val prefix = desugarer.getClass.getSimpleName

  /** The counter for how many fresh names have been created so far. */
  private var count = 0

  /** Create a fresh String name. */
  private def gensym: String = {
    val name = "%s#%d".format(prefix, count)
    count += 1
    name
  }

  /** Create a fresh name as an Id. */
  def makeId: Id = NF.makeId(NF.desugarerSpan, gensym)

  /** Create a fresh name as an Id using the given span. */
  def makeId(span: Span): Id = NF.makeId(span, gensym)

  /** Create a fresh name as an Id using the given span. */
  def makeId(typ: Type): Id = NF.makeId(NU.getSpan(typ), gensym)

  /** Create a fresh name as a VarRef. */
  def makeVarRef: VarRef = EF.makeVarRef(makeId)

  /** Create a fresh name as a VarRef using the given span. */
  def makeVarRef(span: Span): VarRef = EF.makeVarRef(makeId(span))

  /** Create a fresh name as a VarRef using the given type as its declared type. */
  def makeVarRef(typ: Type): VarRef =
    EF.makeVarRef(NU.getSpan(typ), some(typ), makeId(typ))

  /** Create a fresh name as a VarRef using the given type as its declared type. */
  def makeVarRef(expr: Expr): VarRef =
    EF.makeVarRef(NU.getSpan(expr), toJavaOption(getType(expr)), makeId(NU.getSpan(expr)))

  /** Create a fresh name and return a pair of an LValue and a VarRef for that name. */
  def makeLValueAndVarRef(span: Span): (LValue, VarRef) = {
    val name = makeId(span)
    (NF.makeLValue(span, name), EF.makeVarRef(span, name))
  }

}
