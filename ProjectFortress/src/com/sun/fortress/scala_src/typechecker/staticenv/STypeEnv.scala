/*******************************************************************************
    Copyright 2009,2011, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.scala_src.typechecker.staticenv

import _root_.java.util.ArrayList
import _root_.java.util.{List => JList}
import _root_.java.util.{Map => JMap}
import _root_.edu.rice.cs.plt.tuple.{Option => JOption}
import _root_.java.util.{Set => JSet}
// import collection.jcl.Hashtable
import com.sun.fortress.compiler.index._
import com.sun.fortress.exceptions.InterpreterBug.bug
import com.sun.fortress.exceptions.TypeError
import com.sun.fortress.nodes._
import com.sun.fortress.nodes_util.Modifiers
import com.sun.fortress.nodes_util.{NodeFactory => NF}
import com.sun.fortress.nodes_util.{NodeUtil => NU}
import com.sun.fortress.nodes_util.UIDObject
import com.sun.fortress.scala_src.nodes._
import com.sun.fortress.scala_src.useful.Lists._
import com.sun.fortress.scala_src.useful.Maps._
import com.sun.fortress.scala_src.useful.Options._
import com.sun.fortress.scala_src.useful.Sets._
import com.sun.fortress.scala_src.useful.SNodeUtil._
import com.sun.fortress.scala_src.useful.STypesUtil._
import edu.rice.cs.plt.collect.Relation

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
  def extendWithVariables[T <: Variable](m: JMap[Id, T],
                                         api: Option[APIName]): STypeEnv =
    new NestedSTypeEnv(this, STypeEnv.extractVariableBindings(m, api))

  /** Extend me with the bindings of the given typeconses relation. */
  def extendWithTypeConses[T <: TypeConsIndex](m: JMap[Id, T],
                                               api: Option[APIName]): STypeEnv =
    new NestedSTypeEnv(this, STypeEnv.extractTypeConsBindings(m, api))

  def extendWithBindingsFromFnList[T <: Functional](fns:List[T]) =
    new NestedSTypeEnv(this, STypeEnv.extractFunctionBindings(fns.map( x => (x.name, Set(x))), None))

  /** Extend me with the bindings of the given functions relation. */
  def extendWithFunctions[T <: Functional]
      (r: Relation[IdOrOpOrAnonymousName, T],
       api: Option[APIName]): STypeEnv =
    new NestedSTypeEnv(this, STypeEnv.extractFunctionBindings(r, api))

  /** Extend me with the bindings of the given variables relation. */
  def extendWithVariables[T <: Variable](m: JMap[Id, T]): STypeEnv =
    extendWithVariables(m, None)

  /** Extend me with the bindings of the given typeconses relation. */
  def extendWithTypeConses[T <: TypeConsIndex](m: JMap[Id, T]): STypeEnv =
    extendWithTypeConses(m, None)

  /** Extend me with the bindings of the given functions relation. */
  def extendWithFunctions[T <: Functional]
      (r: Relation[IdOrOpOrAnonymousName, T]): STypeEnv =
    extendWithFunctions(r, None)

  def extendWithListOfFunctions[T <: Functional](fns:List[T]): STypeEnv =
    extendWithBindingsFromFnList(fns)

  /** Extend me without the bindings with the given names. */
  def extendWithout[T <: Name](names: Iterable[T]): STypeEnv =
    new ConcealingSTypeEnv(this, names)

  /** Same as `lookup`. */
  def getType(x: Name): Option[Type] =
    lookup(x).flatMap(_.typeThunk.apply)

  /** Get the modifiers for the given name, if it exists. */
  def getMods(x: Name): Option[Modifiers] = lookup(x).map(_.mods)

  /** Return whether the given name is mutable, if it exists. */
  def isMutable(x: Name): Boolean = lookup(x) match {
    case Some(binding) =>
      binding.mutable || binding.mods.isMutable
    case _ => false
  }

  /** Get the functional indices for this name, if any. */
  def getFnIndices(x: Name): Option[List[Functional]] =
    lookup(x).map(_.fnIndices)
}

/** The single empty type environment. */
object EmptySTypeEnv extends STypeEnv with EmptyStaticEnv[Type]

/**
 * A type environment with a parent and some explicit bindings.
 *
 * @param parent A type environment that this one extends.
 * @param _bindings A collection of all the bindings in this environment.
 */
