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

import scala.collection.immutable.HashMap
import com.sun.fortress.nodes._
import com.sun.fortress.nodes_util.NodeFactory
import com.sun.fortress.compiler.Types.ANY
import com.sun.fortress.compiler.Types.BOTTOM
import com.sun.fortress.compiler.typechecker.SubtypeHistory
import com.sun.fortress.compiler.typechecker.InferenceVarReplacer
import com.sun.fortress.compiler.typechecker.SubtypeHistory
import edu.rice.cs.plt.lambda.Lambda
import com.sun.fortress.compiler.typechecker.constraints.ConstraintFormula;
import com.sun.fortress.exceptions.InterpreterBug.bug;
import com.sun.fortress.scala_src.useful.Maps

/**
 * This class represents the constraints accummulated on inference variables. All
 * constraints are kept in disjunctive normal form. In scalaOrder to keep the size of
 * the scalaOr method eliminates redundant constraints. Further information can be found
 * in Section 3.2.2 of Dan Smith's paper Java Type Inference is Broken.
 * 
 * Currently it also works as a wrapper to interface with the Java Constraint Formulas
 */

sealed abstract class ScalaConstraint extends ConstraintFormula{
  /**
   * This method scalaAnds two constraint formulas
   */
  def scalaAnd(c2 :ScalaConstraint, newHistory: SubtypeHistory): ScalaConstraint

  /**
   * This method scalaOrs two constraint formulas
   */
  def scalaOr(c2: ScalaConstraint, newHistory: SubtypeHistory): ScalaConstraint

   /**
   * If the constraints are satisfiable then this returns a substitution of types for
   * inference variables that satisfies the constraints.
   */
  def scalaSolve(): Option[Map[_InferenceVarType,Type]]
  
  override def and(c: ConstraintFormula, h: SubtypeHistory):ConstraintFormula = c match{
    case c2:ScalaConstraint => scalaAnd(c2,h)
    case _ => bug("Can't and a scala formula with a java formula")
  }
  
  override def or(c: ConstraintFormula, h: SubtypeHistory):ConstraintFormula = c match{
    case c2:ScalaConstraint => scalaOr(c2,h)
    case _ => bug("Can't or a scala formula with a java formula")
  }
  
  override def solve() = bug("Use scalaSolve for scala formulas")
}

/**
 * Formulas with no disjunctions
 */
sealed abstract class SimpleFormula extends ScalaConstraint{
  /**
   * This method is used to determine whether a constraint formula is redundant
   */
  def implies(c2: SimpleFormula, newHistory: SubtypeHistory) : Boolean
}


/**
 * The formula with bounds Any:>$i:>Bottom for all $i
 */
case object CnTrue extends SimpleFormula{
  override def scalaAnd(c2: ScalaConstraint, newHistory: SubtypeHistory): ScalaConstraint = c2
  override def scalaOr(c2: ScalaConstraint, newHistory: SubtypeHistory): ScalaConstraint = this
  override def implies(c2: SimpleFormula, newHistory: SubtypeHistory): Boolean = false
  override def scalaSolve(): Option[Map[_InferenceVarType,Type]] = Some(Map.empty)
  override def isTrue():Boolean = true
  override def applySubstitution(substitution: Lambda[Type,Type]): ScalaConstraint = this
}

/**
 * The empty formula
 */
case object CnFalse extends SimpleFormula{
  override def scalaAnd(c2: ScalaConstraint, newHistory: SubtypeHistory): ScalaConstraint = this
  override def scalaOr(c2: ScalaConstraint, newHistory: SubtypeHistory): ScalaConstraint = c2
  override def implies(c2: SimpleFormula, newHistory: SubtypeHistory): Boolean = true
  override def scalaSolve(): Option[Map[_InferenceVarType,Type]] = None
  override def isFalse():Boolean = true
  override def applySubstitution(substitution: Lambda[Type,Type]): ScalaConstraint = this
}


/**
 * This class represents the conjunction of primitive formula of the form
 * U>:$i>:B
 */
