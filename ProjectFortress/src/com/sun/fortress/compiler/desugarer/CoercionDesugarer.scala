/*******************************************************************************
    Copyright 2009,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler.desugarer

import com.sun.fortress.exceptions.InterpreterBug.bug
import com.sun.fortress.nodes._
import com.sun.fortress.nodes_util.{ExprFactory => EF}
import com.sun.fortress.nodes_util.{NodeFactory => NF}
import com.sun.fortress.nodes_util.{NodeUtil => NU}
import com.sun.fortress.nodes_util.Modifiers
import com.sun.fortress.nodes_util.Span
import com.sun.fortress.scala_src.nodes._
import com.sun.fortress.scala_src.useful.Lists._
import com.sun.fortress.scala_src.useful.Options._
import com.sun.fortress.scala_src.useful.SExprUtil._
import com.sun.fortress.scala_src.useful.STypesUtil._
import com.sun.fortress.useful.Useful

/**
 * Desugars all CoercionInvocation nodes into function applications or
 * expressions composed thereof. No CoercionInvocation nodes of any kind will
 * remain.
 */
class CoercionDesugarer extends Walker {

  /** Oracle used for generating names. */
  private val naming = new NameOracle(this)

  /** Walk the AST, recursively desugaring any coercions. */
  override def walk(node: Any) = node match {
    case c @ SCoercionInvocation(_, _, expr) =>
      desugarCoercion(c, this.walk(expr).asInstanceOf[Expr])
    case _ => super.walk(node)
  }

  /** Desugar the given coercion, using the given arg. */
  protected def desugarCoercion(c: CoercionInvocation, arg: Expr): Expr = {
  //  println("Desugaring " + c.toStringReadable + " on argument " + arg.toStringReadable)
   c match {
    case c:TraitCoercionInvocation => desugarTraitCoercion(c, arg)
    case c:TupleCoercionInvocation => desugarTupleCoercion(c, arg)
    case c:ArrowCoercionInvocation => desugarArrowCoercion(c, arg)
    case c:UnionCoercionInvocation => desugarUnionCoercion(c, arg)
  }}

  /** Desugar the given coercion, using its own arg. */
  protected def desugarCoercion(c: CoercionInvocation): Expr =
    desugarCoercion(c, c.getArg)

  /**
   * Desugars a coercion to a trait type into a function application. Because we
   * have already created and stored the required FnRef, this method is trivial.
   */
  protected def desugarTraitCoercion(coercion: TraitCoercionInvocation, expr: Expr): Expr = {
    val STraitCoercionInvocation(info, _, toType, fnRef) = coercion
    addType(EF.make_RewriteFnApp(info.getSpan, fnRef, expr), toType)
  }

  /**
   * Desugars a coercion to a tuple type into a let-bound tuple:
   *   `(A, B).coerce(t)` is desugared to
   *   `(t_0, t_1) = t; (A.coerce(t_0), B.coerce(t_1))`.
   * Because we have already created and stored all the necessary
   * CoercionInvocations on the tuple's constituent types, we need only to copy
   * them onto the new `t_i` variables. 
   * TODO: Keywords args
   */
  protected def desugarTupleCoercion(coercion: TupleCoercionInvocation, expr: Expr): Expr = {
    val STupleCoercionInvocation(info, _, toType, eltCoercions, varargCoercion) = coercion

    // Get the types out of the expr.
    val exprType = getType(expr).get.asInstanceOf[TupleType]
    val exprTypes = maybeSnoc(toListFromImmutable(exprType.getElements),
                              toOption(exprType.getVarargs))

    // Create a fresh variable name for each element in the tuple type.
    val freshVars = exprTypes.map(naming.makeVarRef)

    // Assign the new names to the expr.
    val decl = TempVarDecl(freshVars, expr)

    // Get the list of all the optional constituent coercions, and desugar each
    // one or just use its new variable name if it shouldn't be coerced.
    val subCoercions = maybeSnoc(eltCoercions, varargCoercion)
    val subExprs = (subCoercions, freshVars).zipped.map {
      case (Some(c), freshVar) => desugarCoercion(copyCoercion(c, freshVar))
      case (None, freshVar) => freshVar
    }

    // Recomose these subexpressions into a tuple.
    val (subExprElts, subExprVararg) = maybeUnsnoc(varargCoercion.isSome, subExprs)
    val coercionsTuple = EF.makeTupleExpr(info.getSpan,
                                          info.isParenthesized,
                                          info.getExprType,
                                          toJavaList(subExprElts),
                                          toJavaOption(subExprVararg),
                                          toJavaList(List[KeywordExpr]()),
                                          false)

    // Wrap the decl around the tuple and insert in a do.
    decl.makeLocalVarDeclDo(NU.getSpan(coercion), coercionsTuple)
  }

