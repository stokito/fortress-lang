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

package com.sun.fortress.scala_src.typechecker

import _root_.java.util.ArrayList
import com.sun.fortress.compiler.GlobalEnvironment
import com.sun.fortress.compiler.index.CompilationUnitIndex
import com.sun.fortress.compiler.index.TraitIndex
import com.sun.fortress.compiler.typechecker.StaticParamTypeEnv
import com.sun.fortress.compiler.typechecker.StaticTypeReplacer
import com.sun.fortress.compiler.typechecker.TypeAnalyzer
import com.sun.fortress.exceptions.StaticError
import com.sun.fortress.exceptions.TypeError
import com.sun.fortress.nodes._
import com.sun.fortress.scala_src.nodes._
import com.sun.fortress.scala_src.useful.Lists._
import com.sun.fortress.scala_src.useful.Options._

class TypeWellFormedChecker(compilation_unit: CompilationUnitIndex,
                            globalEnv: GlobalEnvironment,
                            typeAnalyzer: TypeAnalyzer) extends Walker {
  val errors = new ArrayList[StaticError]()
  var analyzer = typeAnalyzer

  def check() = {
    walk(compilation_unit.ast)
    errors
  }

  private def error(s:String, n:Node) = errors.add(TypeError.make(s,n))

  private def getTypes(typ:Id) = {
    val types = typ match {
      case SId(info,Some(name),text) =>
        globalEnv.api(name).typeConses.get(SId(info,None,text))
      case _ => compilation_unit.typeConses.get(typ)
    }
    if (types == null) error("Unknown type: " + typ, typ)
    types
  }

  override def walk(node:Any):Any = {
    node match {
      // Static parameters and where-clause variables may be introduced.
      // To Do: Check for the where-clause variables.
      case STypeAlias(_, _, sparams, typeDef) =>
        val oldAnalyzer = analyzer
        analyzer = analyzer.extend(sparams, None)
        walk(typeDef)
        analyzer = oldAnalyzer
      case STraitDecl(_,
                      STraitTypeHeader(sparams, _, _, where,
                                       throwsC, contract, extendsC, decls),
                      self, excludes, comprises, _) =>
        val oldAnalyzer = analyzer
        analyzer = analyzer.extend(sparams, where)
        walk(sparams); walk(where); walk(throwsC); walk(contract); walk(extendsC)
        walk(decls); walk(self); walk(excludes); walk(comprises)
        analyzer = oldAnalyzer
      case SObjectDecl(_,
                       STraitTypeHeader(sparams, _, _, where,
                                        throwsC, contract, extendsC, decls),
                       self, params) =>
        val oldAnalyzer = analyzer
        analyzer = analyzer.extend(sparams, where)
        walk(sparams); walk(where); walk(throwsC); walk(contract); walk(extendsC)
        walk(decls); walk(self); walk(params)
        analyzer = oldAnalyzer
      case SFnDecl(_,
                   SFnHeader(sparams, _, _, where,
                             throwsC, contract, params, returnType),
                   _, body, _) =>
        val oldAnalyzer = analyzer
        analyzer = analyzer.extend(sparams, where)
        walk(sparams); walk(where); walk(throwsC); walk(contract); walk(params)
        walk(returnType); walk(body)
        analyzer = oldAnalyzer

      // Check the well-formedness of types.
      case SAnyType(_) => // OK
      case SBottomType(_) => // OK
      case t@SVarType(_, name, _) =>
        if ( ! analyzer.kindEnv.contains(name) )
          error("Unbound type: " + name, t)
      case t@STraitType(_, name, sargs, _) =>
        getTypes(name) match {
          case si:TraitIndex => // Trait name should be defined.
            // Static arguments should satisfy the corresponding bounds.
            val sparams = si.staticParameters
            if ( sargs.size == sparams.size ) {
              val replacer = new StaticTypeReplacer(sparams, toJavaList(sargs))
              def wfStaticArgs(pair:(StaticArg,StaticParam)) =
                for ( bound <- toList(pair._2.getExtendsClause);
                      if pair._1.isInstanceOf[TypeArg] ) {
                  val new_bound = replacer.replaceIn(bound)
                  if ( ! analyzer.subtype(pair._1.asInstanceOf[TypeArg].getTypeArg,
                                          new_bound).isTrue )
                    error("Ill-formed type: " + t +
                          "\n    The static argument " + pair._1 +
                          " does not satisfy the corresponding bound " + new_bound + ".", t)
                }
              sargs.zip(toList(sparams)).foreach(wfStaticArgs)
            } else error("Ill-formed type: " + t +
                       "\n    The numbers of the static parameters and " +
                       "the static arguments do not match.", t)
          case _ => error("Unbound type: " + name, t)
        }
      // Keyword parameters are not yet supported...
      case STupleType(_, elements, varargs, keywords) =>
        elements.foreach((t:Type) => walk(t))
        varargs match {
          case Some(ty) => walk(ty)
          case _ =>
        }
      // Effects are not yet supported...
      case SArrowType(_, domain, range, effect, io, _) => walk(domain); walk(range)
      case SIntersectionType(_, elements) => elements.foreach((t:Type) => walk(t))
      case SUnionType(_, elements) => elements.foreach((t:Type) => walk(t))
      case SLabelType(_) => // OK
      case SDimBase(_) => // OK
      /* Not yet implemented...
      case SFixedPointType(_, name, body) =>
      case STaggedDimType(_, elemType, dimExpr, unitExpr) =>
      case STaggedUnitType(_, elemType, unitExpr) =>
      case SDimRef(_, name) =>
      case SDimExponent(_, base, power) =>
      case SDimUnaryOp(_, dimVal, op) =>
      case SDimBinaryOp(_, left, right, op) =>
      */
      case _ => super.walk(node)
    }
    node
  }
}
