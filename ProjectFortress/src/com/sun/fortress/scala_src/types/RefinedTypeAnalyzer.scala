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

import _root_.junit.framework._
import _root_.junit.framework.Assert._
import com.sun.fortress.exceptions.InterpreterBug.bug
import com.sun.fortress.nodes._
import com.sun.fortress.nodes_util.{NodeFactory => NF}
import com.sun.fortress.nodes_util.{NodeUtil => NU}
import com.sun.fortress.scala_src.nodes._
import com.sun.fortress.scala_src.typechecker._
import com.sun.fortress.scala_src.useful.Lists._
import com.sun.fortress.scala_src.useful.Options._
import com.sun.fortress.scala_src.useful.STypesUtil._
import com.sun.fortress.scala_src.useful.TypeParser

import scala.collection.mutable.HashMap

/**
 * A RefinedTypeAnalyzer wraps a TypeAnalyzer in a refining substitution so
 * that all judgments will be made in the range of the substitution.
 */
class RefinedTypeAnalyzer(parent: TypeAnalyzer, refiner: Type => Type)
    extends TypeAnalyzer(parent.traits, parent.env) {
  
  /**
   * Refines the type and then uses the parent's normalize method. This
   * affects all type judgments since each such method first calls normalize
   * on its type arguments. Therefore, any calls to the parent will involve
   * those types in the range of the refiner.
   */
  override def normalize(t: Type): Type = parent.normalize(refiner(t))
}

object RefinedTypeAnalyzer {
  
  /**
   * Creates a RefinedTypeAnalyzer from some parent TypeAnalyzer and two types
   * that by assumption do not exclude.
   */
  def maybeMake(parent: TypeAnalyzer,
                tvT: Type,
                tvU: Type,
                skolems: Option[WhereClause])
               : Option[RefinedTypeAnalyzer] = {
    
    // Extend the analyzer with the static params bound as skolems.
    implicit val taWithSkolems = parent.extend(Nil, skolems)
    
    // Get all the type variables of T and U.
    val tvVars = getVarTypes(tvT) union getVarTypes(tvU)
    
    // Create bijective mappings between type vars and inference vars.
    val tv2ivMap = new HashMap[VarType, _InferenceVarType]
    val iv2tvMap = new HashMap[_InferenceVarType, VarType]
    val ivVars = tvVars map { tvX =>
      val ivX = NF.make_InferenceVarType(NU.getSpan(tvX))
      tv2ivMap(tvX) = ivX
      iv2tvMap(ivX) = tvX
      ivX
    }
    
    // Make full substitutions whose domains are all types.
    val tv2iv = liftTypeSubstitution(tv2ivMap)
    val iv2tv = liftTypeSubstitution(iv2tvMap)
    
    // Create constraint formula for these inference vars and their bounds.
    val ivBoundsConstraints = (tv2ivMap foldLeft (True:CFormula)) {
      case (cf, (tvX, ivX)) =>
      
        // Get the bound of this type var as an intersection and add to formula.
        val spX = taWithSkolems.staticParam(tvX.getName)
        val bound = NF.makeIntersectionType(spX.getExtendsClause)
        // println("adding bound: %s <: %s".format(ivX, tv2iv(bound)))
        Formula.and(cf, taWithSkolems.subtype(ivX, tv2iv(bound)))
    }
    
    // Transform T and U to contain inference vars.
    val ivT = tv2iv(tvT)
    val ivU = tv2iv(tvU)
    
    // Create constraint formula for the non-exclusion of T and U AND the
    // bounds for the inference variables.
    // println("bounds constraints: %s".format(ivBoundsConstraints))
    // println("nonExclusion: %s".format(taWithSkolems.notExclude(ivT, ivU)))
    val ivConstraints = Formula.and(taWithSkolems.notExclude(ivT, ivU),
                                    ivBoundsConstraints)
    
    // Unify the equality constraints for a refining substitution and
    // simplified bounds constraints.
    // println("ivConstraints: %s".format(ivConstraints))
    val (ivBoundsRefined, ivRefiner) = Formula.unify(ivConstraints).getOrElse{return None}
    
    // The remaining inference variables correspond to the new, shadowing
    // static params that need to be added to the kind env; their bounds are
    // given in the formula.
    // TODO: reduce ivBoundsRefined first?
    // println("ivBoundsRefined: %s".format(ivBoundsRefined))
    val ivBoundsTriples = Formula.toTriples(ivBoundsRefined).getOrElse{return None}
    val newStaticParams = ivBoundsTriples map { case (ivLbs, ivX, ivUbs) =>
    
      // Map them all into type variables again.
      val (tvLbs, tvX, tvUbs) = (ivLbs map iv2tv, iv2tvMap(ivX), ivUbs map iv2tv)
      
      assert(tvLbs.isEmpty)
      assert(tvUbs.forall(_.isInstanceOf[BaseType]))
      val tvXextends = toJavaList(tvUbs.toList.asInstanceOf[List[BaseType]])
      
      // Make the static param.
      NF.makeTypeParam(NU.getSpan(tvX), tvX.getName, tvXextends, none[Type], false)
    }
    
    // Turn the iv refiner into a tv refiner.
    val tvRefiner = ivRefiner compose tv2iv
    
    // Create it!
    // TODO: use `parent` or `taWithSkolems` as parent??
    Some(new RefinedTypeAnalyzer(parent, tvRefiner))
  }
}






