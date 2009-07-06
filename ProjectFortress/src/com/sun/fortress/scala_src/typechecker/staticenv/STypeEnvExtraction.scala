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


/**
 * Provides the functionality for extracting type bindings from nodes and
 * indices to create type environments.
 */
trait STypeEnvExtraction  { self: STypeEnv.type =>
  
  /** Extract out the bindings in node. */
  def extractEnvBindings(node: Node): Iterable[TypeBinding] = {
    def recur(node: Node) = extractEnvBindings(node)
    (node match{
      case SBinding(_, name, mods, Some(typ)) =>
        TypeBinding(name, typ, mods, false)
      
      case SParam(_, name, mods, _, _, Some(vaTyp)) =>
        TypeBinding(name, vaTyp, mods, false)
      case SParam(_, name, mods, Some(typ), _, _) =>
        TypeBinding(name, typ, mods, false)
      
      case SLValue(_, name, mods, Some(typ), mutable) =>
        TypeBinding(name, typ, mods, mutable)
        
      case SLocalVarDecl(_, _, lValues, _) => lValues.map(recur)

      case _ => Nil
    }) match{
      case bs:Iterable[TypeBinding] => bs
      case b:TypeBinding => List(b)
    }
  }
  
//  def extractEnvBindings(m: JMap[Id, Variable]): Iterable[TypeBinding] = toMap(m).flatMap(xv =>{ 
//    xv match{
//      case (x:Id, v:DeclaredVariable) => extractEnvBindings(v.ast)
//      case (x:Id, v:SingletonVariable) =>
//        extractEnvBindings(NF.makeLValue(x, v.declaringTrait))
//      case (x:Id, v:ParamVariable) =>  extractEnvBindings(v.ast)
//    }
//  })
  
//  def extractEnvBindings(m: JMap[Id, TypeConsIndex]): Iterable[TypeBinding] = toMap(m).flatMap(xv => {
//    xv match {
//      // Bind object names to their types.
//      case (x:Id, v:ObjectTraitIndex) =>
//        val decl = v.ast
//        val params = toOption(NU.getParams(decl))
//        val sparams = NU.getStaticParams(decl)
//        val objType = params match {
//          case None if sparams.isEmpty =>
//            // Just a single trait type.
//            NF.makeTraitType(x)
//          case None =>
//            // A generic trait type.
//            NF.makeGenericSingletonType(x, sparams)
//          case Some(params) if sparams.isEmpty =>
//            // Arrow type for basic constructor.
//            NF.makeArrowType(NU.getSpan(x),
//                             STypesUtil.makeDomainType(toList(params)),
//                             NF.makeTraitType(x))
//          case Some(params) =>
//            // Generic arrow type for constructor.
//            val sargs = toList(sparams).map(STypesUtil.staticParamToArg)
//            NF.makeArrowType(NU.getSpan(decl),
//                             false,
//                             STypesUtil.makeDomainType(toList(params)),
//                             NF.makeTraitType(x, toJavaList(sargs)),
//                             NF.emptyEffect, // TODO: Change this?
//                             sparams,
//                             NU.getWhereClause(decl))
//        }
//        List(TypeBinding(x, objType, Modifiers.None, false))
//    }
//  })
  
  def extractEnvBindings(r: Relation[IdOrOpOrAnonymousName, Functional]): Iterable[TypeBinding] = {
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
  
  def make(comp: CompilationUnitIndex): STypeEnv = {
    EmptySTypeEnv//.extendWith(comp.functions)
     // .extendWith(comp.variables)
      //.extendWith(comp.typeConses)
  }
}
