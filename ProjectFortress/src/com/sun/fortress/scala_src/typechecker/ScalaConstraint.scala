/*******************************************************************************
    Copyright 2009 Sun Microsystems, Inc.,
    4150 Network Circle, Santa Clara, California 95054, U.S.A.
    All rights reserved.

    U.S. Government Rights - Commercial software.
    Government users are subject to the Sun Microsystems, Inc. standard
    license agreement scala_and applicable provisions of the FAR scala_and its supplements.

    Use is subject to license terms.

    This distribution may include materials developed by third parties.

    Sun, Sun Microsystems, the Sun logo scala_and Java are trademarks scala_or registered
    trademarks of Sun Microsystems, Inc. in the U.S. scala_and other countries.
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
 * constraints are kept in disjunctive normal form. In scala_order to keep the size of
 * the scala_or method eliminates redundant constraints. Further information can be found
 * in Section 3.2.2 of Dan Smith's paper Java Type Inference is Broken.
 * 
 * Currently it also works as a wrapper to interface with the Java Constraint Formulas
 */

sealed abstract class ScalaConstraint extends ConstraintFormula{
  /**
   * This method scala_ands two constraint formulas
   */
  def scala_and(c2 :ScalaConstraint, newhistory: SubtypeHistory): ScalaConstraint

  /**
   * This method scala_ors two constraint formulas
   */
  def scala_or(c2: ScalaConstraint, newhistory: SubtypeHistory): ScalaConstraint

   /**
   * If the constraints are satisfiable then this returns a substitution of types for
   * inference variables that satisfies the constraints.
   */
  def scala_solve(): Option[Map[_InferenceVarType,Type]]
  
  override def and(c: ConstraintFormula, h: SubtypeHistory):ConstraintFormula = c match{
    case c2:ScalaConstraint => scala_and(c2,h)
    case _ => bug("Can't and a scala formula with a java formula")
  }
  
  override def or(c: ConstraintFormula, h: SubtypeHistory):ConstraintFormula = c match{
    case c2:ScalaConstraint => scala_or(c2,h)
    case _ => bug("Can't or a scala formula with a java formula")
  }
  
  override def solve() = bug("Use scala_solve for scala formulas")
}

/**
 * Formulas with no disjunctions
 */
sealed abstract class SimpleFormula extends ScalaConstraint{
  /**
   * This method is used to determine whether a constraint formula is redundant
   */
  def implies(c2: SimpleFormula, newhistory: SubtypeHistory) : Boolean
}


/**
 * The formula with bounds Any:>$i:>Bottom for all $i
 */
case object CnTrue extends SimpleFormula{
  override def scala_and(c2: ScalaConstraint, newhistory: SubtypeHistory): ScalaConstraint = c2
  override def scala_or(c2: ScalaConstraint, newhistory: SubtypeHistory): ScalaConstraint = this
  override def implies(c2: SimpleFormula, newhistory: SubtypeHistory): Boolean = false
  override def scala_solve(): Option[Map[_InferenceVarType,Type]] = Some(Map.empty)
  override def isTrue():Boolean = true
  override def applySubstitution(substitution: Lambda[Type,Type]): ScalaConstraint = this
}

/**
 * The empty formula
 */
case object CnFalse extends SimpleFormula{
  override def scala_and(c2: ScalaConstraint, newhistory: SubtypeHistory): ScalaConstraint = this
  override def scala_or(c2: ScalaConstraint, newhistory: SubtypeHistory): ScalaConstraint = c2
  override def implies(c2: SimpleFormula, newhistory: SubtypeHistory): Boolean = true
  override def scala_solve(): Option[Map[_InferenceVarType,Type]] = None
  override def isFalse():Boolean = true
  override def applySubstitution(substitution: Lambda[Type,Type]): ScalaConstraint = this
}


/**
 * This class represents the conjunction of primitive formula of the form
 * U>:$i>:B
 */
