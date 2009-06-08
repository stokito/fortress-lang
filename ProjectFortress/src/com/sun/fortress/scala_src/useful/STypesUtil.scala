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
import com.sun.fortress.scala_src.typechecker.ScalaConstraint
import com.sun.fortress.scala_src.typechecker.ScalaConstraintUtil._
import com.sun.fortress.scala_src.typechecker.STypeChecker
import com.sun.fortress.scala_src.useful.Lists._
import com.sun.fortress.scala_src.useful.Options._
import com.sun.fortress.useful.HasAt
import com.sun.fortress.useful.NI

import scala.collection.immutable.HashMap

object STypesUtil {

  /**
   * A function type that takes two types and returns a boolean.
   */
  type Subtype = (Type, Type) => Boolean
  
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
              hook.isSubtype(argType, bound, arg,
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
  private def insertStaticParams(typ: Type, sparams: List[StaticParam]): Type = {
    
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
   * Identical to the overloading but with an explicitly given list of static
   * parameters.
   */
  def staticInstantiation(sargs: List[StaticArg],
                          sparams: List[StaticParam],
                          body: Type,
                          hook: STypeChecker#Hook): Option[Type] = {
    
    // Check that the args match.
    if (!staticArgsMatchStaticParams(sargs, sparams, hook)) return None
    
    // Create mapping from parameter names to static args.
    val paramMap = HashMap(sparams.map(_.getName).zip(sargs): _*)
    
    // Gets the actual value out of a static arg.
    def sargToVal(sarg: StaticArg): Node = sarg match {
      case sarg:TypeArg => sarg.getTypeArg
      case sarg:IntArg => sarg.getIntVal
      case sarg:BoolArg => sarg.getBoolArg
      case sarg:OpArg => sarg.getName
      case sarg:DimArg => sarg.getDimArg
      case sarg:UnitArg => sarg.getUnitArg
      case _ => bug("unexpected kind of static arg")
    }
    
    // Replaces all the params with args in a node.
    object staticReplacer extends Walker {
      override def walk(node: Any): Any = node match {
        case n:VarType => paramMap.get(n.getName).map(sargToVal).getOrElse(n)
        // TODO: Check proper name for OpArgs.
        case n:OpArg => paramMap.get(n.getName.getOriginalName).getOrElse(n)
        case n:IntRef => paramMap.get(n.getName).map(sargToVal).getOrElse(n)
        case n:BoolRef => paramMap.get(n.getName).map(sargToVal).getOrElse(n)
        case n:DimRef => paramMap.get(n.getName).map(sargToVal).getOrElse(n)
        case n:UnitRef => paramMap.get(n.getName).map(sargToVal).getOrElse(n)
        case _ => super.walk(node)
      }
    }
    
    // Get the replaced type and clear out its static params, if any.
    Some(clearStaticParams(staticReplacer(body).asInstanceOf[Type]))
  }
  
  /**
   * Instantiates a generic type with some static arguments. The static
   * parameters are retrieved from the body type and replaced inside body with
   * their corresponding static arguments. In the end, any static parameters
   * in the replaced type will be cleared.
   * 
   * @param args A list of static arguments to apply to the generic type body.
   * @param body The generic type whose static parameters are to be replaced.
   * @param hook A typechecker hook for checking subtypes and reporting errors.
   * @return An option of a type identical to body but with every occurrence of
   *         one of its declared static parameters replaced by corresponding
   *         static args. If None, then the instantiation failed.
   */
  def staticInstantiation(sargs: List[StaticArg],
                          body: Type,
                          hook: STypeChecker#Hook): Option[Type] =
    staticInstantiation(sargs, getStaticParams(body), body, hook)
  
  def checkApplicable(fnType: ArrowType,
                      argType: Type,
                      expectedType: Option[Type],
                      hook: STypeChecker#Hook): Option[(ArrowType, List[StaticArg])] = {
    val sparams = getStaticParams(fnType)
    
    // Substitute inference variables for static parameters in fnType.
    
    // 1. build substitution S = [T_i -> $T_i]
    // 2. instantiate fnType with S to get an arrow type with inf vars, infArrow
    val sargs = sparams.map(makeInferenceArg)
    val infArrow = staticInstantiation(sargs, sparams, fnType, hook).
      getOrElse(return None).asInstanceOf[ArrowType]
   
    // 3. argType <:? dom(infArrow) yields a constraint, C1
    val domainConstraint = hook.checkSubtype(argType, infArrow.getDomain)
    
    // 4. if expectedType given, C := C1 AND range(infArrow) <:? expectedType
    val rangeConstraint = expectedType.map(
      t => hook.checkSubtype(infArrow.getRange, t)).getOrElse(TRUE_FORMULA)
    val constraint = domainConstraint.scalaAnd(rangeConstraint, hook.isSubtype)
    
    // Get an inference variable type out of a static arg.
    def staticArgType(sarg: StaticArg): Option[_InferenceVarType] = sarg match {
      case sarg:TypeArg => Some(sarg.getTypeArg.asInstanceOf)
      case _ => None
    }
    
    // 5. build bounds map B = [$T_i -> S(UB(T_i))]
    val infVars = sargs.flatMap(staticArgType)
    val sparamBounds = sparams.flatMap(staticParamBoundType).
      map(t => insertStaticParams(t, sparams))
    val boundsMap = HashMap(infVars.zip(sparamBounds): _*)
    
    // 6. solve C to yield a substitution S' = [$T_i -> U_i]
    val subst = constraint.scalaSolve(boundsMap).getOrElse(return None)
    
    // 7. instantiate infArrow with [U_i] to get resultArrow
    val resultArrow = substituteTypesForInferenceVars(subst, infArrow).
      asInstanceOf[ArrowType]
    
    // 8. return (resultArrow,StaticArgs([U_i])) 
    val resultArgs = infVars.map((t) => 
      NodeFactory.makeTypeArg(resultArrow.getInfo.getSpan, subst.apply(t)))
    Some((resultArrow,resultArgs))
  }
  
  /**
   * Return the statically most applicable arrow type along with the static args
   * that instantiated that arrow type.
   */
  def staticallyMostApplicableArrow(fnType: Type,
                                    argType: Type,
                                    expectedType: Option[Type],
                                    hook: STypeChecker#Hook):
                                      Option[(ArrowType, List[StaticArg])] = {
    
    // Get a list of arrow types from the fnType.
    val allArrows = conjuncts(fnType)
    
    // Filter applicable arrows and their instantiated args.
    val arrowsAndInstantiations = allArrows.
      flatMap((ty) => checkApplicable(ty.asInstanceOf[ArrowType], argType, expectedType, hook))
    
    // Define an ordering relation on arrows with their instantiations.
    def lessThan(overloading1: (ArrowType, List[StaticArg]),
                 overloading2: (ArrowType, List[StaticArg])): Boolean = {
      
      val SArrowType(_, domain1, range1, _) = overloading1
      val SArrowType(_, domain2, range2, _) = overloading2
      
      if (hook.equivalentTypes(domain1, domain2)) false
      else hook.isSubtype(domain1, domain2)
    }
    
    // Sort the arrows and instantiations to find the statically most
    // applicable. Return None if none were applicable.
    arrowsAndInstantiations.sort(lessThan).firstOption
  }
  
  /**
   * Returns the type of the static parameter's bound if it is a type parameter.
   */
  private def staticParamBoundType(sparam: StaticParam): Option[Type] =
    sparam.getKind match {
      case SKindType(_) => Some(NodeFactory.makeIntersectionType(sparam.getExtendsClause))
      case _ => None
    }
  
  /**
   * Given a static parameters, returns a static arg containing a fresh
   * inference variable.
   */
  private def makeInferenceArg(sparam: StaticParam): StaticArg = sparam.getKind match {
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
   * type, this is a list of the constituents. If ANY, this is empty. If some
   * other type, this is the singleton list of that type.
   */
  def conjuncts(ty: Type): List[Type] = ty match {
    case _:AnyType => Nil
    case SIntersectionType(_, elts) => elts.flatMap(conjuncts)
    case _ => List(ty)
  }
}
