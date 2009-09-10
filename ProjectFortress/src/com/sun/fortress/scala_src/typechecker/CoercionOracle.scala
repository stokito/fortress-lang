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

package com.sun.fortress.scala_src.typechecker

import com.sun.fortress.exceptions.CompilerError
import com.sun.fortress.exceptions.InterpreterBug.bug
import com.sun.fortress.nodes._
import com.sun.fortress.nodes_util.{ExprFactory => EF}
import com.sun.fortress.nodes_util.{NodeFactory => NF}
import com.sun.fortress.nodes_util.{NodeUtil => NU}
import com.sun.fortress.scala_src.nodes._
import com.sun.fortress.compiler.index._
import com.sun.fortress.compiler.typechecker.TypeAnalyzer
import com.sun.fortress.nodes_util.NodeFactory
import com.sun.fortress.scala_src.typechecker.staticenv.KindEnv
import com.sun.fortress.scala_src.useful.ASTGenHelper._
import com.sun.fortress.scala_src.useful.Iterators._
import com.sun.fortress.scala_src.useful.ErrorLog
import com.sun.fortress.scala_src.useful.Lists._
import com.sun.fortress.scala_src.useful.Options._
import com.sun.fortress.scala_src.useful.Sets._
import com.sun.fortress.scala_src.useful.SExprUtil._
import com.sun.fortress.scala_src.useful.STypesUtil._

class CoercionOracle(traits: TraitTable,
                     exclusions: ExclusionOracle)
                    (implicit analyzer: TypeAnalyzer) {

  /** Create the Id for an invocation of a coercion to trait U. */
  def makeCoercionId(u: TraitType): Id = {
    val api = toOption(u.getName.getApiName) match {
      case Some(a) => "_%s".format(a.getText)
      case None => ""
    }
    NF.makeId(NF.typeSpan, "coerce%s_%s".format(api, u.getName.getText))
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
      for (c <- cs) {
        if (result.isEmpty || moreSpecific(c, result.get)) result = Some(c)
      }
      result
    }
  }

  /** The `moreSpecific` relation; no less specific and unequal. */
  private def moreSpecific(t: Type, u: Type): Boolean =
    noLessSpecific(t, u) && !analyzer.equivalent(t, u).isTrue

  /** The `noLessSpecific` relation. */
  private def noLessSpecific(t: Type, u: Type): Boolean =
    analyzer.subtype(t, u).isTrue ||
      (exclusions.excludes(t, u) && coercesTo(t, u) && rejects(t, u))

  /** Determines if T rejects U. */
  private def rejects(t: Type, u: Type): Boolean =
    getCoercionsTo(t).forall(a => exclusions.excludes(a.getDomain, u))

  /** The set of all types T such that T --> U. Nil if u not a trait type. */
  def getCoercionsTo(u: Type): Set[ArrowType] = {
    if (!u.isInstanceOf[TraitType]) return Set()

    // Get name and possible static args out of the type.
    val STraitType(_, name, sargs, _) = u

    // Get all the coercions from the type U.
    val coercions = toOption(traits.typeCons(name)) match {
      case Some(ti:TraitIndex) => toSet(ti.coercions)
      case None => Set()
    }

    // Get the instantiated coercion arrow.
    def instantiateArrow(c: Coercion): Option[ArrowType] = {
      makeArrowFromFunctional(c).flatMap(arrow =>
        instantiateLiftedStaticParams(sargs, arrow).map(instArrow =>
          instArrow.asInstanceOf[ArrowType]))
    }

    // Get all the arrows that were found.
    coercions.flatMap(instantiateArrow)
  }

  /** Determines if T is substitutable for U. */
  def substitutableFor(t: Type, u: Type): Boolean =
    analyzer.subtype(t, u).isTrue || coercesTo(t, u)

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
    if (analyzer.subtype(t, u).isTrue)
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
    case (_, u:TraitType) => checkCoercionTrait(t, u, expr)
    case (t:TupleType, u:TupleType) => checkCoercionTuple(t, u, expr)
    case (t:ArrowType, u:ArrowType) => checkCoercionArrow(t, u, expr)
    case _ => None
  }

  private def checkCoercionTrait(t: Type,
                                 u: TraitType,
                                 maybeArg: Option[Expr])
                                 : Option[Option[TraitCoercionInvocation]] = {
    // Make the dummy argument.
    val dummyArg = makeDummyFor(t)

    // Get a list of all the (arrow, inferred args) pairs that worked.
    val allArrows = getCoercionsTo(u)
    val arrowsAndArgs = allArrows.flatMap(inferStaticParams(_, t, None))
    if (arrowsAndArgs.isEmpty) return None
    else if (maybeArg.isNone) return Some(None)
    val arg = maybeArg.get
    val argSpan = NU.getSpan(arg)

    // Build app candidates and sort them to find the SMA.
    val candidates = arrowsAndArgs.map(aa => (aa._1, aa._2, List(arg)))
    val (smaArrow, unliftedSargs, _) = candidates.toList.sort(moreSpecificCandidate).first

    // Make a dummy overloading for all coercions to U.
    val coercionId = makeCoercionId(u)
    val ovType = NF.makeIntersectionType(toJavaSet(allArrows))
    val overloading = SOverloading(SSpanInfo(argSpan), coercionId, Some(ovType))

    // Make a dummy functional ref.
    val sargs = toList(u.getArgs) ++ unliftedSargs
    val fnRef = EF.makeFnRef(argSpan,
                             false,
                             coercionId,
                             toJavaList(Nil),
                             toJavaList(sargs),
                             toJavaList(List(overloading)))

    // Rewrite the FnRef so that it filters out dynamically unapplicable arrows.
    val newFnRef = rewriteApplicand(fnRef, smaArrow, sargs).asInstanceOf[FnRef]

    // Make the coercion invocation!
    Some(Some(STraitCoercionInvocation(SExprInfo(argSpan, false, Some(u)), arg, u, newFnRef)))
  }

  private def checkCoercionTuple(t: TupleType,
                                 u: TupleType,
                                 maybeArg: Option[Expr])
                                 : Option[Option[TupleCoercionInvocation]] = {
    val (x@STupleType(_, xelts, xvar, _), y@STupleType(_, yelts, yvar, _)) = (t, u)

    // 1. X is not a subtype of Y.
    if (analyzer.subtype(x, y).isTrue) return None

    // 2. If X has more elts, then create the coercions for them.
    if (xelts.length >= yelts.length) return None
    var xCoercionElts = List.map2(xelts, yelts)((xelt, yelt) => {
      val maybeDummy = maybeArg.map(_ => makeDummyFor(xelt))
      checkSubstitutable(xelt, yelt, maybeDummy)
    })

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
        xelts.drop(yelts.length).map(xelt => {
          val maybeDummy = maybeArg.map(_ => makeDummyFor(xelt))
          checkSubstitutable(xelt, yvar.get, maybeDummy)
        })
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
    val c = toOption(teff.getThrowsClause).map(toList[BaseType]).getOrElse(List[BaseType]())
    val f = toOption(ueff.getThrowsClause).map(toList[BaseType]).getOrElse(List[BaseType]())

    // 1. A -> B throws C is not a subtype of D -> E throws F
    if (analyzer.subtype(t, u).isTrue) return None

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
