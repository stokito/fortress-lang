/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.scala_src.typechecker

import scala.Option
import com.sun.fortress.nodes._
import com.sun.fortress.nodes_util.NodeFactory
import com.sun.fortress.compiler.Types.ANY
import com.sun.fortress.compiler.Types.BOTTOM
import com.sun.fortress.compiler.Types.OBJECT
import com.sun.fortress.exceptions.InterpreterBug.bug
import com.sun.fortress.scala_src.types.TypeAnalyzer
import com.sun.fortress.scala_src.useful.Maps._
import com.sun.fortress.scala_src.useful.Sets._
import com.sun.fortress.scala_src.useful.STypesUtil
import edu.rice.cs.plt.lambda.Lambda

/**
 * This class represents the constraints accumulated on inference variables. All
 * constraints are kept in disjunctive normal form. In order to keep the size of
 * the constraints reasonable the or method eliminates redundant constraints. Further 
 * information can be found in Section 3.2.2 of Dan Smith's paper Java Type Inference
 * is Broken.
 * 
 * Currently it also works as a wrapper to interface with the Java Constraint Formulas
 * 
 * @TODO: Top type for inference is ANY or Object?
 */
sealed abstract class ConstraintFormula {
  /**
   * This method ands two constraint formulas
   */
  def and(c2 :ConstraintFormula, newTa: TypeAnalyzer): ConstraintFormula

  /**
   * This method ors two constraint formulas
   */
  def or(c2: ConstraintFormula, newTa: TypeAnalyzer): ConstraintFormula

  /**
   * This method is used to determine whether a constraint formula is redundant
   */
  def implies(c2: ConstraintFormula, newTa: TypeAnalyzer) : Boolean
  
  def isTrue(): Boolean = false
  
  def isFalse(): Boolean = false
  
  /**
   * 
   */
  def reduce(): ConstraintFormula
  
  
  override def toString:String
  
   /**
   * If the constraints are satisfiable then this returns a substitution of types for
   * inference variables that satisfies the constraints.
   */
  def solve(bounds: Map[_InferenceVarType,Type]): Option[Map[_InferenceVarType,Type]]
  
}


/**
 * The formula with bounds Any:>$i:>Bottom for all $i
 */
object CnTrue extends ConstraintFormula{
  override def and(c2: ConstraintFormula, newTa: TypeAnalyzer): ConstraintFormula = c2.reduce
  
  override def or(c2: ConstraintFormula, newTa: TypeAnalyzer): ConstraintFormula = this
  
  override def implies(c2: ConstraintFormula, newTa: TypeAnalyzer): Boolean = c2 match {
    case CnTrue => true
    case CnAnd(u, l, t) => CnAnd(Map.empty,Map.empty, newTa).implies(c2, newTa)
    case CnOr(conjuncts, t) => conjuncts.exists((c: ConstraintFormula)=> this.implies(c, newTa))
    case CnFalse => false
  }
  
  override def solve(bounds: Map[_InferenceVarType,Type]): Option[Map[_InferenceVarType,Type]] = Some(Map.empty)
  
  override def isTrue():Boolean = true

  override def reduce():ConstraintFormula = this

  override def toString():String = "CnTrue"
}

/**
 * The empty formula
 */
case object CnFalse extends ConstraintFormula{
  override def and(c2: ConstraintFormula, newTa: TypeAnalyzer): ConstraintFormula = this
  
  override def or(c2: ConstraintFormula, newTa: TypeAnalyzer): ConstraintFormula = c2.reduce
  
  override def implies(c2: ConstraintFormula, newTa: TypeAnalyzer): Boolean = true
  
  override def solve(bounds: Map[_InferenceVarType,Type]): Option[Map[_InferenceVarType,Type]] = None
  
  override def isFalse():Boolean = true

  override def reduce():ConstraintFormula = this

  override def toString():String = "CnFalse"
}


/**
 * This class represents the conjunction of primitive formula of the form
 * U>:$i>:B
 */
