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
import _root_.java.util.{Set => JSet}
import com.sun.fortress.compiler.index._
import com.sun.fortress.nodes._
import com.sun.fortress.nodes_util.Modifiers
import com.sun.fortress.nodes_util.{NodeFactory => NF}
import com.sun.fortress.nodes_util.{NodeUtil => NU}
import com.sun.fortress.scala_src.nodes._
import com.sun.fortress.scala_src.useful.Lists._
import com.sun.fortress.scala_src.useful.Options._
import com.sun.fortress.scala_src.useful.Maps._
import com.sun.fortress.scala_src.useful.Sets._
import com.sun.fortress.scala_src.useful.STypesUtil
import edu.rice.cs.plt.collect.Relation
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
  def extend(node: Node): STypeEnv =
    new NestedSTypeEnv(this, STypeEnv.extractNodeBindings(node))
  
  /** Extend me with the immediate bindings of the given nodes. */
  def extend[T <: Node](nodes: Iterable[T]): STypeEnv =
    new NestedSTypeEnv(this, nodes.flatMap(STypeEnv.extractNodeBindings))
  
  /** Extend me with the bindings of the given variables relation. */
  def extendWithVariables[T <: Variable](m: JMap[Id, T]): STypeEnv =
    new NestedSTypeEnv(this, STypeEnv.extractVariableBindings(m))
  
  /** Extend me with the bindings of the given typeconses relation. */
  def extendWithTypeConses[T <: TypeConsIndex](m: JMap[Id, T]): STypeEnv =
    new NestedSTypeEnv(this, STypeEnv.extractTypeConsBindings(m))
  
  /** Extend me with the bindings of the given functions relation. */
  def extendWithFunctions[T <: Functional]
      (r: Relation[IdOrOpOrAnonymousName, T]): STypeEnv =
    new NestedSTypeEnv(this, STypeEnv.extractFunctionBindings(r))
  
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
object STypeEnv extends StaticEnvCompanion[Type] {
  
  /** My type. */
  type Env = STypeEnv
  
  /** My binding type. */
  type EnvBinding = TypeBinding
  
  /** Gives Java access to the empty environment. */
  def EMPTY: STypeEnv = EmptySTypeEnv
  
  /**
   * Creates a new instance of the environment containing all the bindings
   * found in the given compilation unit.
   * 
   * @param comp A compilation unit index for the program.
   * @return A new instance of Env containing these bindings.
   */
  def make(comp: CompilationUnitIndex): STypeEnv =
    EmptySTypeEnv.extendWithFunctions(comp.functions)
                 .extendWithVariables(comp.variables)
                 .extendWithTypeConses(comp.typeConses)
  
  /** Extract out the bindings in node. */
  protected def extractNodeBindings(node: Node): Iterable[TypeBinding] = {
    (node match{
      case SBinding(_, name, mods, Some(typ)) =>
        TypeBinding(name, typ, mods, false)
      
      case SParam(_, name, mods, _, _, Some(vaTyp)) =>
        TypeBinding(name, vaTyp, mods, false)
      case SParam(_, name, mods, Some(typ), _, _) =>
        TypeBinding(name, typ, mods, false)
      
      case SLValue(_, name, mods, Some(typ), mutable) =>
        TypeBinding(name, typ, mods, mutable)
        
      case SLocalVarDecl(_, _, lValues, _) => lValues.map(extractNodeBindings)

      case _ => Nil
    }) match{
      case bs:Iterable[TypeBinding] => bs
      case b:TypeBinding => List(b)
    }
  }
  
  protected def extractVariableBindings[T <: Variable](m: JMap[Id, T]): Iterable[TypeBinding] = toMap(m).flatMap(xv =>{ 
    xv match{
      case (x:Id, v:DeclaredVariable) => extractNodeBindings(v.ast)
      case (x:Id, v:SingletonVariable) =>
        extractNodeBindings(NF.makeLValue(x, v.declaringTrait))
      case (x:Id, v:ParamVariable) =>  extractNodeBindings(v.ast)
    }
  })
  
  protected def extractTypeConsBindings[T <: TypeConsIndex](m: JMap[Id, T]): Iterable[TypeBinding] = toMap(m).flatMap(xv => {
    xv match {
      // Bind object names to their types.
      case (x:Id, v:ObjectTraitIndex) =>
        val decl = v.ast
        val params = toOption(NU.getParams(decl))
        val sparams = NU.getStaticParams(decl)
        val objType = params match {
          case None if sparams.isEmpty =>
            // Just a single trait type.
            NF.makeTraitType(x)
          case None =>
            // A generic trait type.
            NF.makeGenericSingletonType(x, sparams)
          case Some(params) if sparams.isEmpty =>
            // Arrow type for basic constructor.
            NF.makeArrowType(NU.getSpan(x),
                             STypesUtil.makeDomainType(toList(params)),
                             NF.makeTraitType(x))
          case Some(params) =>
            // Generic arrow type for constructor.
            val sargs = toList(sparams).map(STypesUtil.staticParamToArg)
            NF.makeArrowType(NU.getSpan(decl),
                             false,
                             STypesUtil.makeDomainType(toList(params)),
                             NF.makeTraitType(x, toJavaList(sargs)),
                             NF.emptyEffect, // TODO: Change this?
                             sparams,
                             NU.getWhereClause(decl))
        }
        List(TypeBinding(x, objType, Modifiers.None, false))
    }
  })
  
  protected def extractFunctionBindings[T <: Functional](r: Relation[IdOrOpOrAnonymousName, T]): Iterable[TypeBinding] = {
    val fnNames = toSet(r.firstSet)     
    // For each name, intersect together all of its overloading types.
    fnNames.flatMap(x => {
        val fns: Set[Functional] = toSet(r.matchFirst(x).asInstanceOf[JSet[Functional]])
        val oTypes =
          fns.map(STypesUtil.makeArrowFromFunctional(_).asInstanceOf[Type])
        val fnType = NF.makeIntersectionType(oTypes)
        Some(TypeBinding(x, fnType, Modifiers.None, false))
    })
  }
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
