/*******************************************************************************
    Copyright 2009,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.scala_src.typechecker

import _root_.java.util.ArrayList
import _root_.java.util.{HashMap => JavaHashMap}
import _root_.java.util.{List => JavaList}
import _root_.java.util.{Set => JavaSet}
import edu.rice.cs.plt.tuple.{Option => JavaOption}
import scala.collection.Set
import com.sun.fortress.compiler.GlobalEnvironment
import com.sun.fortress.compiler.index.ComponentIndex
import com.sun.fortress.compiler.index.ObjectTraitIndex
import com.sun.fortress.compiler.index.ProperTraitIndex
import com.sun.fortress.compiler.index.TypeConsIndex
import com.sun.fortress.compiler.index.{Function => JavaFunction}
import com.sun.fortress.exceptions.StaticError
import com.sun.fortress.exceptions.TypeError
import com.sun.fortress.nodes._
import com.sun.fortress.nodes_util.NodeFactory
import com.sun.fortress.nodes_util.NodeUtil
import com.sun.fortress.nodes_util.Span
import com.sun.fortress.parser_util.IdentifierUtil
import com.sun.fortress.repository.FortressRepository
import com.sun.fortress.scala_src.typechecker.Formula._
import com.sun.fortress.scala_src.types.TypeAnalyzer
import com.sun.fortress.scala_src.useful._
import com.sun.fortress.scala_src.useful.ErrorLog
import com.sun.fortress.scala_src.useful.Lists._
import com.sun.fortress.scala_src.useful.Options._
import com.sun.fortress.scala_src.useful.Sets._
import com.sun.fortress.scala_src.nodes._

/*
 * ToDo: Unlike subtyping checking, all the other checking such as
 * exclusion checking and meet and join operations are not cached.
 * We'll cache the results of those questions later.
 */
