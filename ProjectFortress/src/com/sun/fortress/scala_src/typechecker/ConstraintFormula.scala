/*******************************************************************************
    Copyright 2008 Sun Microsystems, Inc.,
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
import scala.collection.immutable.HashMap 
import com.sun.fortress.nodes_util.NodeFactory
import com.sun.fortress.compiler.Types.ANY
import com.sun.fortress.compiler.Types.BOTTOM
import com.sun.fortress.compiler.typechecker.TypeAnalyzer

/**
 * This class represents the constraints accummulated on inference variables. All
 * constraints are kept in disjunctive normal form. In order to keep the size of
 * the or method eliminates redundant constraints. Further information can be found
 * in Section 3.2.2 of Dan Smith's paper Java Type Inference is Broken 
 */

sealed abstract class ConstraintFormula{
  /**
   * This method ands two constraint formulas
   */
  def and(c2 :ConstraintFormula, analyzer: TypeAnalyzer): ConstraintFormula
  
  /**
   * This method ors two constraint formulas
   */
  def or(c2: ConstraintFormula, analyzer: TypeAnalyzer): ConstraintFormula
}

/**
 * Formulas with no disjunctions
 */
sealed abstract class SimpleFormula extends ConstraintFormula{
  /**
   * This method is used to determine whether a constraint formula is redundant 
   */
  def implies(c2: SimpleFormula, analyzer: TypeAnalyzer) : Boolean
}

/**
 * Helper methods for ConstraintFormula
 */
object ConstraintFormula{
  /**
   * Merges the bounds from two conjunctive formulas
   */
  def mergeBounds(bmap1: Map[_InferenceVarType,Type], 
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
   * Compares the bounds from two conjunctive formulas
   */
  def compareBounds(bmap1: Map[_InferenceVarType,Type], 
                    bmap2: Map[_InferenceVarType,Type],
                    bound: Type,
                    compare: (Type,Type)=>Boolean): Boolean = {
    val boundkeys = bmap1.keySet ++ bmap2.keySet
    var accum = true
    for(ivar <- boundkeys){
      val bound1 = bmap1.get(ivar)
      val bound2 = bmap2.get(ivar)
      (bound1,bound2) match{
        case (None, None) => ()
        case (Some(b1), Some(b2)) => accum &= compare(b1,b2)
        case (Some(b), None) => accum &= compare(b,bound)
        case (None, Some(b)) => accum &= compare(bound, b) 
      }
    }
    accum
  }
}

/**
 * The formula with bounds Any:>T:>Bottom for all T
 */
case object CnTrue extends SimpleFormula{
  override def and(c2: ConstraintFormula, analyzer: TypeAnalyzer): ConstraintFormula = c2
  override def or(c2: ConstraintFormula, analyzer: TypeAnalyzer): ConstraintFormula = this
  override def implies(c2: SimpleFormula, analyze: TypeAnalyzer): Boolean = false
}

/**
 * The empty formula
 */
case object CnFalse extends SimpleFormula{
  override def and(c2: ConstraintFormula, analyzer: TypeAnalyzer): ConstraintFormula = this
  override def or(c2: ConstraintFormula, analyzer: TypeAnalyzer): ConstraintFormula = c2
  override def implies(c2: SimpleFormula, analyze: TypeAnalyzer): Boolean = true
}


/**
 * This class represents the conjunction of primitive formula of the form
 * U>:T>:B
 */
case class CnAnd(uppers: Map[_InferenceVarType, Type], lowers: Map[_InferenceVarType, Type]) extends SimpleFormula{
  
  override def and(c2: ConstraintFormula, analyzer: TypeAnalyzer): ConstraintFormula = c2 match{
    case CnTrue => this
    case CnFalse => c2
    case CnAnd(u2,l2) =>
      val newuppers = ConstraintFormula.mergeBounds(uppers,u2,(x:Type, y:Type) => NodeFactory.makeIntersectionType(x,y))
      val newlowers = ConstraintFormula.mergeBounds(lowers,l2,(x:Type, y:Type) => NodeFactory.makeUnionType(x,y))
      new CnAnd(newuppers,newlowers)
    case CnOr(conjuncts) =>
      val newconjuncts=conjuncts.map((sf: SimpleFormula)=>this.and(sf,analyzer))
      newconjuncts.foldRight(CnFalse.asInstanceOf[ConstraintFormula])((c1: ConstraintFormula, c2: ConstraintFormula) => c1.or(c2,analyzer))
  }
  

  
  override def or(c2: ConstraintFormula, analyzer: TypeAnalyzer): ConstraintFormula = c2 match{
    case CnTrue => c2
    case CnFalse => this
    case c2@CnAnd(u2,l2) => (this.implies(c2,analyzer),c2.implies(this,analyzer)) match{
      case (false,false) => CnOr(this::List(c2))
      case (true,false) => c2
      case (false,true) => this
      case (true,true) => this
    }
    case CnOr(conjuncts) =>
      if(conjuncts.filter((sf: CnAnd)=> this.implies(sf,analyzer)).isEmpty){
        val minimal = conjuncts.filter((sf: CnAnd)=> !sf.implies(this,analyzer))
        if(minimal.isEmpty)
          this
        else
          CnOr(this::minimal)
      }else
        c2
  }
  
  override def implies(c2: SimpleFormula, analyzer: TypeAnalyzer): Boolean = c2 match{
    case CnTrue => true
    case CnFalse => false
    case CnAnd(u2,l2) => 
      val impliesuppers = ConstraintFormula.compareBounds(uppers,u2,ANY,(t1:Type, t2:Type) => analyzer.subtype(t1,t2).isTrue)
      val implieslowers = ConstraintFormula.compareBounds(lowers,l2,BOTTOM,(t1:Type, t2:Type) => analyzer.subtype(t2,t1).isTrue)
      impliesuppers && implieslowers
  }
  
 
}


/**
 * A disjunction of simple formulas
 */
case class CnOr( conjuncts: List[CnAnd]) extends ConstraintFormula{
  override def and(c2: ConstraintFormula, analyzer: TypeAnalyzer): ConstraintFormula = c2 match{
    case CnFalse => c2
    case CnTrue => this
    case CnAnd(u2,l2) => c2.and(this,analyzer)
    case c2@CnOr(conjuncts2) =>
      val newconjuncts = conjuncts.map((cf: ConstraintFormula) => cf.and(c2,analyzer))
      newconjuncts.foldRight(CnFalse.asInstanceOf[ConstraintFormula])((c1: ConstraintFormula, c2: ConstraintFormula) => c1.or(c2,analyzer))
  }
  
  override def or(c2: ConstraintFormula, analyzer: TypeAnalyzer): ConstraintFormula = c2 match{
    case CnFalse => this
    case CnTrue => c2
    case CnAnd(u2,l2) => c2.or(this,analyzer)
    case CnOr(conjuncts2) =>
      val newconjuncts = conjuncts ++ conjuncts2
      newconjuncts.foldRight(CnFalse.asInstanceOf[ConstraintFormula])((c1: ConstraintFormula, c2: ConstraintFormula) => c1.or(c2,analyzer))
  }
  
}