  /**
   * Desugars a coercion to an arrow type into a FnExpr:
   *   `(A -> B).coerce(f)` where f: C -> D is desugared to
   *   `fn (x: A) => B.coerce(f(C.coerce(x)))`.
   * Because we have already created and stored the necessary
   * CoercionInvocations on the domain and range types, we need only to copy
   * them onto the appropriate expressions.
   */
  protected def desugarArrowCoercion(coercion: ArrowCoercionInvocation, f: Expr): Expr = {
    val SArrowCoercionInvocation(info, _, toType, domCoercion, rngCoercion) = coercion

    // Get the types out of the expr.
    val fType = getType(f).get.asInstanceOf[ArrowType]
    val fDomain = fType.getDomain
    val fRange = fType.getRange

    // Make a new variable as the outer FnRef parameter.
    val x = naming.makeVarRef(toType.getDomain)

    // Make the coercion argument to f.
    val fArg = domCoercion match {
      case Some(c) => desugarCoercion(copyCoercion(c, x))
      case None => x
    }

    // Make the application of f to this argument.
    val fApp = addType(EF.make_RewriteFnApp(f, fArg), fRange)

    // Wrap this application in a coercion.
    val body = rngCoercion match {
      case Some(c) => desugarCoercion(copyCoercion(c, fApp))
      case None => fApp
    }

    // Put the body in a new FnExpr.
    val params = List(NF.makeParam(x.getVarId, getType(x).get))
    val fnExpr = EF.makeFnExpr(info.getSpan,
                               toJavaList(params),
                               some(toType.getRange),
                               body)
    addType(fnExpr, toType)
  }

  /**
   * Desugars a coercion from a union type into a Typecase. For each constituent
   * type of the union, there is a typecase clause that evaluates to either
   * the name (which is bound to the coerced expr) if that type is a subtype of
   * the target, or to a desugared coercion expression if that type coerces to
   * the target.
   */
  protected def desugarUnionCoercion(coercion: UnionCoercionInvocation, e: Expr): Expr = {
    val SUnionCoercionInvocation(SExprInfo(span, _, _), toType, _, fromTypes, fromCoercions) = coercion

    // A fresh Id for the bound name of the typecase.
    val boundVarName = naming.makeId
    def boundPattern(t: Type) = {
      val pb = NF.makePlainPattern(span, boundVarName, Modifiers.None, Some(t))
      NF.makePattern(span, None, NF.makePatternArgs(span, Useful.list(pb)))
    }

    // Create the clauses, one for each constituent type of the union.
    val clauses = (fromTypes, fromCoercions).zipped.map {

      // Subtype, so no coercion.
      case (t, None) =>
        val typedBoundVar = EF.makeVarRef(span, Some(t), boundVarName)
        STypecaseClause(SSpanInfo(span), None, boundPattern(t), EF.makeBlock(typedBoundVar))

      // Coerce the bound variable to this type.
      case (t, Some(c)) =>
        val typedBoundVar = EF.makeVarRef(span, Some(t), boundVarName)
        val coercedVar = desugarCoercion(copyCoercion(c, typedBoundVar))
        STypecaseClause(SSpanInfo(span), None, boundPattern(t), EF.makeBlock(coercedVar))
    }
    STypecase(SExprInfo(span, true, Some(toType)),
              e,
              clauses,
              None)
  }

}