class RefinedTypeAnalyzerJUTest extends TestCase {
  
  def typeAnalyzer(str:String) = TypeParser.parse(TypeParser.typeAnalyzer, str).get
  def typ(str: String) = TypeParser.parse(TypeParser.typ, str).get

  // Type analyzer to use.
  val baseTa = typeAnalyzer("""{
    trait Aa,
    trait ZZ extends {Eq[ZZ]} excludes {String},
    trait String extends {Eq[T]} excludes {ZZ},
    object MyString extends {String},
    trait Pair[T,U],
    trait Eq[T],
    trait List[T],
    trait Array[T],
    trait ArrayList[T]}""")
  
  // Conveniently extend a type analyzer.
  def extend(ta: TypeAnalyzer, sps: StaticParam*): TypeAnalyzer =
    ta.extend(sps.toList, None)
  def extend(ta: TypeAnalyzer, wc: WhereClause, sps: StaticParam*): TypeAnalyzer =
    ta.extend(sps.toList, Some(wc))
  
  // Create a dummy type parameter named `x`.
  def typeParam(x: String): StaticParam = typeParam(x, Nil)
  
  // Create a dummy type parameter named `x` with supers `exts`.
  def typeParam(x: String, exts: List[Type]): StaticParam = {
    val extss = toJavaList(exts.asInstanceOf[List[BaseType]])
    NF.makeTypeParam(NF.typeSpan, NF.makeId(NF.typeSpan, x), extss, none[Type], false)
  }
  
  // Skolemize a type parameter by binding it in a where clause.
  def skolem(x: StaticParam): WhereClause = skolem(List(x))
  
  // Skolemize multiple type parameters by binding them in a where clause.
  def skolem(xs: List[StaticParam]): WhereClause = {
    val bs = xs.map { x =>
      new WhereBinding(x.getInfo,
                       x.getName.asInstanceOf[Id],
                       x.getExtendsClause,
                       SKindType())
    }
    SWhereClause(xs.head.getInfo, bs, Nil)
  }
  
  // Equivalence shortcut.
  def eqTypes(ta: TypeAnalyzer, t: Type, u: Type): Boolean =
    Formula.isTrue(ta.equivalent(t, u))(ta)
    
  // Wrapper that will issue junit failure.
  def makeOrFail(ta: TypeAnalyzer,
                 t: Type,
                 u: Type,
                 sk: Option[WhereClause]): RefinedTypeAnalyzer =
    RefinedTypeAnalyzer.maybeMake(ta, t, u, sk)
                       .getOrElse{ fail(); null: RefinedTypeAnalyzer }
  
  
  // Basic tests that do not involve skolems or bounds.
  def testBasic = {
    val ta = extend(baseTa, typeParam("T"), typeParam("S"))
    
    // {
    //   val t = typ("T")
    //   val u = typ("Aa")
    //   val rta = makeOrFail(ta, t, u, None)
    //   assertTrue(eqTypes(rta, t, u))
    // }
    
    {
      // remember: invariant list
      val t = typ("List[T]")
      val u = typ("List[Aa]")
      val rta = makeOrFail(ta, t, u, None)
      assertTrue(eqTypes(rta, typ("T"), typ("Aa")))
      assertTrue(eqTypes(rta, t, u))
    }
    
    {
      val t = typ("Pair[S, T]")
      val u = typ("Pair[Aa, String]")
      val rta = makeOrFail(ta, t, u, None)
      assertTrue(eqTypes(rta, typ("S"), typ("Aa")))
      assertTrue(eqTypes(rta, typ("T"), typ("String")))
      assertTrue(eqTypes(rta, t, u))
    }
    
    {
      val t = typ("Pair[S, T]")
      val u = typ("Pair[Aa, T]")
      val rta = makeOrFail(ta, t, u, None)
      assertTrue(eqTypes(rta, typ("S"), typ("Aa")))
      assertFalse(eqTypes(rta, typ("T"), typ("S")))
      assertTrue(eqTypes(rta, t, u))
    }
  }
}