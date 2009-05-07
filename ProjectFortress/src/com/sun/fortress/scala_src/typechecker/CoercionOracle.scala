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

import com.sun.fortress.exceptions.CompilerError
import com.sun.fortress.exceptions.InterpreterBug
import com.sun.fortress.nodes._
import com.sun.fortress.scala_src.nodes._
import com.sun.fortress.scala_src.useful.Converter._
import com.sun.fortress.compiler.index.CompilationUnitIndex
import com.sun.fortress.compiler.index.TraitIndex
import com.sun.fortress.compiler.typechecker.TypeAnalyzer
import com.sun.fortress.compiler.typechecker.TraitTable
import com.sun.fortress.compiler.typechecker.TypeEnv
import com.sun.fortress.scala_src.useful.ASTGenHelper._

import scala.collection.mutable.HashMap
import scala.collection.mutable.Map
import scala.collection.mutable.Set

class CoercionOracleFactory(traits: TraitTable, analyzer: TypeAnalyzer, errors: ErrorLog) {
  val coercionTable = makeCoercionTable(traits)
  val exclusionOracle = new ExclusionOracle(analyzer)

  private def makeCoercionTable(traits: TraitTable) = {
    /*
     * Build a hashtable mapping types coerced *from* to the types they coerce *to*.
     */
    val result = HashMap[Type, Set[Type]]()

    for (to <- traits) {
      to match {
        case ti: TraitIndex =>
          for (c <- ti.coercions) {
            // TODO Add check that there is no explicit return type declared for this coercion.

            val params = c.parameters
            if (params.size != 1) { 
              errors.signal("A coercion declaration must have exactly one parameter", c.getSpan) 
            }
            else {
              val param = params.get(0)
              val typ = param.getIdType

              scalaify(typ) match {
                case None => errors.signal("A coercion declaration must explicitly declare its parameter type.", param) 
                case Some(from:Type) => {
                  val knownCoercions = result.getOrElseUpdate(from, Set[Type]())

                  scalaify(ti.typeOfSelf) match {
                    case None => errors.signal("The CoercionOracle cannot yet handle TraitObjectDecls without self types.", ti.ast)
                    case Some(tu:Type) => {
                      knownCoercions += tu
                    }
                  }
                }
              }
            }
          }
        // TODO Handle coercions in other indices (what else might we get?)
        case _ => ()
      }
    }
    result
  }
  
  def makeOracle(env: TypeEnv):CoercionOracle = {
    new CoercionOracle(env, traits, coercionTable, exclusionOracle)
  }
}

class CoercionOracle(env: TypeEnv, traits: TraitTable, coercions:Map[Type,Set[Type]], exclusions: ExclusionOracle) {
  def mostSpecific(cs: Set[Type]): Option[Type] = {
    if (cs.isEmpty) {
      throw new InterpreterBug("Attempt to find the most specific type in an empty set")
    }
    else {
      var result: Option[Type] = None
      for (c <- cs) {
        if (result == None || moreSpecific(c, result.get)) result = Some(c)
      }
      result
    }
  }

  private def moreSpecific(t1: Type, t2: Type) = {
    val analyzer = new TypeAnalyzer(traits, env)
    analyzer.subtype(t1,t2).isTrue ||
      (exclusions.excludes(t1,t2) && coercesTo(t1,t2) && rejects(t1,t2))
  }

  private def rejects(t1: Type, t2: Type) = {
    var result = false 
    for (u <- coercionsTo(t1)) {
      result = result || exclusions.excludes(u,t2)
    }
    result
  }


  def coercionsTo(t: Type): Set[Type] = coercions.getOrElseUpdate(t, Set[Type]())
  def coercesTo(t: Type, u: Type): Boolean = coercionsTo(t).contains(u)
}
