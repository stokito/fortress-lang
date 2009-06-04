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
import com.sun.fortress.nodes._
import com.sun.fortress.scala_src.nodes._
import com.sun.fortress.scala_src.typechecker.ScalaConstraint
import com.sun.fortress.scala_src.useful.Lists._
import com.sun.fortress.scala_src.useful.Options._
import com.sun.fortress.useful.HasAt

object STypesUtil {
  
  /**
   * Substitute statics args for static parameters in the given type. This is
   * useful primarily for a single replacement of this sort; to perform this
   * substitution repeatedly, use an instance of StaticTypeReplacer instead.
   */
  def substituteStaticArgsForParams(args: List[StaticArg],
                                    params: List[StaticParam],
                                    ty: Type): Type = {
    val javaArgs = toJavaList(args)
    val javaParams = toJavaList(params)
    new StaticTypeReplacer(javaParams, javaArgs).replaceIn(ty)
  }
  
  /**
   * Determines if the kinds of the given static args match those of the static
   * parameters. In the case of type arguments, the type is checked to be a
   * subtype of the corresponding type parameter's bounds.
   * 
   * @param checkSubtype A function that is called with a subtype, supertype,
   *                     an error string, and a node to be signaled, and returns
   *                     subtype <:? supertype.
   */
  def staticArgsMatchStaticParams(args: List[StaticArg],
                                  params: List[StaticParam],
                                  checkSubtype: (Type, Type, HasAt, String) => Boolean): Boolean = {
    if (args.length != params.length) return false
    
    // Match a single pair.
    def argMatchesParam(argAndParam: (StaticArg, StaticParam)): Boolean = {
      val (arg, param) = argAndParam
      (arg, param.getKind) match {
        case (STypeArg(_, argType), SKindType(_)) =>
            toList(param.getExtendsClause).forall((bound:Type) =>
              checkSubtype(argType, bound, arg,
                           argType + " not a subtype of " + bound))
        case (SIntArg(_, _), SKindInt(_)) => true
        case (SBoolArg(_, _), SKindBool(_)) => true
        case (SDimArg(_, _), SKindDim(_)) => true
        case (SOpArg(_, _), SKindOp(_)) => true
        case (SUnitArg(_, _), SKindUnit(_)) => true
        case (SIntArg(_, _), SKindNat(_)) => true
        case (_, _) => false
      }
    }
    
    // Match every pair.
    args.zip(params).forall(argMatchesParam(_))
  }
  
  /**
   * Get all the static parameters out of the given type.
   */
  def getStaticParams(typ: Type): List[StaticParam] =
    toList(typ.getInfo.getStaticParams)

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
  
  
  private def typeSubstitution[K <: Type, V <: Type]
                               (substitution: Map[K, V],
                                body: Type): Type = {
    
    // Walks an AST, performing the given substitution. 
    object substitutionWalker extends Walker {
      override def walk(node: Any): Any = node match {
        case ty:K => substitution.get(ty).getOrElse(super.walk(ty))
        case _ => super.walk(node)
      }
    }
    
    // Perform the substitution on the body type.
    substitutionWalker(body).asInstanceOf
  }
  
  
  def betaConversion(substitution: Map[StaticParam, StaticArg],
                      body: Type): Type = {
    null
  }
  
  def checkApplicable(fnType: ArrowType,
                      argType: Type,
                      expectedType: Option[Type]): Option[List[StaticArg]] = {
    // Substitute inference variables for static parameters in fnType.
    
    
    
    null
  }
  
  def staticallyMostApplicableArrow(fnType: Type,
                                    argType: Type,
                                    expectedType: Option[Type]): Type = {
    null
  }
}