case class CnAnd(uppers: Map[_InferenceVarType, Type], lowers: Map[_InferenceVarType, Type], history: SubtypeHistory) extends SimpleFormula{

  override def scalaAnd(c2: ScalaConstraint, newHistory: SubtypeHistory): ScalaConstraint = c2 match{
    case CnTrue => this
    case CnFalse => c2
    case CnAnd(u2,l2,h2) =>
      val newUppers = mergeBounds(uppers,u2,(x:Type, y:Type) => NodeFactory.makeIntersectionType(x,y))
      val newLowers = mergeBounds(lowers,l2,(x:Type, y:Type) => NodeFactory.makeUnionType(x,y))
      if(!compareBounds(lowers,uppers,BOTTOM,ANY,
                       (t1: Type,t2:Type) => newHistory.subtypeNormal(t1,t2).isTrue))
        CnFalse
      else
        CnAnd(newUppers,newLowers,newHistory)
    case CnOr(conjuncts,h2) =>
      val newConjuncts=conjuncts.map((sf: SimpleFormula)=>this.scalaAnd(sf,newHistory))
      newConjuncts.foldRight(CnFalse.asInstanceOf[ScalaConstraint])((c1: ScalaConstraint, c2: ScalaConstraint) => c1.scalaOr(c2,newHistory))
  }

  override def scalaOr(c2: ScalaConstraint, newHistory: SubtypeHistory): ScalaConstraint = c2 match{
    case CnTrue => c2
    case CnFalse => this
    case c2@CnAnd(u2,l2,h2) => (this.implies(c2,newHistory),c2.implies(this,newHistory)) match{
      case (false,false) => CnOr(this::List(c2),newHistory)
      case (true,false) => c2
      case (false,true) => this
      case (true,true) => this
    }
    case CnOr(conjuncts,h2) =>
      if(conjuncts.filter((sf: CnAnd)=> this.implies(sf,newHistory)).isEmpty){
        val minimal = conjuncts.filter((sf: CnAnd)=> !sf.implies(this,newHistory))
        if(minimal.isEmpty)
          this
        else
          CnOr(this::minimal,newHistory)
      }else
        c2
  }

  override def implies(c2: SimpleFormula, newHistory: SubtypeHistory): Boolean = c2 match{
    case CnTrue => true
    case CnFalse => false
    case CnAnd(u2,l2,h2) =>
      val impliesUppers = compareBounds(uppers, u2, ANY , ANY,
                                        (t1:Type, t2:Type) => newHistory.subtypeNormal(t1,t2).isTrue)
      val impliesLowers = compareBounds(lowers, l2, BOTTOM, BOTTOM,
                                        (t1:Type, t2:Type) => newHistory.subtypeNormal(t2,t1).isTrue)
      impliesUppers && impliesLowers
  }

  override def applySubstitution(substitution: Lambda[Type,Type]): CnAnd = {
    val newUppers = new HashMap[_InferenceVarType,Type]
    for(key <- uppers.keys){
      newUppers.update(substitution.value(key).asInstanceOf, substitution.value(uppers.apply(key)))
    }
    val newLowers = new HashMap[_InferenceVarType,Type]
    for(key <- lowers.keys){
      newLowers.update(substitution.value(key).asInstanceOf, substitution.value(lowers.apply(key)))
    }
    CnAnd(newUppers,newLowers,history)
  }

  override def scalaSolve(): Option[Map[_InferenceVarType,Type]] = {
    val bounds: Map[_InferenceVarType,Type] = Map.empty
    if(inbounds(lowers,bounds))
      Some(lowers)
    else
      None
  }
  
  /**
   * Merges the bounds from two conjunctive formulas
   */
  private def mergeBounds(bounds1: Map[_InferenceVarType,Type],
                  bounds2: Map[_InferenceVarType,Type],
                  merge:(Type,Type)=>Type): Map[_InferenceVarType,Type] = {
      val boundKeys = bounds1.keySet ++ bounds2.keySet
      val newBounds = new HashMap[_InferenceVarType, Type]
      for(ivar <- boundKeys){
        val bound1 = bounds1.get(ivar)
        val bound2 = bounds2.get(ivar)
        (bound1,bound2) match{
          case (None, None) => ()
          case (Some(b1), Some(b2)) => newBounds.update(ivar, merge(b1,b2))
          case (Some(b), None) => newBounds.update(ivar,b)
          case (None, Some(b)) => newBounds.update(ivar,b)
        }
      }
      newBounds
  }

  /**
   * Compares two sets of bounds
   */
  private def compareBounds(bounds1: Map[_InferenceVarType,Type],
                    bounds2: Map[_InferenceVarType,Type],
                    defaultBound1: Type,
                    defaultBound2: Type,
                    compare: (Type,Type)=>Boolean): Boolean = {
    val boundKeys = bounds1.keySet ++ bounds2.keySet
    val pred = (ivar: _InferenceVarType) => {
      val bound1 = bounds1.get(ivar)
      val bound2 = bounds2.get(ivar)
      (bound1,bound2) match{
        case (None, None) => true
        case (Some(b1), Some(b2)) => compare(b1,b2)
        case (Some(b), None) => compare(b,defaultBound2)
        case (None, Some(b)) => compare(defaultBound1, b)
      }
    }
    boundKeys.forall(pred)
  }

  /**
   * Determines whether a given substitution of types for
   * inference variables is valid
   */
  private def inbounds(substitutions: Map[_InferenceVarType,Type],bounds: Map[_InferenceVarType,Type]): Boolean = {
    val pred = (ivar: _InferenceVarType) => {
      val bound  = bounds.apply(ivar)
      val replacer=new InferenceVarReplacer(Maps.toJavaMap(substitutions))
      val newBound=bound.asInstanceOf[Node].accept(replacer).asInstanceOf[Type]
      substitutions.get(ivar) match {
        case None => history.subtypeNormal(ANY,newBound).isTrue;
        case Some(substitution) => history.subtypeNormal(substitution,newBound).isTrue;
      }
    }
    bounds.keys.forall(pred)
  }

}


