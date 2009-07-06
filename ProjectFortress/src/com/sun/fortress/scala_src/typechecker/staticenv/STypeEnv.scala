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

import com.sun.fortress.nodes._
import com.sun.fortress.nodes_util.Modifiers
import scala.collection.immutable.EmptyMap

/**
 * Represents a list of variable name to type bindings for some context. All
 * instances of this class should be created with `STypeEnv.make` or with the
 * `extendWith` method.
 */
abstract sealed class STypeEnv extends StaticEnv[Type] {
  
  /** My type. */
  type Env = STypeEnv
  
  /** My binding type. */
  type EnvBinding = TypeBinding
  
  /** Extend me with the immediate bindings of the given node. */
  
  /** Same as `lookup`. */
  def getType(x: Name): Option[Type] = lookup(x).map(_.value)
}

/** The single empty type environment. */
object EmptySTypeEnv extends STypeEnv with EmptyStaticEnv[Type]

/**
 * A type environment with a parent and some explicit bindings.
 * 
 * @param parent A type environment that this one extends.
 * @param _bindings A collection of all the bindings in this environment.
 */
class NestedSTypeEnv protected (protected val parent: STypeEnv,
                                _bindings: Iterable[TypeBinding])
    extends STypeEnv with NestedStaticEnv[Type] {
    
  /** Internal representation of `bindings` is a map. */
  protected val bindings: Map[Name, TypeBinding] =
    new EmptyMap ++ _bindings.map(b => b.name -> b)
}

/** Companion module for STypeEnv. */
object STypeEnv extends StaticEnvCompanion[Type]
    with STypeEnvExtraction {
  
  /** My type. */
  type Env = STypeEnv
  
  /** My binding type. */
  type EnvBinding = TypeBinding
  
  def empty(): STypeEnv = EmptySTypeEnv 
  
}


/**
 * A binding for a type environment contains name to type pairs, along with some
 * modifiers for the binding and whether or not the binding is mutable.
 * 
 * @param name The variable name for the binding.
 * @param value The bound type for this variable name.
 * @param mods Any modifiers for the binding.
 * @param mutable Whether or not the binding is mutable.
 */
case class TypeBinding(override val name: Name,
                       typ: Type,
                       mods: Modifiers,
                       mutable: Boolean) extends StaticBinding[Type](name, typ)
