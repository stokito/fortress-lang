/*******************************************************************************
    Copyright 2009,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler.desugarer

import com.sun.fortress.compiler.Types
import com.sun.fortress.exceptions.InterpreterBug.bug
import com.sun.fortress.nodes._
import com.sun.fortress.nodes_util.{ExprFactory => EF}
import com.sun.fortress.nodes_util.{NodeFactory => NF}
import com.sun.fortress.nodes_util.{NodeUtil => NU}
import com.sun.fortress.nodes_util.Span
import com.sun.fortress.scala_src.useful.Lists._
import com.sun.fortress.scala_src.useful.Options._
import com.sun.fortress.scala_src.useful.SExprUtil._
import com.sun.fortress.scala_src.useful.STypesUtil._

/**
 * A temporary variable declaration that isn't an actual Node yet. Contains
 * a list of *typed* VarRefs and possibly a RHS expression.
 */
class TempVarDecl(val refs: List[VarRef], val rhs: Option[Expr]) {

  /** Create a list of LValues from the VarRefs. */
  def makeLValues: List[LValue] =
    refs.map(ref => NF.makeLValue(NU.getSpan(ref), ref.getVarId, getType(ref)))

  /** Create a LocalVarDecl using the given body exprs. */
  def makeLocalVarDecl(span: Span, body: List[Expr]): LocalVarDecl = {
    val decl = EF.makeLocalVarDecl(span,
                                   toJavaList(body),
                                   toJavaList(makeLValues),
                                   toJavaOption(rhs))
    // If the body hasn't been typed yet, just use the decl without a type.
    if (!haveTypes(body)) return decl
    
    // Otherwise get a type for the body and the decl.
    val declType = body.size match {
      case 0 => Types.VOID
      case _ => getType(body.last).get
    }
    addType(decl, declType).asInstanceOf[LocalVarDecl]
  }

  /** Create a LocalVarDecl using the given body expr. */
  def makeLocalVarDecl(span: Span, body: Expr): LocalVarDecl =
    makeLocalVarDecl(span, List(body))
  
  /** Create a Do expr around a LocalVarDecl using the given body exprs. */
  def makeLocalVarDeclDo(span: Span, body: List[Expr]): Do = {
    val decl = makeLocalVarDecl(span, body)
    EF.makeDo(span, NU.getExprType(decl), decl)
  }
  
  /** Create a Do expr around a LocalVarDecl using the given body expr. */
  def makeLocalVarDeclDo(span: Span, body: Expr): Do =
    makeLocalVarDeclDo(span, List(body))
}

/** Convenience methods. */
object TempVarDecl {

  // Stuff for pattern matching and creation.
  def unapply(d: TempVarDecl) =
    Some((d.refs, d.rhs))

  def apply(ref: VarRef, rhs: Expr): TempVarDecl = new TempVarDecl(List(ref), Some(rhs))
  def apply(refs: List[VarRef], rhs: Expr): TempVarDecl = new TempVarDecl(refs, Some(rhs))

  def apply(ref: VarRef): TempVarDecl = new TempVarDecl(List(ref), None)
  def apply(refs: List[VarRef]): TempVarDecl = new TempVarDecl(refs, None)
  
  /**
   * Create a decl for multiple vars on the left and a tuple comprised of the
   * given expressions on the right.
   */
  def apply(refs: List[VarRef], rhses: List[Expr]): TempVarDecl = {
    val javaRhses = toJavaList(rhses)
    val rhsTuple =
      if (haveTypes(rhses)) {
        // Give the RHS a type.
        val rhsType = makeArgumentType(rhses.map(getType(_).get))
        val tuple = EF.makeMaybeTupleExpr(NU.spanAll(javaRhses), javaRhses)
        addType(tuple, rhsType)
      } else {
        EF.makeMaybeTupleExpr(NU.spanAll(javaRhses), javaRhses)
      }
    
    new TempVarDecl(refs, Some(rhsTuple))
  }

  /**
   * Collapse a list of TempVarDecls into one big nested LocalVarDecl using
   * the given list of exprs as the body of the last one. Each invocation
   * of the folded function must yield a list of exprs since that's what
   * is expected for `body`. At the end we know we have a singleton list, so
   * get its element out.
   */
  def makeLocalVarDeclFromList(decls: List[TempVarDecl],
                               span: Span,
                               body: List[Expr]): LocalVarDecl =
    decls.foldRight(body) {
      (nextDecl, nextBody) => List(nextDecl.makeLocalVarDecl(span, nextBody))
    }.head.asInstanceOf[LocalVarDecl]

  /** Same as the other but takes only a single body expression. */
  def makeLocalVarDeclFromList(decls: List[TempVarDecl],
                               span: Span,
                               body: Expr): LocalVarDecl =
    decls.foldRight(body) {
      (nextDecl, nextBody) => nextDecl.makeLocalVarDecl(span, nextBody)
    }.asInstanceOf[LocalVarDecl]
}