case class CnAnd(uppers: Map[_InferenceVarType, Type], lowers: Map[_InferenceVarType, Type], history: SubtypeHistory) extends SimpleFormula{

  override def scala_and(c2: ScalaConstraint, newhistory: SubtypeHistory): ScalaConstraint = c2 match{
    case CnTrue => this
    case CnFalse => c2
    case CnAnd(u2,l2,h2) =>
      val newuppers = mergeBounds(uppers,u2,(x:Type, y:Type) => NodeFactory.makeIntersectionType(x,y))
      val newlowers = mergeBounds(lowers,l2,(x:Type, y:Type) => NodeFactory.makeUnionType(x,y))
      if(!compareBounds(lowers,uppers,BOTTOM,ANY,
                       (t1: Type,t2:Type) => newhistory.subtypeNormal(t1,t2).isTrue))
        CnFalse
      else
        CnAnd(newuppers,newlowers,newhistory)
    case CnOr(conjuncts,h2) =>
      val newconjuncts=conjuncts.map((sf: SimpleFormula)=>this.scala_and(sf,newhistory))
      newconjuncts.foldRight(CnFalse.asInstanceOf[ScalaConstraint])((c1: ScalaConstraint, c2: ScalaConstraint) => c1.scala_or(c2,newhistory))
  }

  override def scala_or(c2: ScalaConstraint, newhistory: SubtypeHistory): ScalaConstraint = c2 match{
    case CnTrue => c2
    case CnFalse => this
    case c2@CnAnd(u2,l2,h2) => (this.implies(c2,newhistory),c2.implies(this,newhistory)) match{
      case (false,false) => CnOr(this::List(c2),newhistory)
      case (true,false) => c2
      case (false,true) => this
      case (true,true) => this
    }
    case CnOr(conjuncts,h2) =>
      if(conjuncts.filter((sf: CnAnd)=> this.implies(sf,newhistory)).isEmpty){
        val minimal = conjuncts.filter((sf: CnAnd)=> !sf.implies(this,newhistory))
        if(minimal.isEmpty)
          this
        else
          CnOr(this::minimal,newhistory)
      }else
        c2
  }

  override def implies(c2: SimpleFormula, newhistory: SubtypeHistory): Boolean = c2 match{
    case CnTrue => true
    case CnFalse => false
    case CnAnd(u2,l2,h2) =>
      val impliesuppers = compareBounds(uppers, u2, ANY , ANY,
                                        (t1:Type, t2:Type) => newhistory.subtypeNormal(t1,t2).isTrue)
      val implieslowers = compareBounds(lowers, l2, BOTTOM, BOTTOM,
                                        (t1:Type, t2:Type) => newhistory.subtypeNormal(t2,t1).isTrue)
      impliesuppers && implieslowers
  }

  override def applySubstitution(substitution: Lambda[Type,Type]): CnAnd = {
    val newuppers = new HashMap[_InferenceVarType,Type]
    for(key <- uppers.keys){
      newuppers.update(substitution.value(key).asInstanceOf, substitution.value(uppers.apply(key)))
    }
    val newlowers = new HashMap[_InferenceVarType,Type]
    for(key <- lowers.keys){
      newlowers.update(substitution.value(key).asInstanceOf, substitution.value(lowers.apply(key)))
    }
    CnAnd(newuppers,newlowers,history)
  }

  override def scala_solve(): Option[Map[_InferenceVarType,Type]] = {
    val bounds: Map[_InferenceVarType,Type] = Map.empty
    if(inbounds(lowers,bounds))
      Some(lowers)
    else
      None
  }
  
  /**
   * Merges the bounds from two conjunctive formulas
   */
  private def mergeBounds(bmap1: Map[_InferenceVarType,Type],
                  bmap2: Map[_InferenceVarType,Type],
                  merge:(Type,Type)=>Type): Map[_InferenceVarType,Type] = {
      val boundkeys = bmap1.keySet ++ bmap2.keySet
      val newbounds = new HashMap[_InferenceVarType, Type]
      for(ivar <- boundkeys){
        val bound1 = bmap1.get(ivar)
        val bound2 = bmap2.get(ivar)
        (bound1,bound2) match{
          case (None, None) => ()
          case (Some(b1), Some(b2)) => newbounds.update(ivar, merge(b1,b2))
          case (Some(b), None) => newbounds.update(ivar,b)
          case (None, Some(b)) => newbounds.update(ivar,b)
        }
      }
      newbounds
  }

  /**
   * Compares two sets of bounds
   */
  private def compareBounds(bmap1: Map[_InferenceVarType,Type],
                    bmap2: Map[_InferenceVarType,Type],
                    dbound1: Type,
                    dbound2: Type,
                    compare: (Type,Type)=>Boolean): Boolean = {
    val boundkeys = bmap1.keySet ++ bmap2.keySet
    var accum = true
    for(ivar <- boundkeys){
      val bound1 = bmap1.get(ivar)
      val bound2 = bmap2.get(ivar)
      (bound1,bound2) match{
        case (None, None) => ()
        case (Some(b1), Some(b2)) => accum &= compare(b1,b2)
        case (Some(b), None) => accum &= compare(b,dbound2)
        case (None, Some(b)) => accum &= compare(dbound1, b)
      }
    }
    accum
  }

  /**
   * Determines whether a given substitution of types for
   * inference variables is valid
   */
  private def inbounds(substitutions: Map[_InferenceVarType,Type],bounds: Map[_InferenceVarType,Type]): Boolean = {
    var accum = true
    for((ivar,bound) <- bounds){
      val replacer=new InferenceVarReplacer(Maps.toJavaMap(substitutions))
      val newbound=bound.asInstanceOf[Node].accept(replacer).asInstanceOf[Type]
      substitutions.get(ivar) match {
        case None => accum&=history.subtypeNormal(ANY,newbound).isTrue;
        case Some(substitution) => accum&=history.subtypeNormal(substitution,newbound).isTrue;
      }
    }
    accum
  }

}