/**
 * A disjunction of simple formulas
 */
case class CnOr( conjuncts: List[CnAnd], history: SubtypeHistory) extends ScalaConstraint{
  override def scalaAnd(c2: ScalaConstraint, newHistory: SubtypeHistory): ScalaConstraint = c2 match{
    case CnFalse => c2
    case CnTrue => this
    case CnAnd(u2,l2,h2) => c2.scalaAnd(this,newHistory)
    case c2@CnOr(conjuncts,h2) =>
      val newConjuncts = conjuncts.map((cf: ScalaConstraint) => cf.scalaAnd(c2,newHistory))
      newConjuncts.foldRight(CnFalse.asInstanceOf[ScalaConstraint])((c1: ScalaConstraint, c2: ScalaConstraint) => c1.scalaOr(c2,newHistory))
  }

  override def scalaOr(c2: ScalaConstraint, newHistory: SubtypeHistory): ScalaConstraint = c2 match{
    case CnFalse => this
    case CnTrue => c2
    case CnAnd(u2,l2,h2) => c2.scalaOr(this,newHistory)
    case CnOr(conjuncts2,h2) =>
      val newConjuncts = conjuncts ++ conjuncts2
      newConjuncts.foldRight(CnFalse.asInstanceOf[ScalaConstraint])((c1: ScalaConstraint, c2: ScalaConstraint) => c1.scalaOr(c2,newHistory))
  }

  override def scalaSolve(): Option[Map[_InferenceVarType,Type]] = {
    for(conjunct <- conjuncts){
      val solved = conjunct.scalaSolve
      if(solved.isDefined)
        return solved
    }
    None
  }

  override def applySubstitution(substitution: Lambda[Type,Type]): ScalaConstraint = 
    CnOr(conjuncts.map((c:CnAnd) => c.applySubstitution(substitution)),history)
}
