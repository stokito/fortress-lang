/*******************************************************************************
    Copyright 2009,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.scala_src.typechecker

import com.sun.fortress.compiler.index._
import com.sun.fortress.exceptions.CompilerError
import com.sun.fortress.exceptions.InterpreterBug.bug
import com.sun.fortress.nodes._
import com.sun.fortress.nodes_util.{ExprFactory => EF}
import com.sun.fortress.nodes_util.{NodeFactory => NF}
import com.sun.fortress.nodes_util.{NodeUtil => NU}
import com.sun.fortress.scala_src.nodes._
import com.sun.fortress.scala_src.typechecker.Formula._
import com.sun.fortress.scala_src.typechecker.staticenv.KindEnv
import com.sun.fortress.scala_src.types.TypeAnalyzer
import com.sun.fortress.scala_src.useful.ASTGenHelper._
import com.sun.fortress.scala_src.useful.Iterators._
import com.sun.fortress.scala_src.useful.ErrorLog
import com.sun.fortress.scala_src.useful.Lists._
import com.sun.fortress.scala_src.useful.Options._
import com.sun.fortress.scala_src.useful.Sets._
import com.sun.fortress.scala_src.useful.SExprUtil._
import com.sun.fortress.scala_src.useful.SNodeUtil._
import com.sun.fortress.scala_src.useful.STypesUtil._

class CoercionOracle(traits: TraitTable,
                     current: CompilationUnitIndex)
                    (implicit analyzer: TypeAnalyzer) {
  
  /** A coercion index and its arrow type with lifted params instantiated. */
  type LiftedCoercion = (Coercion, ArrowType, ArrowType)

  /**
   * Create the Id for an invocation of a coercion to trait U. If the trait's
   * name is Oof which is an imported alias of Foo from API A, then the coercion
   * name will include "Foo" instead of "Oof". In any case, if the trait was
   * imported explicitly (e.g. not via ellipsis), this coercion name will be
   * qualified.
   */
  def makeCoercionId(u: TraitType): Id = {
    val realTraitName = getRealName(u.getName, toListFromImmutable(current.ast.getImports))
    NF.makeLiftedCoercionId(NU.getSpan(u), realTraitName.asInstanceOf[Id])
  }

  /**
   * Get the most specific type out of a set of types under the `moreSpecific`
   * relation.
   */
  def mostSpecific(cs: Set[Type]): Option[Type] = {
    if (cs.isEmpty) {
      bug("Attempt to find the most specific type in an empty set")
    }
    else {
      var result: Option[Type] = None
      for (c <- cs ; if result.isEmpty || moreSpecific(c, result.get)) {
        result = Some(c)
      }
      result
    }
  }

  /** The `moreSpecific` relation; no less specific and unequal. */
  def moreSpecific(t: Type, u: Type): Boolean =
    noLessSpecific(t, u) && !isTrue(analyzer.equivalent(t, u))

  /** The `noLessSpecific` relation. */
  def noLessSpecific(t: Type, u: Type): Boolean =
    isTrue(analyzer.subtype(t, u)) ||
      (analyzer.definitelyExcludes(t, u) && coercesTo(t, u) && rejects(t, u))

  /** Determines if T rejects U. */
  def rejects(t: Type, u: Type): Boolean =
    getCoercionsTo(t).forall(ca => analyzer.definitelyExcludes(ca._2.getDomain, u))

  /** The set of all arrow types for coercions from types T to U. */
  def getCoercionsTo(uu: Type): Set[LiftedCoercion] = {
    val u =
      if (uu.isInstanceOf[TraitSelfType] &&
          uu.asInstanceOf[TraitSelfType].getNamed.isInstanceOf[TraitType])
        uu.asInstanceOf[TraitSelfType].getNamed.asInstanceOf[TraitType]
      else uu

    if (!u.isInstanceOf[TraitType]) return Set()

    // Get name and possible static args out of the type.
    val STraitType(_, name, sargs, _) = u

    // Get all the coercions from the type U.
    val coercions = toOption(traits.typeCons(name)) match {
      case Some(ti:TraitIndex) => toSet(ti.coercions)
      case None => Set()
    }

    // Get the instantiated coercion arrow.
    def instantiateArrow(c: Coercion): Option[LiftedCoercion] = {
      makeArrowFromFunctional(c).flatMap(arrow =>
        staticInstantiation(sargs,
                            arrow,
                            applyLifted = true,
                            applyUnlifted = false)
          .map(instArrow => (c, instArrow.asInstanceOf[ArrowType], arrow)))
    }

    // Get all the arrows that were found.
    coercions.flatMap(instantiateArrow)
  }

  /** Determines if T is substitutable for U. */
  def substitutableFor(t: Type, u: Type): Boolean =
    isTrue(analyzer.subtype(t, u)) || coercesTo(t, u)

  /**
   * Determine if T is substitutable for U. If T <: U, then return Some(None).
   * If T ~~> U and expr given, then return Some(Some(c)) where c is the
   * coercion of expr to type U. If T ~~> U and expr not given, then return
   * Some(None). Otherwise, return None.
   */
  def checkSubstitutable(t: Type,
                         u: Type,
                         expr: Option[Expr])
                         : Option[Option[CoercionInvocation]] = {
    if (isTrue(analyzer.subtype(t, u)))
      Some(None)
    else
      checkCoercion(t, u, expr)
  }

  /** Determines if T ~~> U. */
  def coercesTo(t: Type, u: Type): Boolean = checkCoercion(t, u, None).isSome

  /** Determines if e:T ~~> U and if so returns the FnRefs for that coercion. */
  def buildCoercion(e: Expr, u: Type): Option[CoercionInvocation]
    = checkCoercion(getType(e).get, u, Some(e)).flatMap(o => o)

  /** Determines if T ~~> U and possibly builds a coercion. */
  private def checkCoercion(t: Type,
                            u: Type,
                            expr: Option[Expr])
                            : Option[Option[CoercionInvocation]] = (t, u) match {
    case (t:UnionType, _) => checkCoercionUnion(t, u, expr)
    case (_, u:TraitType) => checkCoercionTrait(t, u, expr)
    case (_, u:TraitSelfType) => checkCoercion(t, u.getNamed, expr)
    case (t:TupleType, u:TupleType) => checkCoercionTuple(t, u, expr)
    case (t:ArrowType, u:ArrowType) => checkCoercionArrow(t, u, expr)
    case _ => None
  }


  /*
   * The union of {T_i} coerces to U if all of the following hold:
   * - any of the T_i can be coerced to U
   * - the remaining T_i are substitutable for U
   * - the T_i all exclude each other
   */
  private def checkCoercionUnion(t: UnionType,
                                 u: Type,
                                 maybeArg: Option[Expr])
                                 : Option[Option[CoercionInvocation]] = {
    val SUnionType(_, telts) = t

    // Check that all of the T_i exclude.
    if (!isTrue(analyzer.allExclude(telts))) return None

    // Get all the possible coercions from T_i to U.
    val tiCoercions = telts.map(checkSubstitutable(_, u, maybeArg))

    // Check that all of them were at least substitutable.
    if (tiCoercions.exists(_.isNone)) return None

    // If there's an arg, build the coercion. Otherwise just say yes: Some(None).
    val arg = maybeArg.getOrElse(return Some(None))
    val info = SExprInfo(NU.getSpan(arg), false, Some(u))
    Some(Some(SUnionCoercionInvocation(info, u, arg, telts, tiCoercions.map(_.get))))
  }


  private def checkCoercionTrait(t: Type,
                                 u: TraitType,
                                 maybeArg: Option[Expr])
                                 : Option[Option[TraitCoercionInvocation]] = {
    // Make the dummy argument.
    val dummyArg = makeDummyFor(t)

    // Get a list of all the (coercion, arrow, inferred args, original arrow)
    // tuples that worked. The inferred args returned are only the unlifted
    // ones, as the lifted args are given in U.
    val liftedSargs = toListFromImmutable(u.getArgs)
    val allLiftedCoercions = getCoercionsTo(u)
    val coercionsAndArgs = allLiftedCoercions flatMap { liftedCoercion =>
      inferStaticParams(liftedCoercion._2, t, None) map { arrowAndSargs =>
        (liftedCoercion._1,
         arrowAndSargs._1,
         liftedSargs ++ arrowAndSargs._2, // Prepend the lifted static args.
         liftedCoercion._3)
      }
    }
    if (coercionsAndArgs.isEmpty) return None
    else if (maybeArg.isNone) return Some(None)
    val arg = maybeArg.get
    val argSpan = NU.getSpan(arg)

    // Build app candidates and sort them to find the SMA.
    val candidates = coercionsAndArgs.map { caa => {
      if (false) {
          AppCandidate(caa._2, caa._3, List(arg), None, None)
      } else {
          // Trying to fake an overloading to see if we can get the schema right
          // So far this breaks things, but why?
          val caa_ast = caa._1.ast
          AppCandidate(caa._2, caa._3, List(arg),
                  Some(NF.makeOverloading(caa_ast.getInfo,
                       caa_ast.getUnambiguousName,
                       caa_ast.getHeader.getName,
                       Some(caa._2),
                       Some(caa._4))), None)
          }
      }
    }.toList.sortWith { (c1, c2) =>
      moreSpecificCandidate(c1, c2)(this)
    }
    val AppCandidate(bestArrow, bestSargs, _, bestOverload, bestFnl) = candidates.head
    val coercionId = makeCoercionId(u)
    
    // Make an overloading for each lifted coercion to U.
    val overloadings = coercionsAndArgs map { caa =>
      
      // Compute the name here using the import information, but use the span
      // from the original coercion decl.
      val coercionNameSpan = NU.getSpan(caa._1.ast.getUnambiguousName)
      val overloadingId = setSpan(coercionId, coercionNameSpan).asInstanceOf[Id]
      
      // Use coercionId as originalName -- no idea what the real name should be
      SOverloading(SSpanInfo(coercionNameSpan),
                   coercionId,
                   coercionId,
                   Some(caa._2),
                   Some(caa._4))
    }

    // Make a dummy functional ref.
    val fnRef = EF.makeFnRefHelpScala(argSpan,
                             false,
                             coercionId,
                             toJavaList(Nil),
                             toJavaList(overloadings),
                             toJavaList(overloadings),
                             bestOverload.get.getType,
                             bestOverload.get.getSchema)

    // Rewrite the FnRef so that it filters out dynamically unapplicable arrows.
    val newFnRef = rewriteApplicand(fnRef, candidates, true).asInstanceOf[FnRef]

    // Make the coercion invocation!
    Some(Some(STraitCoercionInvocation(SExprInfo(argSpan, false, Some(u)), arg, u, newFnRef)))
  }

  private def checkCoercionTuple(t: TupleType,
                                 u: TupleType,
                                 maybeArg: Option[Expr])
                                 : Option[Option[TupleCoercionInvocation]] = {
    val (x@STupleType(_, xelts, xvar, _), y@STupleType(_, yelts, yvar, _)) = (t, u)

    // 1. X is not a subtype of Y.
    if (isTrue(analyzer.subtype(x, y))) return None

    // 2. If X has more elts, then create the coercions for them.
    if (xelts.length > yelts.length) return None
    var xCoercionElts = (xelts, yelts).zipped.map { (xelt, yelt) =>
      val maybeDummy = maybeArg.map(_ => makeDummyFor(xelt))
      checkSubstitutable(xelt, yelt, maybeDummy)
    }

    // 3. Check for same number of plain types if neither have varargs.
    if (xelts.length != yelts.length && yvar.isNone) return None

    // 4. Check varargs types, possibly appending X's vararg to the coercions.
    val xCoercionVararg =
      if (xvar.isDefined && yvar.isDefined) {
        val maybeDummy = maybeArg.map(_ => makeDummyFor(xvar.get))
        Some(checkSubstitutable(xvar.get, yvar.get, maybeDummy))
      } else
        None

    // 5. Check that remainder of X is substitutable for Y's varargs.
    val xCoercionsMoreElts =
      if (yvar.isDefined)
        xelts.drop(yelts.length).map { xelt =>
          val maybeDummy = maybeArg.map(_ => makeDummyFor(xelt))
          checkSubstitutable(xelt, yvar.get, maybeDummy)
        }
      else
        Nil

    // Combine all the possible coercions.
    val xMaybeCoercions = xCoercionElts ++ xCoercionsMoreElts
    if (xMaybeCoercions.exists(_.isNone)) return None
    val coercions = xMaybeCoercions.map(_.get)

    // Peel out the vararg coercion, checking for failure.
    val varargCoercion = xCoercionVararg match {
      case Some(None) => return None // vararg coercion failed
      case Some(Some(oc)) => Some(oc)
      case None => None
    }

    // Are we building a coercion?
    if (maybeArg.isNone) return Some(None)
    val arg = maybeArg.get
    val argSpan = NU.getSpan(arg)

    // Make the tuple coercion.
    Some(Some(STupleCoercionInvocation(SExprInfo(argSpan, false, Some(u)),
                                       arg,
                                       u,
                                       coercions,
                                       varargCoercion)))
  }

  private def checkCoercionArrow(t: ArrowType,
                                 u: ArrowType,
                                 maybeArg: Option[Expr])
                                 : Option[Option[ArrowCoercionInvocation]] = {

    val (SArrowType(_, a, b, teff, _, _), SArrowType(_, d, e, ueff, _, _)) = (t, u)
    // Get throws types.
    val c = toOption(teff.getThrowsClause).map(toList[Type]).getOrElse(List[Type]())
    val f = toOption(ueff.getThrowsClause).map(toList[Type]).getOrElse(List[Type]())

    // 1. A -> B throws C is not a subtype of D -> E throws F
    if (isTrue(analyzer.subtype(t, u))) return None

    // 2. D substitutable for A
    val dummyD = maybeArg.map(_ => makeDummyFor(d))
    val domainCoercion = checkSubstitutable(d, a, dummyD).getOrElse(return None)

    // 3. B substitutable for E
    val dummyB = maybeArg.map(_ => makeDummyFor(b))
    val rangeCoercion = checkSubstitutable(b, e, dummyB).getOrElse(return None)

    // 4. for all X in C, there is a Y in F such that X is substitutable for Y
    if (!c.forall(x => f.exists(y => substitutableFor(x, y)))) return None

    // Are we building a coercion?
    if (maybeArg.isNone) return Some(None)
    val arg = maybeArg.get
    val argSpan = NU.getSpan(arg)

    // Build the invocation node.
    Some(Some(SArrowCoercionInvocation(SExprInfo(argSpan, false, Some(u)),
                                       arg,
                                       u,
                                       domainCoercion,
                                       rangeCoercion)))
  }

}
