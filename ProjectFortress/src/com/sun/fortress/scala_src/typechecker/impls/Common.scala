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

  protected def findMethodsInTraitHierarchy(methodName: IdOrOp,
                                            receiverType: Type)
                                            : Set[Method] = {

    val traitTypes = traitTypesCallable(receiverType)
    val ttAsWheres = traitTypes.map(NodeFactory.makeTraitTypeWhere)
    val allMethods = commonInheritedMethods(ttAsWheres, analyzer.traits).groupBy(_.name)
    allMethods.getOrElse(methodName, List[Method]()).toSet
  }

  def smaller(g1:FieldGetterOrSetterMethod, g2:FieldGetterOrSetterMethod): Boolean = 
    isSubtype(g1.selfType.unwrap, g2.selfType.unwrap)
  
  def getGetterType(fieldName: IdOrOp, receiverType: Type): Option[Type] = {
    // We can just assume there is a getter index for every field
    val methods = findMethodsInTraitHierarchy(fieldName, receiverType)
    def isGetter(m: Method): Option[FieldGetterMethod] = m match {
      case g:FieldGetterMethod => Some(g)
      case _ => None
    }
    val getters = methods.flatMap(isGetter).toList
    
    if (getters.isEmpty) None else 
    toOption(getters.min(Ordering.fromLessThan(smaller)).getReturnType)
    // getters.headOption.flatMap(g => toOption(g.getReturnType))
  }

  def getSetterType(fieldName: IdOrOp, receiverType: Type): Option[Type] = {
    //We can just assume there is a getter for every field
    val methods = findMethodsInTraitHierarchy(fieldName, receiverType)
    def isSetter(m: Method): Option[FieldSetterMethod] = m match {
      case s:FieldSetterMethod => Some(s)
      case _ => None
    }
    val setters = methods.flatMap(isSetter).toList
    if (setters.isEmpty) None else
    makeArrowFromFunctional(setters.min(Ordering.fromLessThan(smaller))).map(_.getDomain)
    // setters.headOption.flatMap(s => makeArrowFromFunctional(s).map(_.getDomain))
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
      case Some(s@SStaticParam(_, _, _, ts, _, _, _, SKindType(_), _)) =>      // TODO: variance needs to be addressed
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
