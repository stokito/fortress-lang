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

import com.sun.fortress.exceptions.InterpreterBug.bug
import com.sun.fortress.nodes._
import com.sun.fortress.nodes_util.{NodeFactory => NF}
import com.sun.fortress.nodes_util.{NodeUtil => NU}
import com.sun.fortress.scala_src.typechecker._
import com.sun.fortress.scala_src.useful.Lists._
import com.sun.fortress.scala_src.useful.Options._
import com.sun.fortress.scala_src.useful.STypesUtil._

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
               : Option[TypeAnalyzer] = {
                               
    // TEMP: implicit def
    import TypeAnalyzer.constraintFormulaConversion
    
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
    val iv2tv = liftTypeSubstitution(tv2ivMap)
    
    // Create constraint formula for these inference vars and their bounds.
    val ivBoundsConstraints = (tv2ivMap foldLeft (True:CFormula)) {
      case (cf, (tvX, ivX)) =>
      
        // Get the bound of this type var as an intersection and add to formula.
        val spX = taWithSkolems.staticParam(tvX.getName)
        val bound = NF.makeIntersectionType(spX.getExtendsClause)
        Formula.and(cf, taWithSkolems.subtype(ivX, tv2iv(bound)))
    }
    
    // Transform T and U to contain inference vars.
    val ivT = tv2iv(tvT)
    val ivU = tv2iv(tvU)
    
    // Create constraint formula for the non-exclusion of T and U AND the
    // bounds for the inference variables.
    val ivConstraints = Formula.and(taWithSkolems.notExclude(ivT, ivU),
                                    ivBoundsConstraints)
    
    // Unify the equality constraints for a refining substitution and
    // simplified bounds constraints.
    val (ivBoundsRefined, refiner) = Formula.unify(ivConstraints).getOrElse{return None}
    
    // The remaining inference variables correspond to the new, shadowing
    // static params that need to be added to the kind env; their bounds are
    // given in the formula.
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
    
    // Create it!
    // TODO: use `parent` or `taWithSkolems` as parent??
    Some(new RefinedTypeAnalyzer(parent, refiner))
  }
}