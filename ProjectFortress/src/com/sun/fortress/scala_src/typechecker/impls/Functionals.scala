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

package com.sun.fortress.scala_src.typechecker.impls

import com.sun.fortress.compiler.index.Method
import com.sun.fortress.exceptions.StaticError.errorMsg
import com.sun.fortress.nodes._
import com.sun.fortress.nodes_util.NodeFactory
import com.sun.fortress.nodes_util.NodeUtil
import com.sun.fortress.scala_src.typechecker._
import com.sun.fortress.scala_src.typechecker.ScalaConstraintUtil._
import com.sun.fortress.scala_src.nodes._
import com.sun.fortress.scala_src.useful.Lists._
import com.sun.fortress.scala_src.useful.Options._
import com.sun.fortress.scala_src.useful.Sets._
import com.sun.fortress.scala_src.useful.SExprUtil._
import com.sun.fortress.scala_src.useful.STypesUtil._
import com.sun.fortress.useful.NI

/**
 * Provides the implementation of cases relating to functionals and functional
 * application.
 * 
 * This trait must be mixed in with an `STypeChecker with Common` instance
 * in order to provide the full type checker implementation.
 * 
 * (The self-type annotation at the beginning declares that this trait must be
 * mixed into STypeChecker along with the Common helpers. This is what
 * allows this trait to implement abstract members of STypeChecker and to
 * access its protected members.)
 */
trait Functionals { self: STypeChecker with Common =>

  // ---------------------------------------------------------------------------
  // HELPER METHODS ------------------------------------------------------------

  /**
   * Given a type, which could be a VarType, Intersection or Union, return the TraitTypes
   * that this type could be used as for the purposes of calling methods and fields.
   */
  protected def traitTypesCallable(typ: Type): Set[TraitType] = typ match {
    case t:TraitType => Set(t)

    // Combine all the trait types callable from constituents.
    case typ:IntersectionType =>
      conjuncts(typ).filter(NodeUtil.isTraitType).flatMap(traitTypesCallable)

    // Get the trait types callable from the upper bounds of this parameter.
    case SVarType(_, name, _) => toOption(env.staticParam(name)) match {
      case Some(s@SStaticParam(_, _, ts, _, _, SKindType(_))) =>
        Set(ts:_*).filter(NodeUtil.isTraitType).flatMap(traitTypesCallable)
      case _ => Set.empty[TraitType]
    }

    case SUnionType(_, ts) =>
      signal(typ, errorMsg("You should be able to call methods on this type,",
                           "but this is not yet implemented."))
      Set.empty[TraitType]

    case _ => Set.empty[TraitType]
  }

  /**
   * Not yet implemented.
   * Waiting for _RewriteFnApp to be implemented.
   */
  protected def findMethodsInTraitHierarchy(methodName: IdOrOpOrAnonymousName,
                                            receiverType: Type)
                                            : Set[Method] = {

    val traitTypes = traitTypesCallable(receiverType)
    val ttAsWheres = traitTypes.map(NodeFactory.makeTraitTypeWhere)
    val allMethods = inheritedMethods(ttAsWheres.toList)
    toSet(allMethods.matchFirst(methodName))
  }
  
  /**
   * Determines if the given overloading is dynamically applicable.
   */
  protected def isDynamicallyApplicable(overloading: Overloading,
                                        smaArrow: ArrowType,
                                        inferredStaticArgs: List[StaticArg])
                                        : Option[Overloading] = {
    // Is this arrow type applicable.
    def arrowTypeIsApplicable(overloadingType: ArrowType): Option[Type] = {
      val typ =
        // If static args given, then instantiate the overloading first.
        if (inferredStaticArgs.isEmpty)
          overloadingType
        else
          staticInstantiation(inferredStaticArgs, overloadingType).
            getOrElse(return None).asInstanceOf[ArrowType]

      if (isSubtype(typ.getDomain, smaArrow.getDomain))
        Some(typ)
      else
        None
    }

    // If overloading type is an intersection, check that any of its
    // constituents is applicable.
    val applicableArrows = conjuncts(toOption(overloading.getType).get).
      map(_.asInstanceOf[ArrowType]).
      flatMap(arrowTypeIsApplicable)

    val overloadingType = applicableArrows.toList match {
      case Nil => return None
      case t::Nil => t
      case _ => NodeFactory.makeIntersectionType(applicableArrows)
    }
    Some(SOverloading(overloading.getInfo,
                      overloading.getUnambiguousName,
                      Some(overloadingType)))
  }

  /**
   * Calls the other overloading with the conjuncts of the given function type.
   */
  protected def staticallyMostApplicableArrow(fnType: Type,
                                              argType: Type,
                                              expectedType: Option[Type])
                                              : Option[(ArrowType, List[StaticArg])] = {

    val arrows = conjuncts(fnType).toList.map(_.asInstanceOf[ArrowType])
    staticallyMostApplicableArrow(arrows, argType, expectedType)
  }

