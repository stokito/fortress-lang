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

package com.sun.fortress.scala_src.useful

import com.sun.fortress.compiler.typechecker.StaticTypeReplacer
import com.sun.fortress.compiler.typechecker.TypeAnalyzer
import com.sun.fortress.compiler.typechecker.TypesUtil
import com.sun.fortress.exceptions.InterpreterBug.bug
import com.sun.fortress.nodes._
import com.sun.fortress.nodes_util.NodeFactory
import com.sun.fortress.nodes_util.NodeUtil
import com.sun.fortress.scala_src.nodes._
import com.sun.fortress.scala_src.useful.Lists._
import com.sun.fortress.scala_src.useful.Options._
import com.sun.fortress.useful.HasAt
import com.sun.fortress.useful.NI

object STypesUtil {

  /**
   * A function type that takes two types and returns a boolean.
   */
  type Subtype = (Type, Type) => Boolean
  
  /**
   * Get all the static parameters out of the given type.
   */
  def getStaticParams(typ: Type): List[StaticParam] =
    toList(typ.getInfo.getStaticParams)
  
  /**
   * Return an identical type that has no static params.
   * TODO: How to handle where clauses in TypeInfos?
   */
  def clearStaticParams(typ: Type): Type = {
    
    // A walker that clears static params out of TypeInfos.
    object paramWalker extends Walker {
      override def walk(node: Any): Any = node match {
        case STypeInfo(_, _, Nil, _) => node
        case STypeInfo(a, b, _, _) => STypeInfo(a, b, Nil, None)
        case _ => super.walk(node)
      }
    }
    paramWalker(typ).asInstanceOf[Type]
  }
  
  /**
   * Insert static parameters into a type. If the type already has static
   * parameters, a bug is thrown.
   */
  def insertStaticParams(typ: Type, sparams: List[StaticParam]): Type = {
    
    // A walker that clears static params out of TypeInfos.
    object paramWalker extends Walker {
      override def walk(node: Any): Any = node match {
        case STypeInfo(a, b, Nil, c) => STypeInfo(a, b, sparams, c)
        case STypeInfo(_, _, _, _) =>
          bug("cannot overwrite static parameters")
        case _ => super.walk(node)
      }
    }
    paramWalker(typ).asInstanceOf
  }

  /**
   * Determine if the type previously inferred by the type checker from the
   * given expression is an arrow or intersection of arrow types.
   */
  def isArrows(expr: Expr): Boolean = isArrows(SExprUtil.getType(expr).get)
  
  /**
   * Determine if the given type is an arrow or intersection of arrow types.
   */
  def isArrows(ty: Type): Boolean =
    TypesUtil.isArrows(ty).asInstanceOf[Boolean]
  
  /**
   * Performs the given substitution on the body type.
   */
  def substituteTypesForInferenceVars(substitution: Map[_InferenceVarType, Type],
                                      body: Type): Type = {
   
    object substitutionWalker extends Walker {
      override def walk(node: Any): Any = node match {
        case ty:_InferenceVarType => substitution.get(ty).getOrElse(super.walk(ty))
        case _ => super.walk(node)
      }
    }
    
    // Perform the substitution on the body type.
    substitutionWalker(body).asInstanceOf[Type]
  }
  
  /**
   * Returns the type of the static parameter's bound if it is a type parameter.
   */
  def staticParamBoundType(sparam: StaticParam): Option[Type] =
    sparam.getKind match {
      case SKindType(_) => Some(NodeFactory.makeIntersectionType(sparam.getExtendsClause))
      case _ => None
    }
  
  /**
   * Given a static parameters, returns a static arg containing a fresh
   * inference variable.
   */
  def makeInferenceArg(sparam: StaticParam): StaticArg = sparam.getKind match {
    case SKindType(_) => {
      // Create a new inference var type.
      val t = NodeFactory.make_InferenceVarType(NodeUtil.getSpan(sparam))
      NodeFactory.makeTypeArg(NodeFactory.makeSpan(t), t)
    }
    case SKindInt(_) => NI.nyi()
    case SKindBool(_) => NI.nyi()
    case SKindDim(_) => NI.nyi()
    case SKindOp(_) => NI.nyi()
    case SKindUnit(_) => NI.nyi()
    case SKindNat(_) => NI.nyi()
    case _ => bug("unexpected kind of static parameter")
  }
  
  /**
   * Returns a list of conjuncts of the given type. If given an intersection
   * type, this is the set of the constituents. If ANY, this is empty. If some
   * other type, this is the singleton set of that type.
   */
  def conjuncts(ty: Type): Set[Type] = ty match {
    case _:AnyType => Set.empty[Type]
    case SIntersectionType(_, elts) => Set(elts:_*).flatMap(conjuncts)
    case _ => Set(ty)
  }
}
