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

import com.sun.fortress.exceptions.StaticError.errorMsg
import com.sun.fortress.nodes._
import com.sun.fortress.nodes_util.{NodeFactory => NF}
import com.sun.fortress.nodes_util.{NodeUtil => NU}
import com.sun.fortress.scala_src.nodes._
import com.sun.fortress.scala_src.useful.Lists._
import com.sun.fortress.scala_src.useful.Options._
import com.sun.fortress.scala_src.useful.SExprUtil._
import com.sun.fortress.scala_src.useful.STypesUtil._
import fortress.useful.NI

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
      case _ => NF.makeIntersectionType(applicableArrows)
    }
    Some(SOverloading(overloading.getInfo,
                      overloading.getUnambiguousName,
                      Some(overloadingType)))
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

    
  //TODO: Should be rewritten to a method invocation since there is so much duplication  
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
        case t => NF.makeTupleType(NU.getSpan(expr), toJavaList(t))
      }

      // Get the methods and arrows from the op.
      val methods = findMethodsInTraitHierarchy(op.get, objType)
      var arrows = methods.flatMap(makeArrowFromFunctional)
      //Make sure all of the functions had return types declared or inferred
      //TODO: This could be handled more gracefully
      if(arrows.size != methods.size){
        signal(expr, "The return type for %s could not be inferred".format(op))
        return expr
      }
      //Instantiate the arrows if you were given static args
      if (!sargs.isEmpty) {
        arrows = arrows.flatMap(a =>staticInstantiation(sargs, a).map(_.asInstanceOf[ArrowType]))
      }
      staticallyMostApplicableArrow(arrows.toList, subsType, None) match {
        case Some((arrow, sargs)) =>
          SSubscriptExpr(SExprInfo(span, paren, Some(arrow.getRange)),
                         checkedObj,
                         checkedSubs,
                         op,
                         sargs)
        case None =>
          signal(expr, "Receiver type %s does not have applicable overloading of %s for argument type %s.".
                         format(objType, op.get, subsType))
          expr
      }
    }
    
    case SMethodInvocation(SExprInfo(span, paren, _), obj, method, sargs, arg, _) =>{
      val checkedObj = checkExpr(obj)
      val checkedArg = checkExpr(arg)
      val recvrType = getType(checkedObj).getOrElse(return expr)
      val argType = getType(checkedArg).getOrElse(return expr)
      val methods = findMethodsInTraitHierarchy(method, recvrType)
      var arrows = methods.flatMap(makeArrowFromFunctional)
      if(arrows.size!=methods.size){
        signal(expr, "The return type for %s could not be inferred".format(method))
        return expr
      }
      if(!sargs.isEmpty){
        arrows = arrows.flatMap(a =>staticInstantiation(sargs, a).map(_.asInstanceOf[ArrowType]))
      }
      staticallyMostApplicableArrow(arrows.toList, argType, None) match {
        case Some((arrow, sargs)) =>
          SMethodInvocation(SExprInfo(span, paren, Some(arrow.getRange)),
                            checkedObj,
                            method,
                            sargs,
                            checkedArg,
                            Some(arrow))
        case None =>
          signal(expr, "Receiver type %s does not have applicable overloading of %s for argument type %s.".
                         format(recvrType, method, argType))
          expr
      }
    }

    case fn@SFunctionalRef(_, sargs, _, name, _, _, overloadings, _) => {
      // Note that ExprDisambiguator inserts the static args from a
      // FunctionalRef into each of its Overloadings.

      // Check all the overloadings and filter out any that have the wrong
      // number or kind of static parameters.
      var hadNoType = false
      def rewriteOverloading(o: Overloading): Option[Overloading] = check(o) match {
        case ov@SOverloading(_, _, Some(ty)) if sargs.isEmpty => Some(ov)
        case SOverloading(info, name, Some(ty)) =>
          staticInstantiation(sargs, getStaticParams(ty), ty, true).
            map(t => SOverloading(info, name, Some(t)))
        case _ => hadNoType = true; None
      }
      val checkedOverloadings = overloadings.flatMap(rewriteOverloading)

      // If there are no overloadings, we cannot continue type checking. If any
      // of the overloadings failed to get a type
      if (checkedOverloadings.isEmpty) {
        if (!hadNoType) {
          signal(expr, errorMsg("Wrong number or kind of static arguments for function: ",
                                name))
        }
        return expr
      }

      // Make the intersection type of all the overloadings.
      val overloadingTypes = checkedOverloadings.map(_.getType.unwrap)
      val intersectionType =
        NF.makeIntersectionType(NU.getSpan(fn),
                                toJavaList(overloadingTypes))
      addType(addOverloadings(fn, checkedOverloadings), intersectionType)
    }

    case S_RewriteFnApp(SExprInfo(span, paren, _), fn, arg) => {
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
        NF.makeTupleType(info.getSpan,
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

    case SFnExpr(SExprInfo(span, paren, _), header, body) => {
      val params = toList(header.getParams)
      if (params.exists(p => p.getIdType.isEmpty)) {
        NI.nyi("Cannot check FnExpr without explicit parameter types.")
      }
      val checkedBody = this.extend(params).checkExpr(body)
      val domain = makeArgumentType(params.map(_.getIdType.unwrap))
      val range = getType(checkedBody).getOrElse(return expr)
      val arrow = NF.makeArrowType(span, domain, range)
      SFnExpr(SExprInfo(span, paren, Some(arrow)), header, checkedBody)
    }
    
    case _ => throw new Error(errorMsg("Not yet implemented: ", expr.getClass))
  }  
}