  /**
   * Return the statically most applicable arrow type along with the static args
   * that instantiated that arrow type. This method assumes that all the arrow
   * types in fnType have already been instantiated if any static args were
   * supplied.
   */
  protected def staticallyMostApplicableArrow(allArrows: List[ArrowType],
                                              argType: Type,
                                              expectedType: Option[Type])
                                              : Option[(ArrowType, List[StaticArg])] = {

    // Filter applicable arrows and their instantiated args.
    val arrowsAndInstantiations =
      allArrows.flatMap(ty => checkApplicable(ty.asInstanceOf[ArrowType],
                                              argType,
                                              expectedType))

    // Define an ordering relation on arrows with their instantiations.
    def lessThan(overloading1: (ArrowType, List[StaticArg]),
                 overloading2: (ArrowType, List[StaticArg])): Boolean = {

      val SArrowType(_, domain1, range1, _, _) = overloading1._1
      val SArrowType(_, domain2, range2, _, _) = overloading2._1

      if (equivalentTypes(domain1, domain2)) false
      else isSubtype(domain1, domain2)
    }

    // Sort the arrows and instantiations to find the statically most
    // applicable. Return None if none were applicable.
    arrowsAndInstantiations.sort(lessThan).firstOption
  }

  /**
   * Checks whether an arrow type if applicable to the given args. If so, then
   * the [possible instantiated] arrow type along with any inferred statics args
   * are returned.
   */
  protected def checkApplicable(fnType: ArrowType,
                                argType: Type,
                                expectedType: Option[Type])
                                : Option[(ArrowType, List[StaticArg])] = {
                                  
    val sparams = getStaticParams(fnType)

    // Substitute inference variables for static parameters in fnType.

    // 1. build substitution S = [T_i -> $T_i]
    // 2. instantiate fnType with S to get an arrow type with inf vars, infArrow
    val sargs = sparams.map(makeInferenceArg)
    val infArrow = staticInstantiation(sargs, sparams, fnType).
      getOrElse(return None).asInstanceOf[ArrowType]

    // 3. argType <:? dom(infArrow) yields a constraint, C1
    val domainConstraint = checkSubtype(argType, infArrow.getDomain)

    // 4. if expectedType given, C := C1 AND range(infArrow) <:? expectedType
    val rangeConstraint = expectedType.map(
      t => checkSubtype(infArrow.getRange, t)).getOrElse(TRUE_FORMULA)
    val constraint = domainConstraint.scalaAnd(rangeConstraint, isSubtype)

    // Get an inference variable type out of a static arg.
    def staticArgType(sarg: StaticArg): Option[_InferenceVarType] = sarg match {
      case sarg:TypeArg => Some(sarg.getTypeArg.asInstanceOf)
      case _ => None
    }

    // 5. build bounds map B = [$T_i -> S(UB(T_i))]
    val infVars = sargs.flatMap(staticArgType)
    val sparamBounds = sparams.flatMap(staticParamBoundType).
      map(t => insertStaticParams(t, sparams))
    val boundsMap = Map(infVars.zip(sparamBounds): _*)

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
   * Given an applicand, the statically most applicable arrow type for it,
   * and the static args from the application, return the applicand updated
   * with the dynamically applicable overloadings, arrow type, and static args.
   */
  protected def rewriteApplicand(fn: Expr,
                                 arrow: ArrowType,
                                 sargs: List[StaticArg]): Expr = fn match {
    case fn: FunctionalRef =>

      // Get the dynamically applicable overloadings.
      val overloadings =
        toList(fn.getNewOverloadings).
        flatMap(o => isDynamicallyApplicable(o, arrow, sargs))

      // Add in the filtered overloadings, the inferred static args,
      // and the statically most applicable arrow to the fn.
      addType(
        addStaticArgs(
          addOverloadings(fn, overloadings), sargs), arrow)

    case _ if !sargs.isEmpty =>
      NI.nyi("No place to put inferred static args in application.")

    // Just add the arrow type if the applicand is not a FunctionalRef.
    case _ => addType(fn, arrow)
  }

  /**
   * Signal a static error for an application for which there were no applicable
   * functions.
   */
  protected def noApplicableFunctions(application: Expr,
                                      fn: Expr,
                                      fnType: Type,
                                      argType: Type) = {
    val kind = fn match {
      case _:FnRef => "function"
      case _:OpRef => "operator"
      case _ => ""
    }
    val argTypeStr = normalize(argType) match {
      case tt:TupleType => tt.getElements.toString
      case _ => "[" + argType.toString + "]"
    }
    val message = fn match {
      case fn:FunctionalRef =>
        val name = fn.getOriginalName
        val sargs = fn.getStaticArgs
        if (sargs.isEmpty)
          "Call to %s %s has invalid arguments, %s".
            format(kind, name, argTypeStr)
        else
          "Call to %s %s with static arguments %s has invalid arguments, %s".
            format(kind, name, sargs, argTypeStr)
      case _ =>
        "Expression of type %s is not applicable to argument type %s.".
          format(normalize(fnType), argTypeStr)
      }
      signal(application, message)
    }

  // ---------------------------------------------------------------------------
  // CHECK IMPLEMENTATION ------------------------------------------------------
  
  def checkFunctionals(node: Node): Node = node match {

    case SOverloading(info, name, _) => {
      val checkedName = check(name).asInstanceOf[IdOrOp]
      getTypeFromName(checkedName) match {
        case Some(checkedType) =>
          SOverloading(info, checkedName, Some(checkedType))
        case None => node
      }
    }

    case _ => throw new Error(errorMsg("not yet implemented: ", node.getClass))
  }
  
  // ---------------------------------------------------------------------------
  // CHECKEXPR IMPLEMENTATION --------------------------------------------------

  def checkExprFunctionals(expr: Expr,
                           expected: Option[Type]): Expr = expr match {

    case SSubscriptExpr(SExprInfo(span, paren, _), obj, subs, op, sargs) => {
      val checkedObj = checkExpr(obj)
      val checkedSubs = subs.map(checkExpr)
      val objType = getType(checkedObj).getOrElse(return expr)

      // Convert sub types into a single type or tuple of types.
      if (!haveTypes(checkedSubs)) return expr
      val subsType = checkedSubs.map(s => getType(s).get) match {
        case t :: Nil => t
        case t =>
          NodeFactory.makeTupleType(NodeUtil.getSpan(expr), toJavaList(t))
      }

      // Get the methods and arrows from the op.
      val methods = findMethodsInTraitHierarchy(op.get, objType)
      val arrows =
        if (sargs.isEmpty) methods.map(getArrowFromMethod)
        else methods.
               flatMap(m => staticInstantiation(sargs, getArrowFromMethod(m))).
               map(_.asInstanceOf[ArrowType])

      staticallyMostApplicableArrow(arrows.toList, subsType, None) match {
        case Some((arrow, sargs)) =>
          SSubscriptExpr(SExprInfo(span, paren, Some(arrow.getRange)),
                         checkedObj,
                         checkedSubs,
                         op,
                         sargs)
        case one =>
          signal(expr, "Receiver type %s does not have applicable overloading of %s for argument type %s.".
                         format(objType, op.get, subsType))
          expr
      }
    }

    case fn@SFunctionalRef(_, sargs, _, name, _, _, overloadings, _) => {
      // Note that ExprDisambiguator inserts the static args from a
      // FunctionalRef into each of its Overloadings.

      // Check all the overloadings and filter out any that have the wrong
      // number or kind of static parameters.
      def rewriteOverloading(o: Overloading): Option[Overloading] = check(o) match {
        case  SOverloading(info, name, Some(ty)) =>
          staticInstantiation(sargs, ty).map(t => SOverloading(info,name,Some(t)))
        case _ => None
      }
      val checkedOverloadings = overloadings.flatMap(rewriteOverloading)

      if (checkedOverloadings.isEmpty)
        signal(expr, errorMsg("Wrong number or kind of static arguments for function: ",
                              name))

      // Make the intersection type of all the overloadings.
      val overloadingTypes = checkedOverloadings.map(_.getType.unwrap)
      val intersectionType =
        NodeFactory.makeIntersectionType(NodeUtil.getSpan(fn),
                                         toJavaList(overloadingTypes))
      addType(addOverloadings(fn, checkedOverloadings), intersectionType)
    }

    case S_RewriteFnApp(SExprInfo(span, paren, optType), fn, arg) => {
      val checkedFn = checkExpr(fn)
      val checkedArg = checkExpr(arg)

      // Check fn and arg and get their types.
      (getType(checkedFn), getType(checkedArg)) match {
        case (Some(fnType), Some(_)) if !isArrows(fnType) =>
          signal(expr, errorMsg("Applicand has a type that is not an arrow: ",
                                normalize(fnType)))
          expr
        case (Some(fnType), Some(argType)) =>

          staticallyMostApplicableArrow(fnType, argType, None) match {
            case Some((smostApp, sargs)) =>

              // Rewrite the applicand to include the arrow and static args
              // and update the application.
              val newFn = rewriteApplicand(checkedFn, smostApp, sargs)
              S_RewriteFnApp(SExprInfo(span, paren, Some(smostApp.getRange)), newFn, checkedArg)

            case None =>
              noApplicableFunctions(expr, checkedFn, fnType, argType)
              expr
        }

        case _ => expr
      }
    }

    case SOpExpr(info, fn, args) => {
      val checkedOp = checkExpr(fn)
      val checkedArgs = args.map(checkExpr)
      val opType = getType(checkedOp).getOrElse(return expr)
      if (!haveTypes(checkedArgs)) return expr
      val argType =
        NodeFactory.makeTupleType(info.getSpan,
                                  toJavaList(checkedArgs.map(t => getType(t).get)))
      staticallyMostApplicableArrow(opType, argType, None) match {
        case Some((smostApp, sargs)) =>
          val newOp = rewriteApplicand(checkedOp,smostApp,sargs).asInstanceOf[OpRef]
          addType(SOpExpr(info, newOp, checkedArgs),smostApp.getRange)

        case None =>
          noApplicableFunctions(expr, checkedOp, opType, argType)
          expr
      }

    }
    
    case _ => throw new Error(errorMsg("Not yet implemented: ", expr.getClass))
  }  
}
