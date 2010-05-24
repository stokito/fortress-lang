/*******************************************************************************
    Copyright 2010 Sun Microsystems, Inc.,
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

package com.sun.fortress.scala_src.types

import com.sun.fortress.nodes._
import com.sun.fortress.scala_src.typechecker._
import com.sun.fortress.scala_src.typechecker.staticenv.KindEnv

/**
 * This oracle determines the constraints on type parameters under which two
 * types or type schemas do not exclude. Such constraints need to be
 * incorporated into the oracle that determines whether two generic functional
 * declarations are valid overloadings.
 *
 * This trait must be mixed into the TypeAnalyzer.
 */
trait NonExclusionOracle { self: TypeAnalyzer =>
  
  /**
   * Determine the constraints under which `t` and `u` may have a common
   * non-bottom subtype.
   */
  def nonExclusion(t: Type, u: Type): ConstraintFormula =
    nexc(removeSelf(t), removeSelf(u))(CnTrue)
  
  /** Like `nonExclusion` but takes some input constraints to satisfy. */
  def nexc(t: Type, u: Type)(implicit cf: ConstraintFormula) = (t, u) match {
      
    // If either is bottom, intersection definitely bottom. Exclude!
    case (t: BottomType, _) => CnFalse
    case (_, u: BottomType) => CnFalse
    
    // If either is any, intersection definitely not bottom. Not exclude!
    case (t: AnyType, _) => CnTrue
    case (_, u: AnyType) => CnTrue
    
    
    // EVERYTHING BELOW NEEDS UPDATING!
    case _ => null
        // 
        // case (t@SVarType(_, id, _), u) =>
        //   val sParam = staticParam(id)
        //   val supers = toListFromImmutable(sParam.getExtendsClause)
        //   supers.exists(nexc(_, t))
        // case (s, t:VarType) => nexc(t, s)
        // 
        // // ToDo: Make sure that two traits with the same exclude each other
        // // if their parameters are definitely different
        // case (s@STraitType(_, n1, a1, _), t@STraitType(_, n2, a2, _)) =>
        //   val sExcludes = excludesClause(s)
        //   val tExcludes = excludesClause(t)
        //   if (sExcludes.exists(sub(t, _).isTrue))
        //     return true
        //   if (tExcludes.exists(sub(s, _).isTrue))
        //     return true
        //   val sIndex = typeCons(n1).asInstanceOf[TraitIndex]
        //   val tIndex = typeCons(n2).asInstanceOf[TraitIndex]
        //   (sIndex, tIndex) match {
        //     case (si: ProperTraitIndex, ti: ProperTraitIndex) =>
        //       val sComprises = comprisesClause(s)
        //       val tComprises = comprisesClause(t)
        //       if (!sComprises.isEmpty && sComprises.forall(exc(t, _)))
        //         return true
        //       if (!tComprises.isEmpty && tComprises.forall(exc(s, _)))
        //         return true
        //       false
        //     case _ =>
        //       or(sub(s, t), sub(t, s)).isFalse
        //   }
        // case (s: ArrowType, t: ArrowType) => false
        // case (s: ArrowType, _) => true
        // case (_, t: ArrowType) => true
        // // ToDo: Handle keywords
        // case (STupleType(_, e1, mv1, _), STupleType(_, e2, mv2, _)) =>
        //   val excludes = (e1, e2).zipped.exists((a, b) => exc(a, b))
        //   val different = (mv1, mv2) match {
        //     case (Some(v1), _) if (e1.size < e2.size) =>
        //       e2.drop(e1.size).exists(exc(_, v1))
        //     case (_, Some(v2)) if (e1.size > e2.size) =>
        //       e1.drop(e2.size).exists(exc(_, v2))
        //     case _ if (e1.size!=e2.size) => true
        //     case _ => false
        //   }
        //   different || excludes
        // case (s: TupleType, _) => true
        // case (_, t: TupleType) => true
        // case (s@SIntersectionType(_, elts), t) =>
        //   elts.exists(exc(_, t))
        // case (s, t: IntersectionType) => exc(t, s)
        // case (s@SUnionType(_, elts), t) =>
        //   elts.forall(exc(_, t))
        // case (s, t: UnionType) => exc(t, s)
        // case _ => false
  }
}
