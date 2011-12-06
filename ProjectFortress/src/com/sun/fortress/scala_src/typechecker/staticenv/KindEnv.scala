/*******************************************************************************
    Copyright 2009,2011, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.scala_src.typechecker.staticenv

import com.sun.fortress.compiler.Types
import com.sun.fortress.nodes._
import com.sun.fortress.nodes_util.{NodeFactory => NF}
import com.sun.fortress.scala_src.nodes._
import com.sun.fortress.scala_src.useful.Lists._
import com.sun.fortress.scala_src.useful.Options._
import com.sun.fortress.useful.NI

/**
 * Represents a list of variable name to static parameter bindings for some
 * context. All instances of this class should be created with `KindEnv.makeFresh`
 * or with the `extend` method.
 */
abstract sealed class KindEnv extends StaticEnv[StaticParam] {

  /** My type. */
  type Env = KindEnv

  /** My binding type. */
  type EnvBinding = KindBinding

  /** Extend me with the immediate bindings of the given node. */
  def extend(node: Node): KindEnv =
    new NestedKindEnv(this, KindEnv.extractNodeBindings(node))

  /** Extend me with the immediate bindings of the given nodes. */
  def extend[T <: Node](nodes: Iterable[T]): KindEnv =
    new NestedKindEnv(this, nodes.flatMap(KindEnv.extractNodeBindings))

  /** Extend me with the immediate bindings of the given node. */
  def extend(node: Node, where: Option[WhereClause]): KindEnv = {
    val whereBindings = where.map(KindEnv.extractNodeBindings)
                             .getOrElse(Iterable.empty)
    val nodeBindings = KindEnv.extractNodeBindings(node)
    new NestedKindEnv(this, whereBindings ++ nodeBindings)
  }

  /** Extend me with the immediate bindings of the given nodes. */
  def extend[T<:Node](nodes: Iterable[T], where: Option[WhereClause]): KindEnv = {
    val whereBindings = where.map(KindEnv.extractNodeBindings)
                             .getOrElse(Iterable.empty)
    val nodesBindings = nodes.flatMap(KindEnv.extractNodeBindings)
    new NestedKindEnv(this, whereBindings ++ nodesBindings)
  }

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
}

/**
 * A kind environment with a parent and some explicit bindings.
 *
 * @param parent A kind environment that this one extends.
 * @param _bindings A collection of all the bindings in this environment.
 */
class NestedKindEnv (protected val parent: KindEnv,
                               _bindings: Iterable[KindBinding])
    extends KindEnv with NestedStaticEnv[StaticParam] {

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
      // Trivially add this param.
      case p:StaticParam => Iterable(KindBinding(p.getName, p))
      
      // Extract bindings from generic declaration.
      case g:Generic =>
        toListFromImmutable(g.getHeader.getStaticParams)
          .map(p => KindBinding(p.getName, p))
      
      // Extract where clause bindings.
      case SWhereClause(info, bindings, _) => bindings.map {
        
        // If a type binding, create a new TypeParam.
        case b:WhereBinding if b.getKind.isInstanceOf[KindType] =>
          val sp = NF.makeTypeParam(info.getSpan, b.getName, b.getSupers, none[Type], false)
          KindBinding(b.getName, sp)
        
        // Other kinds not supported yet.
        case _ => NI.nyi("non-type where clause bindings")
      }
      
      case _ => Iterable.empty
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