class NestedSTypeEnv (protected val parent: STypeEnv,
                                _bindings: Iterable[TypeBinding])
    extends STypeEnv with NestedStaticEnv[Type] {

  /** Internal representation of `bindings` is a map. */
  val bindings: Map[Name, TypeBinding] =
    Map(_bindings.map(b => (b.name, b)).toSeq:_*)
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
  def make(comp: CompilationUnitIndex): STypeEnv = {
    val api = if (comp.isInstanceOf[ApiIndex]) Some(comp.ast.getName) else None
    EmptySTypeEnv.extendWithFunctions(comp.functions, api)
                 .extendWithVariables(comp.variables, api)
                 .extendWithTypeConses(comp.typeConses, api)
  }

  /** Create a binding with the given information. */
  protected def makeBinding(name: Name,
                            typeThunk: TypeThunk,
                            mods: Modifiers,
                            mutable: Boolean,
                            functions: Iterable[Functional]): TypeBinding =
    TypeBinding(name, typeThunk, mods, mutable, functions.toList)

  /**
   * Create a binding with the given information, where the type thunk simply
   * evaluates to the given type.
   */
  protected def makeBinding(name: Name,
                            typ: Type,
                            mods: Modifiers,
                            mutable: Boolean): TypeBinding = {
    val typeThunk = new TypeThunk { def apply: Option[Type] = Some(typ) }
    TypeBinding(name, typeThunk, mods, mutable, Nil)
  }

  /** Extract out the bindings in node. */
  protected def extractNodeBindings(node: Node): Iterable[TypeBinding] =
    node match{
      case SParam(_, name, mods, _, _, Some(vaTyp)) =>
        List(makeBinding(name, vaTyp, mods, false))
      case SParam(_, name, mods, Some(typ), _, _) =>
        typ match {
          case p@SPattern(_,_,_) => bug("Pattern should be desugared away: " + p)
          case t@SType(_) => List(makeBinding(name, t, mods, false))
        }
      case SParam(_, name, mods, _, _, _) =>
        throw TypeError.make("Missing parameter type for " + name, node)

      case SLValue(_, name, mods, Some(typ), mutable) =>
        typ match {
          case p@SPattern(_,_,_) => bug("Pattern should be desugared away: " + p)
          case t@SType(_) => List(makeBinding(name, t, mods, mutable))
        }

      case SLocalVarDecl(_, _, lValues, _) =>
        lValues.flatMap(extractNodeBindings)

      case _ =>
        throw TypeError.make("Cannot handle " + node.asInstanceOf[AbstractNode].toStringVerbose, node)
    }

  protected def extractTypeConsBindings[T <: TypeConsIndex]
      (m: JMap[Id, T], api: Option[APIName]): Iterable[TypeBinding] =

    toMap(m).flatMap(xv => {
      xv match {
        // Bind object names to their types.
        case (x:Id, v:ObjectTraitIndex) =>
          val decl = v.ast
          val qualifiedName = api match {
            case Some(api) => NF.makeId(api, x)
            case None =>
              // The trait type might have a qualified name (i.e. if it is being
              // exported), so get that name from the SelfType.
              toOption(decl.getSelfType) match {
                case Some(STraitSelfType(_, STraitType(_, name, _, _), _)) => name
                case _ => x
              }
          }
          val params = toOption(NU.getParams(decl))
          val sparams = NU.getStaticParams(decl)
          val objType = params match {
            case None if sparams.isEmpty =>
              // Just a single trait type.
              NF.makeTraitType(qualifiedName)
            case None =>
              // A generic trait type.
              NF.makeGenericSingletonType(qualifiedName, sparams)
            case Some(params) if sparams.isEmpty =>
              // Arrow type for basic constructor.
              NF.makeArrowType(NU.getSpan(qualifiedName),
                               // Constructors must have all param types.
                               makeDomainType(toListFromImmutable(params)).get,
                               NF.makeTraitType(qualifiedName))
            case Some(params) =>
              // Generic arrow type for constructor.
              val sargs = toListFromImmutable(sparams).map(staticParamToArg)
              NF.makeArrowType(NU.getSpan(decl),
                               false,
                               // Constructors must have all param types.
                               makeDomainType(toListFromImmutable(params)).get,
                               NF.makeTraitType(qualifiedName,
                                                toJavaList(sargs)),
                               NF.emptyEffect, // TODO: Change this?
                               sparams,
                               NU.getWhereClause(decl))
          }
          List(makeBinding(x, objType, Modifiers.None, false))
        case _ => Nil
      }
    })

  protected def extractVariableBindings[T <: Variable]
      (m: JMap[Id, T], api: Option[APIName]): Iterable[TypeBinding] = {

      toMap(m).map(xv => {
        val (x:Id, v:Variable) = xv

        // Lazily compute the type for this function binding at the time of
        // lookup.
        val lazyTypeEvaluation: TypeThunk = new TypeThunk {
          def apply: Option[Type] = v.getInferredType
        }
        makeBinding(x, lazyTypeEvaluation, v.modifiers, v.mutable, Nil)
      })
    }


  protected def extractFunctionBindings[T <: Functional]
      (r: Relation[IdOrOpOrAnonymousName, T],
       api: Option[APIName]): Iterable[TypeBinding] = {
    val fns = toSet(r.firstSet)
    // For each name, intersect together all of its overloading types.
    val map = fns.map(x => (x, toSet(r.matchFirst(x).asInstanceOf[JSet[Functional]])))
    extractFunctionBindings(map,api)
  }

  protected def extractFunctionBindings[S <: Name, T <: Functional]
                                       (functions: Iterable[(S,Set[T])],
                                        api: Option[APIName]): Iterable[TypeBinding] = {

    // Collect all bindings found among all these functions.
    functions.flatMap { nameAndFunctions =>
      val (f, fnsSet) = nameAndFunctions
      val fns = fnsSet.toList

      // Create the binding for each overloading of the function named f.
      val unambiguousBindings = fns.map { fn =>

        // Create a lazy computation for the type of this overloading.
        val lazyType = new TypeThunk {
          def apply: Option[Type] = makeArrowFromFunctional(fn)
        }

        // Bind the unambiguous name of this overloading to its type.
        makeBinding(unqualifiedName(fn.unambiguousName),
                    lazyType,
                    fn.mods,
                    false,
                    List(fn))
      }

      // Create a lazy computation for the type of the whole overloaded
      // function named x.
      val ambiguousThunk = new TypeThunk {
        def maff(f:Functional) = makeArrowFromFunctional(f)
        def apply: Option[Type] = {
          val oTypes = fns.flatMap(maff)
          if (oTypes.isEmpty)
            None
          else
            Some(NF.makeMaybeIntersectionType(toJavaSet(oTypes)))
        }
      }

      // Create the modifiers for the whole binding.
      val ambiguousMods =
        if (fns.isEmpty)
          Modifiers.None
        else fns.find(_.mods != Modifiers.None) match {
          case Some(f) => f.mods
          case _ => Modifiers.None
        }

      // Bind the ambiguous, plain name of this function to the intersection
      // of all its overloadings' types.
      val ambiguousBinding = makeBinding(unqualifiedName(f),
                                         ambiguousThunk,
                                         ambiguousMods,
                                         false,
                                         fns)

      // Return all the bindings.
      ambiguousBinding :: unambiguousBindings
    }
  }
}


