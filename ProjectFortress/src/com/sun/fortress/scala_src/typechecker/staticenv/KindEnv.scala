/*******************************************************************************
    Copyright 2010 Sun Microsystems, Inc.,
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
import com.sun.fortress.compiler.typechecker.ChildSubtypeCache
import com.sun.fortress.compiler.typechecker.RootSubtypeCache
import com.sun.fortress.compiler.typechecker.SubtypeCache
import com.sun.fortress.nodes._
import com.sun.fortress.scala_src.nodes._
import com.sun.fortress.scala_src.useful.Lists._
// import scala.collection.immutable.EmptyMap

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

  val subtypeCache: SubtypeCache
  def getSubtypeCache(): SubtypeCache = subtypeCache

  /** Extend me with the immediate bindings of the given node. */
  def extend(node: Node): KindEnv =
    new NestedKindEnv(this, KindEnv.extractNodeBindings(node))

  /** Extend me with the immediate bindings of the given nodes. */
  def extend[T <: Node](nodes: Iterable[T]): KindEnv =
    new NestedKindEnv(this, nodes.flatMap(KindEnv.extractNodeBindings))

  /** Extend me with the immediate bindings of the given node. */
  def extend(node: Node, where: Option[WhereClause]): KindEnv =
    new NestedKindEnv(this, KindEnv.extractNodeBindings(node))

  /** Extend me with the immediate bindings of the given nodes. */
  def extend[T<:Node](nodes: Iterable[T], where: Option[WhereClause]): KindEnv =
    new NestedKindEnv(this, nodes.flatMap(KindEnv.extractNodeBindings))

  /**
   * Get the type of the given variable name, if it is bound. The resulting
   * type will only be defined if this name maps to a bool, int, or nat static
   * parameter.
   */
  def getType(x: Name): Option[Type] = lookup(x).flatMap(b =>
    b.sparam.getKind match {
      case _:KindBool => Some(Types.BOOLEAN)
      case _:KindInt => Some(Types.INT_LITERAL)
      case _:KindNat => Some(Types.INT_LITERAL)
      case _ => None
    })

  def staticParam(x: Name): Option[StaticParam] = lookup(x).map(_.sparam)
}

/** The single empty kind environment. */
protected object EmptyKindEnv extends KindEnv with EmptyStaticEnv[StaticParam] {

  val subtypeCache: SubtypeCache = RootSubtypeCache.INSTANCE
}

/**
 * A kind environment with a parent and some explicit bindings.
 *
 * @param parent A kind environment that this one extends.
 * @param _bindings A collection of all the bindings in this environment.
 */
class NestedKindEnv protected (protected val parent: KindEnv,
                               _bindings: Iterable[KindBinding])
    extends KindEnv with NestedStaticEnv[StaticParam] {

  /** Subtype cache used by the type analyzer for this kind env. */
  val subtypeCache: SubtypeCache = new ChildSubtypeCache(parent.subtypeCache)

  /** Internal representation of `bindings` is a map. */
  val bindings: Map[Name, KindBinding] =
    Map(_bindings.map(b => (b.name, b)).toSeq:_*)
}

/** Companion module for KindEnv; contains "static" members. */
object KindEnv extends StaticEnvCompanion[StaticParam] {

  /** My type. */
  type Env = KindEnv

  /** My binding type. */
  type EnvBinding = KindBinding

  /** Creates a fresh kind environment. */
  def makeFresh: KindEnv = EmptyKindEnv.extend(Nil)

  /** Extract out the bindings in node. */
  protected def extractNodeBindings(node: Node) : Iterable[KindBinding] =
    node match {
      case p:StaticParam => List(KindBinding(p.getName, p))
      case g:Generic =>
        toListFromImmutable(g.getHeader.getStaticParams).map(p => KindBinding(p.getName, p))
      case _ => Nil
    }
}

/**
 * A binding for a kind environment contains name to static parameter pairs.
 *
 * @param name The variable name for the binding.
 * @param sparam The static parameter that this name is binding.
 */
case class KindBinding(name: Name,
                       sparam: StaticParam)
