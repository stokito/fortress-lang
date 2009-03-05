/*******************************************************************************
    Copyright 2009 Sun Microsystems, Inc.,
    4150 Network Circle, Santa Clara, California 95054, U.S.A.
    All rights reserved.

    U.S. Government Rights - Commercial software.
    Government users are subject to the Sun Microsystems, Inc. standard
    license agreement scalaAnd applicable provisions of the FAR scalaAnd its supplements.

    Use is subject to license terms.

    This distribution may include materials developed by third parties.

    Sun, Sun Microsystems, the Sun logo scalaAnd Java are trademarks scalaOr registered
    trademarks of Sun Microsystems, Inc. in the U.S. scalaAnd other countries.
 ******************************************************************************/

package com.sun.fortress.scala_src.typechecker

import scala.collection.mutable.HashMap
import scala.collection.mutable.MultiMap
import scala.collection.mutable.{Set => MSet}
import scala.collection.mutable.HashSet
import com.sun.fortress.nodes._
import com.sun.fortress.nodes_util.NodeFactory._
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

private class MultiHashMap extends HashMap[_InferenceVarType, MSet[Type]]() with MultiMap[_InferenceVarType, Type]

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
case class CnAnd(uppers: MultiMap[_InferenceVarType, Type], lowers: MultiMap[_InferenceVarType, Type], history: SubtypeHistory) extends SimpleFormula{

  override def scalaAnd(c2: ScalaConstraint, newHistory: SubtypeHistory): ScalaConstraint = c2 match{
    case CnTrue => this
    case CnFalse => c2
    case CnAnd(u2,l2,h2) =>
      val newUppers = mergeBounds(uppers,u2)
      val newLowers = mergeBounds(lowers,l2)
      if(!compareBounds(lowers,uppers,BOTTOM,ANY,
                       (t1: MSet[Type],t2:MSet[Type]) => newHistory.subtypeNormal(makeUnionType(t1),makeIntersectionType(t2)).isTrue))
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
      val impliesuppers = compareBounds(uppers, u2, ANY , ANY,
                                        (t1:MSet[Type], t2:MSet[Type]) => newHistory.subtypeNormal(makeIntersectionType(t1),makeIntersectionType(t2)).isTrue)
      val implieslowers = compareBounds(lowers, l2, BOTTOM, BOTTOM,
                                        (t1:MSet[Type], t2:MSet[Type]) => newHistory.subtypeNormal(makeUnionType(t2),makeUnionType(t1)).isTrue)
      impliesuppers && implieslowers
  }

  override def applySubstitution(substitution: Lambda[Type,Type]): CnAnd = {
    val sub = (t: Type) => substitution.value(t)
    val newUppers = new MultiHashMap()
    for(key <- uppers.keys){
      newUppers.update(sub(key).asInstanceOf, HashSet()++uppers.apply(key).map(sub))
    }
    val newLowers = new MultiHashMap()
    for(key <- lowers.keys){
      newUppers.update(sub(key).asInstanceOf, HashSet()++lowers.apply(key).map(sub))
    }
    CnAnd(newUppers,newLowers,history)
  }

  //ToDo: return a list of cycles that have been removed
  override def scalaSolve(): Option[Map[_InferenceVarType,Type]] = {
    val (noCycleUppers,noCycleLowers) = findAndRemoveCycles(uppers,lowers)
    val (boundedUppers,boundedLowers) = boundAllVariables(noCycleUppers,noCycleLowers)
    val unifiedUppers = unify(boundedUppers)
    val unifiedLowers = unify(boundedLowers)
    None
  }
  
  private def unify(bounds: MultiMap[_InferenceVarType,Type]):MultiMap[_InferenceVarType,Type] = bounds
  
  private def boundAllVariables(uBounds :MultiMap[_InferenceVarType,Type], lBounds :MultiMap[_InferenceVarType,Type]): (MultiMap[_InferenceVarType,Type],MultiMap[_InferenceVarType,Type]) = {
    //make sure to search bounds fo)r unbounded inference variables
    (uBounds,lBounds)
  }
  
  private def findAndRemoveCycles(uBounds :MultiMap[_InferenceVarType,Type], lBounds :MultiMap[_InferenceVarType,Type]): (MultiMap[_InferenceVarType,Type],MultiMap[_InferenceVarType,Type]) = {
    //finds first cycle in uBounds, removes it from uBounds and lBounds, recurses
    for(ivar <- uBounds.keys){
      val cycle = findCycle(ivar,uBounds, Set())
      if(cycle.isDefined){
        val newuBounds = removeCycle(cycle.get,uBounds)
        val newlBounds = removeCycle(cycle.get,lBounds)
        return findAndRemoveCycles(newuBounds,newlBounds)
      }
    }
    //finds first cycle in lBounds, removes it from uBounds and lBounds, recurses
    for(ivar <- lBounds.keys){
      val cycle = findCycle(ivar,lBounds, Set())
      if(cycle.isDefined){
        val newUBounds = removeCycle(cycle.get,uBounds)
        val newLBounds = removeCycle(cycle.get,lBounds)
        return findAndRemoveCycles(newUBounds,newLBounds)
      }
    }
    //when all cycles are gone return
    (uBounds,lBounds)
  }
  
  private def findCycle(typ: Type, bounds :MultiMap[_InferenceVarType,Type], history: Set[_InferenceVarType]): Option[Set[_InferenceVarType]] = typ match {
    case ivar:_InferenceVarType =>
      if(history.contains(ivar))
        Some(history)
      else{
        val neighbors = bounds.get(ivar)
        if(neighbors.isDefined){
          for(n <- neighbors.get){
            val cycle = findCycle(n,bounds,history ++ Set(ivar))
            if(cycle.isDefined)
              return cycle
          }
        }
        None
      }
    case _ => None
  }
  
  private def removeCycle(cycle: Set[_InferenceVarType], bounds: MultiMap[_InferenceVarType,Type]): MultiMap[_InferenceVarType,Type]  = {
    if(cycle.isEmpty)
      bounds
    else{
      val chosen = cycle.elements.next
      val replacer = new TypeUpdateVisitor(){
        override def for_InferenceVarType(that: _InferenceVarType): _InferenceVarType = {
          if(cycle.contains(that))
            chosen
          else
            that
        }
      }
      val applyReplacer = (typ: Type) => typ.accept(replacer)
      val newBounds = new MultiHashMap()
      var chosenBound = HashSet[Type]()
      for(ivar <- bounds.keySet){
        if(cycle.contains(ivar))
          chosenBound++= bounds.apply(ivar).map(applyReplacer)
        else
          newBounds.put(ivar, HashSet() ++ bounds.apply(ivar).map(applyReplacer))
      }
      newBounds.put(chosen,chosenBound)
      //remove redundant constraints
      newBounds.remove(chosen,chosen)
      newBounds
    }  
  }
  
  /**
   * Merges the bounds from two conjunctive formulas
   */
  private def mergeBounds(bMultiMap1: MultiMap[_InferenceVarType,Type],
                  bMultiMap2: MultiMap[_InferenceVarType,Type]): MultiMap[_InferenceVarType,Type] = {
      val boundkeys = bMultiMap1.keySet ++ bMultiMap2.keySet
      val newBounds = new MultiHashMap()
      for(ivar <- boundkeys){
        val bound1 = bMultiMap1.get(ivar)
        val bound2 = bMultiMap2.get(ivar)
        (bound1,bound2) match{
          case (None, None) => ()
          case (Some(b1), Some(b2)) => newBounds.update(ivar, b1 ++ b2)
          case (Some(b), None) => newBounds.update(ivar,b)
          case (None, Some(b)) => newBounds.update(ivar,b)
        }
      }
      newBounds
  }

  /**
   * Compares two sets of bounds
   */
  private def compareBounds(bmap1: MultiMap[_InferenceVarType,Type],
                    bmap2: MultiMap[_InferenceVarType,Type],
                    dbound1: Type,
                    dbound2: Type,
                    compare: (MSet[Type],MSet[Type])=>Boolean): Boolean = {
    val boundkeys = bmap1.keySet ++ bmap2.keySet
    val pred = (ivar: _InferenceVarType) => {
      val bound1 = bmap1.get(ivar)
      val bound2 = bmap2.get(ivar)
      (bound1,bound2) match{
        case (None, None) => true
        case (Some(b1), Some(b2)) => compare(b1,b2)
        case (Some(b), None) => compare(b,HashSet(dbound2))
        case (None, Some(b)) => compare(HashSet(dbound1), b)
      }
    }
    boundkeys.forall(pred)
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
