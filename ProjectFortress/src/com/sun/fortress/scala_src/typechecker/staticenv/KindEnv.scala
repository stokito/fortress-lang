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

package com.sun.fortress.scala_src.typechecker.staticenv

import com.sun.fortress.compiler.Types
import com.sun.fortress.nodes._
import scala.collection.immutable.EmptyMap

/**
 * Represents a list of variable name to static parameter bindings for some
 * context. All instances of this class should be created with `KindEnv.make`
 * or with the `extendWith` method.
 */
abstract sealed class KindEnv extends StaticEnv[StaticParam] {
  
  /** My type. */
  type Env = KindEnv
  
  /** My binding type. */
  type EnvBinding = KindBinding

  /** Extend me with the immediate bindings of the given node. */
  def extendWith(node: Any): KindEnv =
    new NestedKindEnv(this, KindEnv.extractEnvBindings(node))

  /**
   * Get the type of the given variable name, if it is bound. The resulting
   * type will only be defined if this name maps to a bool, int, or nat static
   * parameter.
   */
  def getType(x: Name): Option[Type] = lookup(x).flatMap(b =>
    b.value.getKind match {
      case _:KindBool => Some(Types.BOOLEAN)
      case _:KindInt => Some(Types.INT_LITERAL)
      case _:KindNat => Some(Types.INT_LITERAL)
      case _ => None
    })
}

/** The single empty kind environment. */
object EmptyKindEnv extends KindEnv with EmptyStaticEnv[StaticParam]

/**
 * A kind environment with a parent and some explicit bindings.
 * 
 * @param parent A kind environment that this one extends.
 * @param _bindings A collection of all the bindings in this environment.
 */
class NestedKindEnv protected (protected val parent: KindEnv,
                               _bindings: Collection[KindBinding])
    extends KindEnv with NestedStaticEnv[StaticParam] {
    
  /** Internal representation of `bindings` is a map. */
  protected val bindings: Map[Name, KindBinding] =
    new EmptyMap ++ _bindings.map(b => b.name -> b)
}

/** Companion module for KindEnv; contains "static" members. */
object KindEnv extends StaticEnvCompanion[StaticParam] {
  
  /** My type. */
  type Env = KindEnv
  
  /** My binding type. */
  type EnvBinding = KindBinding
  
  /** New kind environment with empty parent and the node's bindings. */
  def make(node: Any): KindEnv =
    new NestedKindEnv(EmptyKindEnv, extractEnvBindings(node))
  
  /** Extract out the bindings in node. */
  protected def extractEnvBindings(node: Any) : Collection[KindBinding] = null
}

/**
 * A binding for a kind environment contains name to static parameter pairs.
 * 
 * @param name The variable name for the binding.
 * @param sparam The static parameter that this name is binding.
 */
case class KindBinding(override val name: Name,
                       sparam: StaticParam)
    extends StaticBinding[StaticParam](name, sparam)