case class CnAnd(uppers: Map[_InferenceVarType, Type], lowers: Map[_InferenceVarType, Type], ta: TypeAnalyzer) extends ConstraintFormula{

  override def and(c2: ConstraintFormula, newTa: TypeAnalyzer): ConstraintFormula = c2 match{
    case CnTrue => this.reduce
      
    case CnFalse => CnFalse
    
    case CnAnd(u2, l2, t2) =>
      val newUppers = mergeBounds(uppers, u2, (x:Type, y:Type) => ta.meet(x, y))
      val newLowers = mergeBounds(lowers, l2, (x:Type, y:Type) => ta.join(x, y))
      CnAnd(newUppers, newLowers, newTa).reduce
    
    case CnOr(conjuncts, t2) =>
      val newConjuncts = conjuncts.map((sf: ConstraintFormula) => this.and(sf, newTa))
      newConjuncts.foldLeft(CnFalse.asInstanceOf[ConstraintFormula])((c1: ConstraintFormula, c2: ConstraintFormula) => c1.or(c2, newTa))
  }

  override def or(c2: ConstraintFormula, newTa: TypeAnalyzer): ConstraintFormula = c2 match{
    case CnTrue => c2.reduce
    case CnFalse => this.reduce
    case c2@CnAnd(u2, l2, t2) => (this.implies(c2, newTa),c2.implies(this, newTa)) match{
      case (false, false) => CnOr(this::List(c2), newTa).reduce
      case (true, false) => c2.reduce
      case (false, true) => this.reduce
      case (true, true) => c2.reduce
    }
    case CnOr(conjuncts,h2) =>
      if(this.implies(c2, newTa))
        return c2.reduce
      val minimal = conjuncts.filter((sf: CnAnd)=> !sf.implies(this, newTa))
      CnOr(this::minimal, newTa).reduce
  }

  override def implies(c2: ConstraintFormula, newTa: TypeAnalyzer): Boolean = c2 match{
    case CnTrue => true
    //the CnFalse case is almost certainly wrong
    case CnFalse => !compareBounds(lowers,uppers,BOTTOM,ANY, newTa.lteq)
    case CnAnd(u2,l2,h2) =>
      val impliesUppers = compareBounds(uppers, u2, ANY , ANY, newTa.lteq)
      val impliesLowers = compareBounds(lowers, l2, BOTTOM, BOTTOM, newTa.gteq)
      impliesUppers && impliesLowers
    case CnOr(conjuncts,h) => conjuncts.exists((c: ConstraintFormula)=>this.implies(c, newTa)) 
  }

  override def solve(bounds: Map[_InferenceVarType,Type]): Option[Map[_InferenceVarType,Type]] = {
    if(!isFalse && inBounds(lowers, bounds))
      Some(lowers)
    else
      None
  }
  
  override def isFalse(): Boolean = this.implies(CnFalse, ta)
  
  override def isTrue(): Boolean = CnTrue.implies(this, ta)
  
  override def reduce(): ConstraintFormula = {
    if(this.isTrue)
      return CnTrue
    if(this.isFalse)
      return CnFalse
    this
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
          case (Some(b1), Some(b2)) => newBounds=newBounds.updated(ivar, merge(b1, b2))
          case (Some(b), None) => newBounds=newBounds.updated(ivar, b)
          case (None, Some(b)) => newBounds=newBounds.updated(ivar, b)
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
        case (Some(b1), Some(b2)) => compare(b1, b2)
        case (Some(b), None) => compare(b, defaultBound2)
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
      val theta: Type=> Type = STypesUtil.liftSubstitution(substitutions)
      val newBound = theta(bounds(ivar))
      substitutions.get(ivar) match {
        case None => ta.lteq(BOTTOM, newBound)
        case Some(substitution) => ta.lteq(substitution, newBound)
      }
    }
    bounds.keysIterator.forall(pred)
  }

  override def toString():String = {
    val result = new StringBuilder("[") 
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
case class CnOr(conjuncts: List[CnAnd], ta: TypeAnalyzer) extends ConstraintFormula{
  
  override def and(c2: ConstraintFormula, newTa: TypeAnalyzer): ConstraintFormula = c2 match{
    case CnFalse => CnFalse
    case CnTrue => this.reduce
    case CnAnd(u, l, _) => c2.and(this, newTa)
    case CnOr(conjuncts2, _) =>
      val newConjuncts = conjuncts.map((cf: ConstraintFormula) => cf.and(c2, newTa))
      newConjuncts.foldLeft(CnFalse.asInstanceOf[ConstraintFormula])((c1, c2) => c1.or(c2, newTa))
  }

  override def or(c2: ConstraintFormula, newTa: TypeAnalyzer): ConstraintFormula = c2 match{
    case CnFalse => this.reduce
    case CnTrue => CnTrue
    case CnAnd(u, l, _) => c2.or(this, newTa)
    case CnOr(conjuncts2, _) =>
      val newConjuncts = conjuncts.union(conjuncts2)
      newConjuncts.foldLeft(CnFalse.asInstanceOf[ConstraintFormula])((c1, c2) => c1.or(c2, newTa))
  }

  override def solve(bounds: Map[_InferenceVarType,Type]): Option[Map[_InferenceVarType,Type]] = {
    for(conjunct <- conjuncts){
      val solved = conjunct.solve(bounds)
      if(solved.isDefined)
        return solved
    }
    None
  }

  override def implies(c2: ConstraintFormula, newTa: TypeAnalyzer) = conjuncts.forall((c: ConstraintFormula) => c.implies(c2, newTa))
  
  override def isFalse(): Boolean = this.implies(CnFalse, ta)
  
  override def isTrue(): Boolean = CnTrue.implies(this, ta)
  
  override def reduce(): ConstraintFormula = {
    if(this.isTrue)
      return CnTrue
    if(this.isFalse)
      return CnFalse
    if(conjuncts.size == 1)
      return conjuncts.head.reduce
    this
  }
  
  override def toString():String = conjuncts.mkString(" OR ")
}
