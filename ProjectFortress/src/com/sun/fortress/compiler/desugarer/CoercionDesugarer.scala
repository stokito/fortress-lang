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

import com.sun.fortress.compiler.typechecker.TypeAnalyzer
import com.sun.fortress.exceptions.InterpreterBug.bug
import com.sun.fortress.nodes._
import com.sun.fortress.nodes_util.{ExprFactory => EF}
import com.sun.fortress.nodes_util.{NodeFactory => NF}
import com.sun.fortress.nodes_util.{NodeUtil => NU}
import com.sun.fortress.nodes_util.Span
import com.sun.fortress.scala_src.nodes._
import com.sun.fortress.scala_src.typechecker.TraitTable
import com.sun.fortress.scala_src.useful.KindEnvWalker
import com.sun.fortress.scala_src.useful.Lists._
import com.sun.fortress.scala_src.useful.SExprUtil._
import com.sun.fortress.scala_src.useful.STypesUtil._

/**
 * Desugars all CoercionInvocation nodes into function applications.
 */
class CoercionDesugarer(table: TraitTable) extends KindEnvWalker {

  /** Oracle used for generating names. */
  private val naming = new NameOracle(this)

  override def walk(node: Any) = node match {
    case SCoercionInvocation(v1, v2, v3, expr) =>
      val recurredExpr = this.walk(expr).asInstanceOf[Expr]
      desugarCoercion(SCoercionInvocation(v1, v2, v3, recurredExpr))
    case _ => super.walk(node)
  }

  def desugarCoercion(c: CoercionInvocation): Expr = c.getToType match {
    case _:TupleType => desugarTupleCoercion(c)
    case _:ArrowType => desugarArrowCoercion(c)
    case _:BaseType => desugarBaseCoercion(c)
    case t => bug("cannot perform coercion to type %s".format(t))
  }

  /**
   * Desugars a coercion to a base type into a function application. It needs to
   * construct a type analyzer from the current kind environment due to the
   * possibility of coercing to type variables. Also, coercions created from
   * desugaring arrow- and tuple-type coercions may or may not be necessary,
   * so we also need to check for subtypes.
   */
  protected def desugarBaseCoercion(coercion: CoercionInvocation): Expr = {
    val SCoercionInvocation(v1, toType:BaseType, sargs, expr) = coercion
    val exprType = getType(expr).get

    // Create the type analyzer with the correct kind environment.
    val analyzer = new TypeAnalyzer(table, env)

    // Is a coercion necessary?
    if (analyzer.subtype(exprType, toType).isTrue) return expr

    // For now, just pass the CoercionInvocation along.
    coercion
  }

  // TODO: Keywords and varargs
  protected def desugarTupleCoercion(coercion: CoercionInvocation): Expr = {
    val SCoercionInvocation(v1, toType:TupleType, sargs, expr) = coercion

    // Get the types out of the expr.
    val exprType = getType(expr).get.asInstanceOf[TupleType]
    val exprTypes = toList(exprType.getElements)

    // Create a fresh variable name for each element in the tuple type.
    val freshVars = exprTypes.map(naming.makeVarRef)

    // Assign the new names to the expr.
    val decl = TempVarDecl(freshVars, expr)

    // Get tuple elements from target type.
    val toTypeElts = toList(toType.getElements)

    // Make a coercion for each element in the tuple and desugar them.
    val coercions = List.map2(toTypeElts, freshVars) ((typ, varRef) =>
      makeDesugaredCoercion(typ, varRef))

    // Recompose these into a tuple.
    val coercionsTuple =
      addType(EF.makeTupleExpr(NU.getSpan(coercion), toJavaList(coercions)), toType)

    // Wrap the decl around the tuple.
    decl.makeLocalVarDecl(coercionsTuple)
  }

  protected def desugarArrowCoercion(coercion: CoercionInvocation): Expr = {
    val SCoercionInvocation(v1, toType:ArrowType, sargs, f) = coercion

    // Get the types out of the expr.
    val fType = getType(f).get.asInstanceOf[ArrowType]
    val fDomain = fType.getDomain
    val fRange = fType.getRange

    // Get the domain and range of the target type.
    val toDomain = toType.getDomain
    val toRange = toType.getRange

    // Make a new variable as the outer FnRef parameter.
    val x = naming.makeVarRef(toDomain)

    // Make the coercion argument to f.
    val fArg = desugarCoercion(makeCoercion(fDomain, x))

    // Make the application of f to this argument.
    val fApp = addType(EF.make_RewriteFnApp(f, fArg), fRange)

    // Wrap this application in a coercion.
    val body = makeDesugaredCoercion(toRange, fApp)

    // Put the body in a new FnExpr.
    val params = List(NF.makeParam(x.getVarId, getType(x).get))
    addType(EF.makeFnExpr(NU.getSpan(coercion), toJavaList(params), body), toType)
  }

  /** Make a coercion and desugar it. */
  protected def makeDesugaredCoercion(toType: Type, expr: Expr): Expr =
    desugarCoercion(makeCoercion(toType, expr))
}