/**
 * A binding for a type environment contains name to type pairs, along with some
 * modifiers for the binding and whether or not the binding is mutable.
 *
 * @param name The variable name for the binding.
 * @param typeThunk A function that when evaluated yields a type for this
 *                  binding.
 * @param mods Any modifiers for the binding.
 * @param mutable Whether or not the binding is mutable.
 * @param fnIndices A list of Functional indices, if any, to which this name
 *                  refers.
 */
case class TypeBinding(name: Name,
                       typeThunk: TypeThunk,
                       mods: Modifiers,
                       mutable: Boolean,
                       fnIndices: List[Functional])

/**
 * A special type environment that conceals the given names from its children.
 *
 * @param parent The parent environment.
 * @param concealedNames The set of names to conceal.
 */
class ConcealingSTypeEnv(protected val parent: STypeEnv,
                         protected val concealedNames: Iterable[Name])
    extends STypeEnv {
  
  /** Approximately correct? */
  def isEmpty: Boolean = parent match {
    case parent:NestedSTypeEnv => 
      parent.bindings.map(_._2.name)
            .filterNot(concealedNames.toList.contains).isEmpty
    case _ => true // case EmptySTypeEnv
  }
  
  /** Fail when looking up any of the hidden names. */
  override def lookup(x: Name): Option[TypeBinding] = {
    val stripped = stripApi(x)
    if (concealedNames.exists(_ == stripped))
      None
    else
      parent.lookup(x)
  }
}
