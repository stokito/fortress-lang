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

import com.sun.fortress.compiler.Types
import com.sun.fortress.exceptions.InterpreterBug.bug
import com.sun.fortress.nodes._
import com.sun.fortress.nodes_util.{ExprFactory => EF}
import com.sun.fortress.nodes_util.{NodeFactory => NF}
import com.sun.fortress.nodes_util.{NodeUtil => NU}
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
  def makeLocalVarDecl(body: List[Expr]): LocalVarDecl = {
    val decl = EF.makeLocalVarDecl(NF.desugarerSpan,
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
  def makeLocalVarDecl(body: Expr): LocalVarDecl = makeLocalVarDecl(List(body))
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
   * Collapse a list of TempVarDecls into one big nested LocalVarDecl using
   * the given list of exprs as the body of the last one. Each invocation
   * of the folded function must yield a list of exprs since that's what
   * is expected for `body`. At the end we know we have a singleton list, so
   * get its element out.
   */
  def makeLocalVarDeclFromList(decls: List[TempVarDecl],
                               body: List[Expr]): LocalVarDecl =
    decls.foldRight(body) {
      (nextDecl, nextBody) => List(nextDecl.makeLocalVarDecl(nextBody))
    }.first.asInstanceOf[LocalVarDecl]

  /** Same as the other but takes only a single body expression. */
  def makeLocalVarDeclFromList(decls: List[TempVarDecl],
                               body: Expr): LocalVarDecl =
    decls.foldRight(body) {
      (nextDecl, nextBody) => nextDecl.makeLocalVarDecl(nextBody)
    }.asInstanceOf[LocalVarDecl]
}