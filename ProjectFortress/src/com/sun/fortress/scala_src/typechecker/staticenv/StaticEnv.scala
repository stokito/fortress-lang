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

import _root_.java.util.{Map => JMap}
import com.sun.fortress.nodes._
import com.sun.fortress.nodes_util.{NodeFactory => NF}
import com.sun.fortress.nodes_util.{NodeUtil => NU}
import com.sun.fortress.scala_src.nodes._
import com.sun.fortress.scala_src.useful.Options._
import edu.rice.cs.plt.collect.Relation

/**
 * Represents an environment that exists during static checking, mapping
 * variable names to some value. Each static environment contains 
 */
trait StaticEnv[T] {
  
  /** Define Env as the type of the implementing class. */
  type Env <: StaticEnv[T]
  
  /** Define EnvBinding as the type of the bindings. */
  type EnvBinding
  
  /**
   * Extend this environment with the bindings immediately contained in the
   * given node.
   * 
   * @param node A node in which to find bindings.
   * @return A new environment with the bindings of this one and those found in
   *         `node` combined.
   */
  def extend(node: Node): Env
  
  /**
   * Extend this environment with the bindings immediately contained in the
   * given collection of nodes.
   * 
   * @param node A collection of nodes in which to find bindings.
   * @return A new environment with the bindings of this one and those found in
   *         `nodes` combined.
   */
  def extend[T <: Node](nodes: Iterable[T]): Env
  
  /**
   * Gets the value stored for the given variable name, if that binding exists.
   *
   * @param name The name to lookup.
   * @return Some(T) if name:T is a binding. None otherwise.
   */
  def lookup(x: Name): Option[EnvBinding]
  
  /** Same as lookup when treating implementing object as a function. */
  def apply(x: Name): Option[EnvBinding] = lookup(x)
  
  /** Does the environment contain a binding for the given name? */
  def isDefinedAt(x: Name): Boolean = lookup(x).isDefined
  
  /**
   * Gets the type stored for the given variable name, if that binding exists.
   * 
   * @param name The name to lookup.
   * @return Some(T) if name:T is a binding. None otherwise.
   */
  def getType(x: Name): Option[Type]
  
  /** Strip the API out of the given name. */
  protected def stripApi(x: Name): Name = x match {
    case SId(info, Some(api), text) => SId(info, None, text)
    case SOp(info, Some(api), text, fix, enc) =>
      SOp(info, None, text, fix, enc)
    case x:AnonymousFnName => NF.makeAnonymousFnName(NU.getSpan(x), none[APIName])
    case x:ConstructorFnName =>
      NF.makeConstructorFnName(NU.getSpan(x), none[APIName], x.getConstructor)
    case _ => x
  }
}

/**
 * A single empty static environment. There are no bindings at all in this
 * environemnt.
 */
trait EmptyStaticEnv[T] extends StaticEnv[T] {
      
  /** Every call to `lookup` fails. */
  override def lookup(x: Name): Option[EnvBinding] = None
      
  /** Every call to `getType` fails. */
  override def getType(x: Name): Option[Type] = None
}

/**
 * A nested static environment that contains all the bindings of some parent
 * environment and additionally all the explicitly supplied bindings.
 */
trait NestedStaticEnv[T] extends StaticEnv[T] {
    
  /** A static environment that this one extends. */
  protected val parent: Env {
    // Require that the parent has the same binding type.
    type EnvBinding = NestedStaticEnv.this.EnvBinding
  }
    
  /** The bindings explicitly declared in this environment. */
  protected val bindings: Map[Name, EnvBinding]
  
  /** Find it among `bindings` or else in `parent`. */
  def lookup(x: Name): Option[EnvBinding] = bindings.get(stripApi(x)) match {
    case Some(v) => Some(v)
    case None => parent.lookup(x)
  }
}

/**
 * Provides functionality for finding bindings in nodes and creating new
 * instances of the environment.
 */
trait StaticEnvCompanion[T] {
  
  /** Define Env as the type of the implementing object's companion class. */
  type Env <: StaticEnv[T]
  
  /** Define EnvBinding as the type of the bindings. */
  type EnvBinding
  
  /**
   * Extracts all the _immediate_ bindings for this kind of environment from the
   * given node. Any bindings located further inside the node will not be
   * extracted.
   * 
   * @param node A node in which to extract bindings.
   * @return A collection of all the bindings extracted in the given node.
   */
  protected def extractNodeBindings(node: Node): Iterable[EnvBinding]
}
