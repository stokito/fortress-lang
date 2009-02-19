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
import scala.collection.jcl.{HashMap => JavaHashMap}
import scala.collection.jcl.MapWrapper
import scala.collection.jcl.Conversions
import com.sun.fortress.nodes._
import com.sun.fortress.nodes_util.NodeFactory
import com.sun.fortress.compiler.Types.ANY
import com.sun.fortress.compiler.Types.BOTTOM
import com.sun.fortress.compiler.typechecker.SubtypeHistory
import com.sun.fortress.compiler.typechecker.InferenceVarReplacer
import com.sun.fortress.compiler.typechecker.SubtypeHistory
import edu.rice.cs.plt.lambda.Lambda
import com.sun.fortress.compiler.typechecker.constraints.ConstraintFormula;

/*
 * Helper methods for ConstraintFormula
 */
object ScalaConstraintUtil{
  /*
   * Converts a boolean into a Constraint Formula
   */
  def fromBoolean(bool: Boolean): ConstraintFormula = bool match {
    case true => CnTrue
    case false => CnFalse
  }

  /*
   * Gives a constraint with ivar<:ubound
   */
  def upperBound(ivar: _InferenceVarType, ubound: Type, history: SubtypeHistory): ConstraintFormula = history.subtypeNormal(ANY,ubound).isTrue match{
    case true => CnTrue
    case false =>
      val empty: Map[_InferenceVarType,Type] = HashMap.empty;
      val ubounds = empty.update(ivar,ubound)
      CnAnd(ubounds,empty)
  }

  /*
   * Gives a constraint with lbound<:ivar
   */
  def lowerBound(ivar: _InferenceVarType, lbound: Type, history: SubtypeHistory): ConstraintFormula = history.subtypeNormal(lbound,BOTTOM).isTrue match{
    case true => CnTrue
    case false =>
      val empty: Map[_InferenceVarType,Type] = HashMap.empty;
      val lbounds = empty.update(ivar,lbound)
      CnAnd(empty,lbounds)
  }
  def trueFormula(): ConstraintFormula = CnTrue

  def falseFormula(): ConstraintFormula = CnFalse

}
