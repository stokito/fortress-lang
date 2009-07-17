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
import com.sun.fortress.scala_src.nodes._
import com.sun.fortress.compiler.index.{Coercion, CompilationUnitIndex, TraitIndex}
import com.sun.fortress.compiler.typechecker.TypeAnalyzer
import com.sun.fortress.nodes_util.NodeFactory
import com.sun.fortress.scala_src.typechecker.staticenv.KindEnv
import com.sun.fortress.scala_src.useful.ASTGenHelper._
import com.sun.fortress.scala_src.useful.Iterators._
import com.sun.fortress.scala_src.useful.ErrorLog
import com.sun.fortress.scala_src.useful.Lists._
import com.sun.fortress.scala_src.useful.Options._
import com.sun.fortress.scala_src.useful.Sets._
import com.sun.fortress.scala_src.useful.STypesUtil._

class CoercionOracle(traits: TraitTable,
                     exclusions: ExclusionOracle)
                    (implicit analyzer: TypeAnalyzer) {

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
    getCoercionsTo(t).forall(a => exclusions.excludes(a, u))

  /** The set of all types T such that T --> U. Nil if u not a trait type. */
  def getCoercionsTo(u: Type): Set[Type] = {
    if (!u.isInstanceOf[TraitType]) return Set()

    // Get name and possible static args out of the type.
    val STraitType(_, name, sargs, _) = u

    // Get all the coercions from the type U.
    val coercions = toOption(traits.typeCons(name)) match {
      case Some(ti:TraitIndex) => toSet(ti.coercions)
      case None => Set()
    }

    // Get the domain from an instantiated coercion arrow.
    def getDomain(c: Coercion): Option[Type] = {
      makeArrowFromFunctional(c).flatMap(arrow =>
        staticInstantiation(sargs, arrow).map(instArrow =>
          instArrow.asInstanceOf[ArrowType].getDomain))
    }

    // Get all the domains that were found.
    coercions.flatMap(getDomain)
  }

  /** Determines if T is substitutable for U. */
  def substitutableFor(t: Type, u: Type): Boolean =
    analyzer.subtype(t, u).isTrue || coercesTo(t, u)

  /** Determines if T ~~> U. */
  def coercesTo(t: Type, u: Type): Boolean = (t, u) match {

    case (_, u:TraitType) =>
      getCoercionsTo(u).exists(tt => analyzer.subtype(t, tt).isTrue)

    case (x@STupleType(_, xelts, xvar, _), y@STupleType(_, yelts, yvar, _)) => {
      val rules = List[Boolean](
        // 1. X is not a subtype of Y.
        !analyzer.subtype(x, y).isTrue,

        // 2. for every T in Y, corresponding type in X is substitutable for T
        xelts.length >= yelts.length && List.forall2(xelts, yelts)(substitutableFor),

        // 3. same number of plain types if neither have varargs
        (xelts.length == yelts.length || yvar.isDefined),

        // 4. check varargs types
        (!(xvar.isDefined && yvar.isDefined) || substitutableFor(xvar.get, yvar.get)),

        // 5. check remainder of x is substitutable for y's varargs
        (!yvar.isDefined || xelts.drop(yelts.length).forall(substitutableFor(_, yvar.get)))
      )
      rules.forall(identity)
    }

    case (SArrowType(_, a, b, teff, _), SArrowType(_, d, e, ueff, _)) => {
      // Get throws types.
      val c = toOption(teff.getThrowsClause).map(toList[BaseType]).getOrElse(List[BaseType]())
      val f = toOption(ueff.getThrowsClause).map(toList[BaseType]).getOrElse(List[BaseType]())
            
      val rules = List[Boolean](
        // 1. A -> B throws C is not a subtype of D -> E throws F
        !analyzer.subtype(t, u).isTrue,

        // 2. D substitutable for A
        substitutableFor(d, a),

        // 3. B substitutable for E
        substitutableFor(b, e),

        // 4. for all X in C, there is a Y in F such that X is substitutable for Y
        c.forall(x => f.exists(y => substitutableFor(x, y)))
      )
      rules.forall(identity)
    }

    case _ => false
  }
}
