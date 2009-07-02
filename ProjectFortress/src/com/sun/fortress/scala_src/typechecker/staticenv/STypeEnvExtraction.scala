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

trait STypeEnvExtraction { self: STypeEnv.type =>
  
  /** Extract out the bindings in node. */
  def extractEnvBindings(node: Any): Collection[TypeBinding] = {
    def recur(node: Any) = extractEnvBindings(node)
    (node match {
      
      case SBinding(_, name, mods, Some(typ)) =>
        TypeBinding(name, typ, mods, false)
      
      case SParam(_, name, mods, _, _, Some(vaTyp)) =>
        TypeBinding(name, vaTyp, mods, false)
      case SParam(_, name, mods, Some(typ), _, _) =>
        TypeBinding(name, typ, mods, false)
      
      case SLValue(_, name, mods, Some(typ), mutable) =>
        TypeBinding(name, typ, mods, mutable)
      
      case SLocalVarDecl(_, _, lValues, _) => lValues.map(recur)
      
      case v:DeclaredVariable => recur(v.ast)
      
      // Type erasure makes us put all JMap cases in one.
      case m:JMap[_, _] => toMap(m).flatMap(xv =>
        xv match {
          
          // Match Map[Id, Variable]
          case (x:Id, v:DeclaredVariable) => recur(v)
          case (x:Id, v:SingletonVariable) =>
            recur(NF.makeLValue(x, v.declaringTrait))
          case (x:Id, v:ParamVariable) => recur(v.ast)
          
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
        })
      
      // Matches all, but must be [IdOrOpOrAnonymousName, ? <: Functional]
      case r:Relation[IdOrOpOrAnonymousName, Functional] =>
        val fnNames = toSet(r.firstSet)
        
        // For each name, intersect together all of its overloading types.
        fnNames.flatMap(x => x match {
          
          // Make sure this is actually a name.
          case x: IdOrOpOrAnonymousName => 
            val fns: Set[Functional] = toSet(r.matchFirst(x).asInstanceOf[JSet[Functional]])
            val oTypes =
              fns.map(STypesUtil.makeArrowFromFunctional(_).asInstanceOf[Type])
            val fnType = NF.makeIntersectionType(oTypes)
            Some(TypeBinding(x, fnType, Modifiers.None, false))
            
//          case _ => None
        })
      
      case xs:Iterable[_] => xs.flatMap(recur)
      
      case _ => Nil
      
    }) match { // Guarantee that a Collection is returned.
      case bs:Collection[TypeBinding] => bs
      case b:TypeBinding => List(b)
    }
  }
}
