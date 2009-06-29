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

import com.sun.fortress.nodes.Name
import com.sun.fortress.nodes.Type

// Get access to the EnvBinding type alias to use in the header.
import StaticEnv.EnvBinding

/**
 * Represents an environment that exists during static checking, mapping
 * variable names to some value. Each static environment contains 
 */
trait StaticEnv[T] extends Collection[EnvBinding[T]] {
  
  /** Define Env as the type of the implementing class. */
  type Env <: StaticEnv[T]
  
  /**
   * Extend this environment with the bindings immediately contained in the
   * given node.
   * 
   * @param node A node or collection of nodes in which to find bindings.
   * @return A new environment with the bindings of this one and those found in
   *         `node` combined.
   */
  def extendWith(node: Any): Env
  
  /**
   * Gets the value stored for the given variable name, if that binding exists.
   *
   * @param name The name to lookup.
   * @return Some(T) if name:T is a binding. None otherwise.
   */
  def lookup(x: Name): Option[T]
  
  /** Same as lookup when treating implementing object as a function. */
  def apply(x: Name): Option[T] = lookup(x)
  
  /**
   * Gets the type stored for the given variable name, if that binding exists.
   * 
   * @param name The name to lookup.
   * @return Some(T) if name:T is a binding. None otherwise.
   */
  def getType(x: Name): Option[Type]  
}

/**
 * A single empty static environment. There are no bindings at all in this
 * environemnt.
 */
trait EmptyStaticEnv[T] extends StaticEnv[T] {
      
  /** Every call to `lookup` fails. */
  override def lookup(x: Name): Option[T] = None
      
  /** Every call to `getType` fails. */
  override def getType(x: Name): Option[Type] = None
  
  // Collection implementation
  override def elements: Iterator[EnvBinding[T]] = Iterator.empty
  override def size: Int = 0
}

/**
 * A nested static environment that contains all the bindings of some parent
 * environment and additionally all the explicitly supplied bindings.
 */
trait NestedStaticEnv[T] extends StaticEnv[T] {
    
  /** A static environment that this one extends. */
  protected val parent: Env
    
  /** The bindings explicitly declared in this environment. */
  protected val bindings: Map[Name, T]
  
  /** Find it among `bindings` or else in `parent`. */
  def lookup(x: Name): Option[T] = bindings.get(x) match {
    case Some(v) => Some(v)
    case None => parent.lookup(x)
  }
  
  // Collection implementation
  def elements: Iterator[EnvBinding[T]] = parent.elements ++ bindings.elements
  def size: Int = parent.size + bindings.size
}

/** Companion module for StaticEnv; contains "static" members. */
object StaticEnv {
  
  /** A pair of a variable name and a value. */
  type EnvBinding[T] = (Name, T)
}

/**
 * Provides functionality for finding bindings in nodes and creating new
 * instances of the environment.
 */
trait StaticEnvCompanion[T] {
  
  /** Define Env as the type of the implementing object's companion class. */
  type Env <: StaticEnv[T]
  
  /**
   * Extracts all the _immediate_ bindings for this kind of environment from the
   * given node. If `node` is a collection of nodes, then this method returns
   * the concatenation of all bindings found therein. Any bindings located
   * further inside the node will not be extracted.
   * 
   * @param node A node or collection of nodes in which to extract bindings.
   * @return A collection of all the bindings extracted in the given node.
   */
  protected def extractEnvBindings(node: Any): Collection[EnvBinding[T]]
  
  /**
   * Creates a new instance of the environment containing all the bindings
   * found in the given node.
   * 
   * @param node A node or collection of nodes in which to find bindings.
   * @return A new instance of Env containing these bindings.
   */
  def make(node: Any): Env
}