/**
 * A disjunction of simple formulas
 */
case class CnOr( conjuncts: List[CnAnd], history: SubtypeHistory) extends ScalaConstraint{
  override def scala_and(c2: ScalaConstraint, newhistory: SubtypeHistory): ScalaConstraint = c2 match{
    case CnFalse => c2
    case CnTrue => this
    case CnAnd(u2,l2,h2) => c2.scala_and(this,newhistory)
    case c2@CnOr(conjuncts,h2) =>
      val newconjuncts = conjuncts.map((cf: ScalaConstraint) => cf.scala_and(c2,newhistory))
      newconjuncts.foldRight(CnFalse.asInstanceOf[ScalaConstraint])((c1: ScalaConstraint, c2: ScalaConstraint) => c1.scala_or(c2,newhistory))
  }

  override def scala_or(c2: ScalaConstraint, newhistory: SubtypeHistory): ScalaConstraint = c2 match{
    case CnFalse => this
    case CnTrue => c2
    case CnAnd(u2,l2,h2) => c2.scala_or(this,newhistory)
    case CnOr(conjuncts2,h2) =>
      val newconjuncts = conjuncts ++ conjuncts2
      newconjuncts.foldRight(CnFalse.asInstanceOf[ScalaConstraint])((c1: ScalaConstraint, c2: ScalaConstraint) => c1.scala_or(c2,newhistory))
  }

  override def scala_solve(): Option[Map[_InferenceVarType,Type]] = {
    var answer: Option[Map[_InferenceVarType,Type]] = None
    for(conjunct <- conjuncts){
      if(answer.isEmpty){
        val solved = conjunct.scala_solve
        if(solved.isDefined)
          answer = solved
      }
    }
    answer
  }

  override def applySubstitution(substitution: Lambda[Type,Type]): ScalaConstraint = 
    CnOr(conjuncts.map((c:CnAnd) => c.applySubstitution(substitution)),history)

}
