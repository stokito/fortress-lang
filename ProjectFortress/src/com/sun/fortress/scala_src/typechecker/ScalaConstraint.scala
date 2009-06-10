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

import com.sun.fortress.nodes._
import com.sun.fortress.nodes_util.NodeFactory
import com.sun.fortress.compiler.Types.ANY
import com.sun.fortress.compiler.Types.BOTTOM
import com.sun.fortress.compiler.Types.OBJECT
import com.sun.fortress.compiler.typechecker.SubtypeHistory
import com.sun.fortress.compiler.typechecker.InferenceVarReplacer
import com.sun.fortress.compiler.typechecker.constraints.ConstraintFormula
import com.sun.fortress.exceptions.InterpreterBug.bug
import com.sun.fortress.scala_src.useful.Maps._
import com.sun.fortress.scala_src.useful.Sets._
import com.sun.fortress.scala_src.useful.STypesUtil
import com.sun.fortress.scala_src.useful.STypesUtil.Subtype
import edu.rice.cs.plt.lambda.Lambda

/**
 * This class represents the constraints accummulated on inference variables. All
 * constraints are kept in disjunctive normal form. In scalaOrder to keep the size of
 * the scalaOr method eliminates redundant constraints. Further information can be found
 * in Section 3.2.2 of Dan Smith's paper Java Type Inference is Broken.
 * 
 * Currently it also works as a wrapper to interface with the Java Constraint Formulas
 * 
 * @TODO: Top type for inference is ANY or BOTTOM?
 */
sealed abstract class ScalaConstraint extends ConstraintFormula{
  /**
   * This method scalaAnds two constraint formulas
   */
  def scalaAnd(c2 :ScalaConstraint, newSubtype: Subtype): ScalaConstraint

  /**
   * This method scalaOrs two constraint formulas
   */
  def scalaOr(c2: ScalaConstraint, newSubtype: Subtype): ScalaConstraint

  /**
   * This method is used to determine whether a constraint formula is redundant
   */
  def implies(c2: ScalaConstraint, newSubtype: Subtype) : Boolean
  
  /**
   * 
   */
  def reduce(): ScalaConstraint
  
  
  override def toString:String
  
   /**
   * If the constraints are satisfiable then this returns a substitution of types for
   * inference variables that satisfies the constraints.
   */
  def scalaSolve(bounds: Map[_InferenceVarType,Type]): Option[Map[_InferenceVarType,Type]]
  
  override def and(c: ConstraintFormula, h: SubtypeHistory):ConstraintFormula = c match{
    case c2:ScalaConstraint => scalaAnd(c2,(t1,t2)=>h.subtypeNormal(t1,t2).isTrue)
    case _ => bug("Can't and a scala formula with a java formula")
  }
  
  override def or(c: ConstraintFormula, h: SubtypeHistory):ConstraintFormula = c match{
    case c2:ScalaConstraint => scalaOr(c2,(t1,t2)=>h.subtypeNormal(t1,t2).isTrue)
    case _ => bug("Can't or a scala formula with a java formula")
  }
  
  override def solve() = bug("Use scalaSolve for scala formulas")
  
}


/**
 * The formula with bounds Any:>$i:>Bottom for all $i
 */
object CnTrue extends ScalaConstraint{
  override def scalaAnd(c2: ScalaConstraint, newSubtype: Subtype): ScalaConstraint = c2.reduce
  
  override def scalaOr(c2: ScalaConstraint, newSubtype: Subtype): ScalaConstraint = this
  
  override def implies(c2: ScalaConstraint, newSubtype: Subtype): Boolean = c2 match {
    case CnTrue => true
    case CnAnd(u,l,h) => CnAnd(Map.empty,Map.empty,newSubtype).implies(c2,newSubtype)
    case CnOr(conjuncts,h) => conjuncts.exists((c: ScalaConstraint)=> this.implies(c,newSubtype))
    case CnFalse => false
  }
  
  override def scalaSolve(bounds: Map[_InferenceVarType,Type]): Option[Map[_InferenceVarType,Type]] = Some(Map.empty)
  
  override def isTrue():Boolean = true
  
  override def applySubstitution(substitution: Lambda[Type,Type]): ScalaConstraint = this

  override def reduce():ScalaConstraint = this

  override def toString():String = "CnTrue"
}

/**
 * The empty formula
 */