class ExclusionOracle(errors: ErrorLog)(implicit typeAnalyzer: TypeAnalyzer) {
  /* Checks the overloading rule: exclusion
   * Invariant: firstParam is not equal to secondParam
   * The following types are not yet supported:
   *     Types tagged with dimensions or units
   *     Effects and io on arrow types
   *     Keyword parameters and varargs parameters
   *     Intersection types
   *     Union types
   *     Fixed-point types
   * The following TypeConsIndex's are not checked:
   *     TypeAliasIndex
   *     Dimension
   *     Unit
   * TupleType's with varargs parameters or keyword parameters are not supprted.
   */
  def excludes(first: Type, second: Type): Boolean = {
    val a_temporary_I_can_see_in_my_debugger =
      ( typeAnalyzer.normalize(first), typeAnalyzer.normalize(second) )
    a_temporary_I_can_see_in_my_debugger match {
      case (SBottomType(_), _) => true
      case (_, SBottomType(_)) => true
      case (SAnyType(_), _) => false
      case (_, SAnyType(_)) => false
      case (f:TupleType, s:TupleType) =>
        NodeUtil.differentArity(f, s) || {
          f.getVarargs.isNone && s.getVarargs.isNone &&
          f.getKeywords.isEmpty && s.getKeywords.isEmpty &&
          f.getElements.size == s.getElements.size &&
          toListFromImmutable(f.getElements).zip(toListFromImmutable(s.getElements)).exists((e:(Type,Type)) =>
                                                                  excludes(e._1,e._2))
        }
      case (f:TupleType ,_) if NodeUtil.isVoidType(f) => true
      case (_, s:TupleType) if NodeUtil.isVoidType(s) => true
      case (f@SVarType(_,_,_), s@SVarType(_,_,_)) =>
        ( toOption(typeAnalyzer.env.staticParam(f.getName)),
          toOption(typeAnalyzer.env.staticParam(s.getName)) ) match {
          case (Some(fp), Some(sp)) =>
            var result = false
            if ( NodeUtil.isTypeParam( fp ) ) {
              for ( ty <- toListFromImmutable(fp.getExtendsClause) ; if ! result ) {
                if ( excludes(ty, s) ) result = true
              }
            }
            if ( NodeUtil.isTypeParam( sp ) ) {
              for ( ty <- toListFromImmutable(sp.getExtendsClause) ; if ! result ) {
                if ( excludes(ty, f) ) result = true
              }
            }
            result
          case (None, Some(_)) =>
            errors.signal("We are being asked about some type that is not in scope:\n    " +
                          f, NodeUtil.getSpan(f))
            false
          case (_, None) =>
            errors.signal("We are being asked about some type that is not in scope:\n    " +
                          s, NodeUtil.getSpan(s))
            false
          case _ =>
            errors.signal("We are being asked about some type that is not in scope:\n    " +
                          f, NodeUtil.getSpan(f))
            errors.signal("We are being asked about some type that is not in scope:\n    " +
                          s, NodeUtil.getSpan(s))
            false
        }
      case (f@SVarType(_,_,_), _) =>
        toOption(typeAnalyzer.env.staticParam(f.getName)) match {
          case Some(fp) =>
            var result = false
            if ( NodeUtil.isTypeParam( fp ) ) {
              for ( ty <- toListFromImmutable(fp.getExtendsClause) ; if ! result ) {
                if ( excludes(ty, second) ) result = true
              }
            }
            result
          case _ =>
            errors.signal("We are being asked about some type that is not in scope:\n    " +
                          f, NodeUtil.getSpan(f))
            false
        }
      case (_, s@SVarType(_,_,_)) =>
        toOption(typeAnalyzer.env.staticParam(s.getName)) match {
          case Some(sp) =>
            var result = false
            if ( NodeUtil.isTypeParam( sp ) ) {
              for ( ty <- toListFromImmutable(sp.getExtendsClause) ; if ! result ) {
                if ( excludes(ty, first) ) result = true
              }
            }
            result
          case _ =>
            errors.signal("We are being asked about some type that is not in scope:\n    " +
                          s, NodeUtil.getSpan(s))
            false
        }
      case (_:ArrowType, _:ArrowType) => false
      case (_:ArrowType, _) => true
      case (_, _:ArrowType) => true
      case (_:TupleType ,_) => true
      case (_, _:TupleType) => true
      case (f:TraitSelfType, s:TraitSelfType) => excludes(f.getNamed, s.getNamed)
      case (f:TraitType, s:TraitSelfType) => excludes(f, s.getNamed)
      case (f:TraitSelfType, s:TraitType) => excludes(f.getNamed, s)
      case (f:ObjectExprType, s:ObjectExprType) => true
      case (f:TraitType, s:TraitType) =>
        ( toOption(typeAnalyzer.traits.typeCons(f.getName)),
          toOption(typeAnalyzer.traits.typeCons(s.getName)) ) match {
          case (Some(fi), Some(si)) =>
            ( NodeUtil.isTraitOrObject(fi), NodeUtil.isTraitOrObject(si) ) match {
              case (true, true) =>
                if ( ! (isTrue(typeAnalyzer.subtype(f, s)) ||
                        isTrue(typeAnalyzer.subtype(s, f))) &&
                     ( isClosedType(fi) || isClosedType(si) ) )
                  return true
                else if ( fi.isInstanceOf[ProperTraitIndex] &&
                          si.isInstanceOf[ProperTraitIndex] )
                  excludes(f, s, si.asInstanceOf[ProperTraitIndex]) ||
                  excludes(s, f, fi.asInstanceOf[ProperTraitIndex])
                else false
              case _ => false
            }
          case (None, Some(_)) =>
            errors.signal("Unrecognized name: " + f.getName, NodeUtil.getSpan(f))
            false
          case (_, None) =>
            errors.signal("Unrecognized name: " + s.getName, NodeUtil.getSpan(s))
            false
          case _ =>
            errors.signal("Unrecognized name: " + f.getName, NodeUtil.getSpan(f))
            errors.signal("Unrecognized name: " + s.getName, NodeUtil.getSpan(s))
            false
        }
      case _ => false
    }
  }
  
  private def transitivelyExcludes(tIndex: ProperTraitIndex): Set[TraitType] = {
    var result = toSet(tIndex.excludesTypes)
    for ( t <- toListFromImmutable(tIndex.extendsTypes) ) {
      if ( t.getBaseType.isInstanceOf[NamedType] ) {
        result ++= transitivelyExcludes(typeAnalyzer.traits.typeCons(t.getBaseType.asInstanceOf[NamedType].getName).unwrap.asInstanceOf[ProperTraitIndex])
      }
    }
    result
  }

