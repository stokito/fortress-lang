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

import com.sun.fortress.compiler.Types
import com.sun.fortress.nodes._
import scala.collection.immutable.EmptyMap

import StaticEnv.EnvBinding

/**
 * Represents a list of variable name to static parameter bindings for some
 * context. All instances of this class should be created with `KindEnv.make`
 * or with the `extendWith` method.
 */
abstract sealed class KindEnv extends StaticEnv[StaticParam] {
  
  /** My type. */
  type Env = KindEnv

  /** Extend me with the immediate bindings of the given node. */
  def extendWith(node: Any): KindEnv =
    new NestedKindEnv(this, KindEnv.extractEnvBindings(node))

  /**
   * Get the type of the given variable name, if it is bound. The resulting
   * type will only be defined if this name maps to a bool, int, or nat static
   * parameter.
   */
  def getType(x: Name): Option[Type] = lookup(x).flatMap(p =>
    p.getKind match {
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
                               _bindings: Collection[EnvBinding[StaticParam]])
    extends KindEnv with NestedStaticEnv[StaticParam] {
    
  /** Internal representation of `bindings` is a map. */
  protected val bindings: Map[Name, StaticParam] = new EmptyMap ++ _bindings
}

/** Companion module for KindEnv; contains "static" members. */
object KindEnv extends StaticEnvCompanion[StaticParam] {
  
  /** My type. */
  type Env = KindEnv
  
  /** New kind environment with empty parent and the node's bindings. */
  def make(node: Any): KindEnv =
    new NestedKindEnv(EmptyKindEnv, extractEnvBindings(node))
  
  /** Extract out the bindings in node. */
  protected def extractEnvBindings(node: Any)
                : Collection[EnvBinding[StaticParam]] = null
}
