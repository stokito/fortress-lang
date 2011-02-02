/*******************************************************************************
    Copyright 2009,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.scala_src.typechecker.impls

import edu.rice.cs.plt.collect.IndexedRelation
import edu.rice.cs.plt.collect.Relation
import com.sun.fortress.compiler.index._
import com.sun.fortress.compiler.typechecker.StaticTypeReplacer
import com.sun.fortress.exceptions.InterpreterBug.bug
import com.sun.fortress.exceptions.StaticError.errorMsg
import com.sun.fortress.nodes._
import com.sun.fortress.nodes_util.NodeUtil
import com.sun.fortress.nodes_util.NodeFactory
import com.sun.fortress.useful.NI
import com.sun.fortress.scala_src.nodes._
import com.sun.fortress.scala_src.typechecker._
import com.sun.fortress.scala_src.useful.Lists._
import com.sun.fortress.scala_src.useful.Options._
import com.sun.fortress.scala_src.useful.Sets._
import com.sun.fortress.scala_src.useful.STypesUtil._

/**
 * Provides some helper methods that are common to cases among multiple groups.
 * Note that helpers that are generally used in _every_ case/group should go in
 * the STypeChecker class; the helpers herein each generally apply to cases
 * among only a few groups.
 *
 * This trait must be mixed in with an STypeChecker instance to provide the
 * full type checker implementation.
 *
 * (The self-type annotation at the beginning declares that this trait must be
 * mixed into STypeChecker. This is what allows this trait to access its
 * protected members.)
 */
trait Common { self: STypeChecker =>

  // TODO: Consider merging with STypesUtil.inheritedMethods
  def inheritedMethods(extendedTraits: Iterable[TraitTypeWhere])
                      : Relation[IdOrOpOrAnonymousName, Method] = {
    def inheritedMethodsHelper(history: HierarchyHistory,
                               methods: Relation[IdOrOpOrAnonymousName, Method],
                               extended_traits: Iterable[TraitTypeWhere])
                               : scala.Unit = {
      for ( STraitTypeWhere(_, ty: TraitType, _) <- extended_traits;
            if history.explore(ty)) {
        toOption(traits.typeCons(ty.getName)) match {
          case Some(ti : TraitIndex) =>
            val trait_params = ti.staticParameters
            val trait_args = ty.getArgs

            // Instantiate methods with static args
            val dotted = toSet(ti.asInstanceOf[TraitIndex].dottedMethods).map(t => (t.first, t.second))
            for ( pair <- dotted ) {
                methods.add(pair._1,
                            pair._2.instantiate(trait_params,trait_args).asInstanceOf[Method])
            }
            val getters = ti.asInstanceOf[TraitIndex].getters
            for ( getter <- toSet(getters.keySet) ) {
                methods.add(getter,
                            getters.get(getter).instantiate(trait_params,trait_args).asInstanceOf[Method])
            }
            val setters = ti.asInstanceOf[TraitIndex].setters
            for ( setter <- toSet(setters.keySet) ) {
                methods.add(setter,
                            setters.get(setter).instantiate(trait_params,trait_args).asInstanceOf[Method])
            }
            val paramsToArgs = new StaticTypeReplacer(trait_params, trait_args)
            val instantiated_extends_types =
              toListFromImmutable(ti.asInstanceOf[TraitIndex].extendsTypes).map( (t:TraitTypeWhere) =>
                    t.accept(paramsToArgs).asInstanceOf[TraitTypeWhere] )
            inheritedMethodsHelper(history.copy, methods, instantiated_extends_types)
          case _ =>
        }
      }
    }
    val methods = new IndexedRelation[IdOrOpOrAnonymousName, Method](false)
    inheritedMethodsHelper(new HierarchyHistory(), methods, extendedTraits)
    methods
  }

  protected def findMethodsInTraitHierarchy(methodName: IdOrOpOrAnonymousName,
                                            receiverType: Type)
                                            : Set[Method] = {

    val traitTypes = traitTypesCallable(receiverType)
    //TODO: What does the next line do?
    val ttAsWheres = traitTypes.map(NodeFactory.makeTraitTypeWhere)
    val allMethods = inheritedMethods(ttAsWheres)
    toSet(allMethods.matchFirst(methodName))
  }

  def getGetterType(fieldName: IdOrOpOrAnonymousName, receiverType: Type): Option[Type] = {
    // We can just assume there is a getter index for every field
    val methods = findMethodsInTraitHierarchy(fieldName, receiverType)
    def isGetter(m: Method): Option[FieldGetterMethod] = m match {
      case g:FieldGetterMethod => Some(g)
      case _ => None
    }
    val getters = methods.flatMap(isGetter).toList
    // TODO: Figure out what the type should be if there are overridden getters.
    getters.headOption.flatMap(g => toOption(g.getReturnType))
  }

  def getSetterType(fieldName: IdOrOpOrAnonymousName, receiverType: Type): Option[Type] = {
    //We can just assume there is a getter for every field
    val methods = findMethodsInTraitHierarchy(fieldName, receiverType)
    def isSetter(m: Method): Option[FieldSetterMethod] = m match {
      case s:FieldSetterMethod => Some(s)
      case _ => None
    }
    val setters = methods.flatMap(isSetter).toList
    // TODO: Figure out what the type should be if there are overridden setters.
    setters.headOption.flatMap(s => makeArrowFromFunctional(s).map(_.getDomain))
  }

  /**
   * Given a type, which could be a VarType, Intersection or Union, return the TraitTypes
   * that this type could be used as for the purposes of calling methods and fields.
   */
  protected def traitTypesCallable(typ: Type): Set[TraitType] = typ match {
    case t:TraitSelfType => traitTypesCallable(t.getNamed)
    case t@SObjectExprType(_, extended) => Set(extended.flatMap(traitTypesCallable):_*)
    case t:TraitType => Set(t)

    // Combine all the trait types callable from constituents.
    case typ:IntersectionType =>
      conjuncts(typ).filter(NodeUtil.isTraitType).flatMap(traitTypesCallable)

    // Get the trait types callable from the upper bounds of this parameter.
    case SVarType(_, name, _) => toOption(analyzer.env.staticParam(name)) match {
      case Some(s@SStaticParam(_, _, ts, _, _, SKindType(_), _)) =>
        Set(ts:_*).filter(NodeUtil.isTraitType).flatMap(traitTypesCallable)
      case _ => Set.empty[TraitType]
    }

    case SUnionType(_, ts) =>
      NI.nyi("You should be able to call methods on this type," + typ +
                           "but this is not yet implemented.")
      Set.empty[TraitType]

    case _ => Set.empty[TraitType]
  }

}