  /* Exists "tau" in "[X |-> ty...]S" such that "s" is a subtype of "tau"
   * where "t" is "T[\ty...\]" and "trait T[\X...\] ... excludes S ... end"
   */
  private def excludes(s: TraitType, t: TraitType, tIndex: ProperTraitIndex) = {
    // staticParams: X...
    val staticParams = toListFromImmutable(tIndex.staticParameters)
    // staticArgs  : ty...
    val staticArgs   = toListFromImmutable(t.getArgs)
    if ( staticParams.size != staticArgs.size )
      errors.signal("The numbers of static parameters and static arguments do not match: " + t,
                    NodeUtil.getSpan(t))
    // subst       : [X |-> ty ...]
    val subst = new JavaHashMap[IdOrOp,Type]()
    staticParams.zip(staticArgs).foreach( (pair:(StaticParam,StaticArg)) =>
                                          if ( pair._2.isInstanceOf[TypeArg] )
                                            subst.put(pair._1.getName,
                                                      pair._2.asInstanceOf[TypeArg].getTypeArg) )
    // excludes    : S
    var excludes: Set[TraitType] = transitivelyExcludes(tIndex)
    // [X |-> ty ...]S
    def saSubst(sa: StaticArg): StaticArg = sa match {
      case STypeArg(i,l,t) => STypeArg(i, l, tySubst(t) )
      case _ => sa
    }
    def spSubst(sp: StaticParam): StaticParam = sp match {
      case SStaticParam(i,v,n,e,dominator,d,a,k,l) =>
        SStaticParam(i, v, n, e.map(tySubst).asInstanceOf[List[BaseType]], dominator,d, a, k, l)
    }
    def tySubst(tau: Type): Type = tau match {
      case SVarType(_,name,_) =>
        if ( subst.containsKey(name) ) subst.get(name)
        else {
          errors.signal("Type variable " + tau + " is unbound.", NodeUtil.getSpan(tau))
          tau
        }
      case STraitType(i,n,a,p) => STraitType(i, n, a.map(saSubst), p.map(spSubst))
      case STraitSelfType(i,n,ts) =>
        STraitSelfType(i, tySubst(n).asInstanceOf[BaseType],
                       ts.map(tySubst).asInstanceOf[List[NamedType]])
      case SObjectExprType(i,ts) =>
        SObjectExprType(i, ts.map(tySubst).asInstanceOf[List[TraitType]])
      case STupleType(i,e,v,k) => STupleType(i, e.map(tySubst), v, k)
      case SArrowType(i,d,r,e,b,m) => SArrowType(i, tySubst(d), tySubst(r), e, b, m.map(miSubst))
      case _ => tau
    }
    def miSubst(mi: MethodInfo): MethodInfo = mi match {
      case SMethodInfo(t,p) => SMethodInfo(tySubst(t), p)
    }
    excludes = excludes.map( tySubst ).asInstanceOf[Set[TraitType]]
    var result = false
    // Exists "tau" in "[X |-> ty ...]S" such that "s" is a subtype of "tau"
    for ( tau <- excludes ; if ! result ) {
        if (isTrue(typeAnalyzer.subtype(s, tau))) result = true
    }
    result
  }

  /* A type is a closed type if and only if
   * it is an object trait type or
   * it is a trait type with a comprises clause containing only closed types.
   */
  private def isClosedType(s: TypeConsIndex): Boolean =
    if ( s.isInstanceOf[ObjectTraitIndex] ) true
    else if ( s.isInstanceOf[ProperTraitIndex] ) {
      val comprises = toSet(s.asInstanceOf[ProperTraitIndex].comprisesTypes)
      if ( comprises.isEmpty ) false
      else {
        var result = true
        for ( t <- comprises ; if result ) {
          if (t.isInstanceOf[VarType]) result = false
          else {
            toOption(typeAnalyzer.traits.typeCons(t.getName)) match {
              case None =>
                errors.signal("Unrecognized name: " + t.getName, NodeUtil.getSpan(t))
              case Some(ind) =>
                if ( ! isClosedType(ind) ) result = false
            }
          }
        }
        result
      }
    } else false

  def errors(): ErrorLog = errors
}