case object CnFalse extends ScalaConstraint{
  override def scalaAnd(c2: ScalaConstraint, newSubtype: Subtype): ScalaConstraint = this
  
  override def scalaOr(c2: ScalaConstraint, newSubtype: Subtype): ScalaConstraint = c2.reduce
  
  override def implies(c2: ScalaConstraint, newSubtype: Subtype): Boolean = true
  
  override def scalaSolve(bounds: Map[_InferenceVarType,Type]): Option[Map[_InferenceVarType,Type]] = None
  
  override def isFalse():Boolean = true
  
  override def applySubstitution(substitution: Lambda[Type,Type]): ScalaConstraint = this

  override def reduce():ScalaConstraint = this

  override def toString():String = "CnFalse"
}


/**
 * This class represents the conjunction of primitive formula of the form
 * U>:$i>:B
 */
case class CnAnd(uppers: Map[_InferenceVarType, Type], lowers: Map[_InferenceVarType, Type], subtype: Subtype) extends ScalaConstraint{

  override def scalaAnd(c2: ScalaConstraint, newSubtype: Subtype): ScalaConstraint = c2 match{
    case CnTrue => this.reduce
      
    case CnFalse => CnFalse
    
    case CnAnd(u2,l2,h2) =>
      val newUppers = mergeBounds(uppers,u2,(x:Type, y:Type) => NodeFactory.makeIntersectionType(x,y))
      val newLowers = mergeBounds(lowers,l2,(x:Type, y:Type) => NodeFactory.makeUnionType(x,y))
      CnAnd(newUppers,newLowers,newSubtype).reduce
    
    case CnOr(conjuncts,h2) =>
      val newConjuncts=conjuncts.map((sf: ScalaConstraint)=>this.scalaAnd(sf,newSubtype))
      newConjuncts.foldRight(CnFalse.asInstanceOf[ScalaConstraint])((c1: ScalaConstraint, c2: ScalaConstraint) => c1.scalaOr(c2,newSubtype))
  }

  override def scalaOr(c2: ScalaConstraint, newSubtype: Subtype): ScalaConstraint = c2 match{
    case CnTrue => c2.reduce
    case CnFalse => this.reduce
    case c2@CnAnd(u2,l2,h2) => (this.implies(c2,newSubtype),c2.implies(this,newSubtype)) match{
      case (false,false) => CnOr(this::List(c2),newSubtype).reduce
      case (true,false) => c2.reduce
      case (false,true) => this.reduce
      case (true,true) => c2.reduce
    }
    case CnOr(conjuncts,h2) =>
      if(this.implies(c2,newSubtype))
        return c2.reduce
      val minimal = conjuncts.filter((sf: CnAnd)=> !sf.implies(this,newSubtype))
      CnOr(this::minimal,newSubtype).reduce
  }

  override def implies(c2: ScalaConstraint, newSubtype: Subtype): Boolean = c2 match{
    case CnTrue => true
    //the CnFalse case is almost certainly wrong
    case CnFalse => !compareBounds(lowers,uppers,BOTTOM,ANY,(t1: Type,t2:Type) => newSubtype(t1,t2))
    case CnAnd(u2,l2,h2) =>
      val impliesUppers = compareBounds(uppers, u2, ANY , ANY,
                                        (t1:Type, t2:Type) => newSubtype(t1,t2))
      val impliesLowers = compareBounds(lowers, l2, BOTTOM, BOTTOM,
                                        (t1:Type, t2:Type) => newSubtype(t2,t1))
      impliesUppers && impliesLowers
    case CnOr(conjuncts,h) => conjuncts.exists((c: ScalaConstraint)=>this.implies(c,newSubtype)) 
  }

  override def scalaSolve(bounds: Map[_InferenceVarType,Type]): Option[Map[_InferenceVarType,Type]] = {
    if(!isFalse && inBounds(lowers,bounds))
      Some(lowers)
    else
      None
  }
  
  override def isFalse(): Boolean = this.implies(CnFalse, subtype)
  
  override def isTrue(): Boolean = CnTrue.implies(this,subtype)
  
  override def reduce(): ScalaConstraint = {
    if(this.isTrue)
      return CnTrue
    if(this.isFalse)
      return CnFalse
    this
  }
  
  override def applySubstitution(substitution: Lambda[Type,Type]): CnAnd = {
    var newUppers = Map.empty[_InferenceVarType,Type]
    for(key <- uppers.keys){
      newUppers=newUppers.update(substitution.value(key).asInstanceOf, substitution.value(uppers.apply(key)))
    }
    var newLowers = Map.empty[_InferenceVarType,Type]
    for(key <- lowers.keys){
      newLowers=newLowers.update(substitution.value(key).asInstanceOf, substitution.value(lowers.apply(key)))
    }
    CnAnd(newUppers,newLowers,subtype)
  }  
  
  /**
   * Merges the bounds from two conjunctive formulas
   */
  private def mergeBounds(bounds1: Map[_InferenceVarType,Type],
                  bounds2: Map[_InferenceVarType,Type],
                  merge:(Type,Type)=>Type): Map[_InferenceVarType,Type] = {
      val boundKeys = bounds1.keySet ++ bounds2.keySet
      var newBounds = Map.empty[_InferenceVarType,Type]
      for(ivar <- boundKeys){
        val bound1 = bounds1.get(ivar)
        val bound2 = bounds2.get(ivar)
        (bound1,bound2) match{
          case (None, None) => ()
          case (Some(b1), Some(b2)) => newBounds=newBounds.update(ivar, merge(b1,b2))
          case (Some(b), None) => newBounds=newBounds.update(ivar,b)
          case (None, Some(b)) => newBounds=newBounds.update(ivar,b)
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
  private def inBounds(substitutions: Map[_InferenceVarType,Type],bounds: Map[_InferenceVarType,Type]): Boolean = {
    val pred = (ivar: _InferenceVarType) => {
      val bound  = bounds(ivar)
      val newBound=STypesUtil.substituteTypesForInferenceVars(substitutions, bound)
      substitutions.get(ivar) match {
        case None => subtype(ANY,newBound)
        case Some(substitution) => subtype(substitution,newBound)
      }
    }
    bounds.keys.forall(pred)
  }

  override def toString():String = {
    val result = new StringBuffer("[") 
    val allKeys = uppers.keySet ++ lowers.keySet
    var counter = 1
    for(ivar <- allKeys){
      val upperBound = uppers.get(ivar)
      val lowerBound = lowers.get(ivar)
      val upperString = if(upperBound.isDefined) upperBound.get.toString else "AnyType"
      val lowerString = if(lowerBound.isDefined) lowerBound.get.toString else "BottomType"
      result.append(lowerString)
      result.append("<:")
      result.append(ivar)
      result.append("<:")
      result.append(upperString)
      if(counter<allKeys.size)
        result.append(", ")
      counter += 1  
    }
    result.append("]")
    result.toString
  }
  
}


/**
 * A disjunction of simple formulas
 */
case class CnOr(conjuncts: List[CnAnd], subtype: Subtype) extends ScalaConstraint{
  
  override def scalaAnd(c2: ScalaConstraint, newSubtype: Subtype): ScalaConstraint = c2 match{
    case CnFalse => CnFalse
    case CnTrue => this.reduce
    case CnAnd(u,l,h) => c2.scalaAnd(this,newSubtype)
    case CnOr(conjuncts2,h2) =>
      val newConjuncts = conjuncts.map((cf: ScalaConstraint) => cf.scalaAnd(c2,newSubtype))
      newConjuncts.foldRight(CnFalse.asInstanceOf[ScalaConstraint])((c1: ScalaConstraint, c2: ScalaConstraint) => c1.scalaOr(c2,newSubtype))
  }

  override def scalaOr(c2: ScalaConstraint, newSubtype: Subtype): ScalaConstraint = c2 match{
    case CnFalse => this.reduce
    case CnTrue => CnTrue
    case CnAnd(u,l,h) => c2.scalaOr(this,newSubtype)
    case CnOr(conjuncts2,h2) =>
      val newConjuncts = conjuncts.union(conjuncts2)
      newConjuncts.foldRight(CnFalse.asInstanceOf[ScalaConstraint])((c1: ScalaConstraint, c2: ScalaConstraint) => c1.scalaOr(c2,newSubtype))
  }

  override def scalaSolve(bounds: Map[_InferenceVarType,Type]): Option[Map[_InferenceVarType,Type]] = {
    for(conjunct <- conjuncts){
      val solved = conjunct.scalaSolve(bounds)
      if(solved.isDefined)
        return solved
    }
    None
  }

  override def implies(c2: ScalaConstraint, newSubtype: Subtype) = conjuncts.forall((c: ScalaConstraint) => c.implies(c2,newSubtype))
  
  override def isFalse(): Boolean = this.implies(CnFalse, subtype)
  
  override def isTrue(): Boolean = CnTrue.implies(this,subtype)
  
  override def applySubstitution(substitution: Lambda[Type,Type]): ScalaConstraint = 
    CnOr(conjuncts.map((c:CnAnd) => c.applySubstitution(substitution)),subtype)
  
  override def reduce(): ScalaConstraint = {
    if(this.isTrue)
      return CnTrue
    if(this.isFalse)
      return CnFalse
    if(conjuncts.size == 1)
      return conjuncts.first.reduce
    this
  }
  
  override def toString():String = {
    val result = new StringBuffer()
    var counter = 1
    for(conjunct <-conjuncts){
      result.append(conjunct)
      if(counter < conjuncts.size)
        result.append(" OR ")
      counter += 1
    }
    result.toString
  }
}


