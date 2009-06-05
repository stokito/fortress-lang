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
import com.sun.fortress.scala_src.typechecker.STypeChecker
import com.sun.fortress.scala_src.useful.Lists._
import com.sun.fortress.scala_src.useful.Options._
import com.sun.fortress.useful.HasAt

import scala.collection.immutable.HashMap

object STypesUtil {

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
                                  hook: STypeChecker#Hook): Boolean = {
    if (args.length != params.length) return false
    
    // Match a single pair.
    def argMatchesParam(argAndParam: (StaticArg, StaticParam)): Boolean = {
      val (arg, param) = argAndParam
      (arg, param.getKind) match {
        case (STypeArg(_, argType), SKindType(_)) =>
            toList(param.getExtendsClause).forall((bound:Type) =>
              hook.checkSubtype(argType, bound, arg,
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
  
  /**
   * Performs the given substitution on the body type.
   */
  private def typeSubstitution[K <: Type, V <: Type]
                              (substitution: Map[K, V], body: Type): Type = {
    
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
  
  /**
   * Perform the substitution as described in typeSubstitution.
   */
  def substituteInferenceVarsForVars(substitution: Map[VarType, _InferenceVarType],
                                     body: Type): Type =
    typeSubstitution(substitution, body)
  
  
  def staticInstantiation(args: List[StaticArg],
                          body: Type,
                          hook: STypeChecker#Hook): Option[Type] = {
    val params = getStaticParams(body)
    
    // Check that the args match.
    if (!staticArgsMatchStaticParams(args, params, hook)) return None
    
    // Create map from parameter names to static args.
    val paramMap = HashMap(params.map(_.getName).zip(args): _*)
    
    // Gets the actual value out of a static arg.
    def argToVal(arg: StaticArg): Node = arg match {
      case arg:TypeArg => arg.getTypeArg
      case arg:IntArg => arg.getIntVal
      case arg:BoolArg => arg.getBoolArg
      case arg:OpArg => arg.getName
      case arg:DimArg => arg.getDimArg
      case arg:UnitArg => arg.getUnitArg
      case _ => null    // Cannot occur.
    }
    
    // Replaces all the params with args in a node.
    object staticReplacer extends Walker {
      override def walk(node: Any): Any = node match {
        case n:VarType => paramMap.get(n.getName).map(argToVal).getOrElse(n)
        // TODO: Check proper name for OpArgs.
        case n:OpArg => paramMap.get(n.getName.getOriginalName).getOrElse(n)
        case n:IntRef => paramMap.get(n.getName).map(argToVal).getOrElse(n)
        case n:BoolRef => paramMap.get(n.getName).map(argToVal).getOrElse(n)
        case n:DimRef => paramMap.get(n.getName).map(argToVal).getOrElse(n)
        case n:UnitRef => paramMap.get(n.getName).map(argToVal).getOrElse(n)
      }
    }
    staticReplacer(body).asInstanceOf
  }
  
  def checkApplicable(fnType: ArrowType,
                      argType: Type,
                      expectedType: Option[Type]): Option[(ArrowType, List[StaticArg])] = {
    // Substitute inference variables for static parameters in fnType.
    
    // 1. build substitution S = [T_i -> $T_i]
    // 2. instantiate fnType with S to get an arrow type with inf vars, infArrow
    // 3. build bounds map B = [$T_i -> S(UB(T_i))]
    // 4. argType <:? dom(infArrow) yields a constraint, C
    // 5. if expectedType given, C := C AND range(infArrow) <:? expectedType
    // 6. solve C to yield a substitution S' = [$T_i -> U_i]
    // 7. instantiate fnType with [U_i]
    // 8. return StaticArgs([U_i])
    
    
    
    null
  }
  
  def staticallyMostApplicableArrow(fnType: Type,
                                    argType: Type,
                                    expectedType: Option[Type]): Type = {
    null
  }
}
