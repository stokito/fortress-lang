/*******************************************************************************
    Copyright 2009,2011, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.scala_src.typechecker.impls

import com.sun.fortress.compiler.Types
import com.sun.fortress.compiler.index._
import com.sun.fortress.exceptions._
import com.sun.fortress.exceptions.InterpreterBug.bug
import com.sun.fortress.exceptions.StaticError.errorMsg
import com.sun.fortress.nodes._
import com.sun.fortress.nodes_util.{ExprFactory => EF}
import com.sun.fortress.nodes_util.{NodeFactory => NF}
import com.sun.fortress.nodes_util.{NodeUtil => NU}
import com.sun.fortress.nodes_util.Span
import com.sun.fortress.scala_src.nodes._
import com.sun.fortress.scala_src.useful.Lists._
import com.sun.fortress.scala_src.useful.Options._
import com.sun.fortress.scala_src.useful.SExprUtil._
import com.sun.fortress.scala_src.useful.SNodeUtil._
import com.sun.fortress.scala_src.useful.STypesUtil._
import com.sun.fortress.scala_src.typechecker._
import com.sun.fortress.exceptions.StaticError
import com.sun.fortress.useful.{HasAt, NI}
import com.sun.fortress.repository.ProjectProperties
import com.sun.fortress.compiler.codegen.FnNameInfo
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
      case ty => if (fn.isInstanceOf[OpRef]) "[" + ty + "]" else ty
    }
    val message = fn match {
      case fn:FunctionalRef =>
        val name = fn.getOriginalName
        val sargs = fn.getStaticArgs
        if (sargs.isEmpty)
          "Call to %s %s has invalid arguments, %s".format(kind, name, argTypeStr)
        else
          "Call to %s %s with static arguments %s has invalid arguments, %s".format(kind, name, sargs, argTypeStr)
      case _ =>
        "Expression of type %s is not applicable to argument type %s.".format(normalize(fnType), argTypeStr)
      }
      signal(application, message)
    }

  /** Given a single argument expr, break it into a list of args. */
  def getArgList(arg: Expr): List[Expr] = arg match {
    case STupleExpr(_, exprs, _, _, _) => exprs
    case _:VoidLiteralExpr => Nil
    case _ => List(arg)
  }


  /**
   * Given a list of arguments, partition it into a list of eithers, where Left is checked and Right
   * is unchecked.
   */
  def partitionArgs(args: List[Expr]): Option[List[Either[Expr, FnExpr]]] = {
    val partitioned = args.map(checkExprIfCheckable)
    if (partitioned.exists(_.fold(getType(_).isNone, x => false)))
      None
    else
      Some(partitioned)
  }

  /**
   * Get the full argument type from the partitioned list of args, filling in with the bottom
   * arrow.
   */
  def getArgType(args: List[Either[Expr, FnExpr]]): Type = getArgType(args, NF.typeSpan)

  /** Same as other but provides a location for the span on the new type. */
  def getArgType(args: List[Either[Expr, FnExpr]], span: Span): Type = {
    val argTypes = args.map {
      case Left(e) => getType(e).get
      case Right(_) => Types.BOTTOM_ARROW
    }
    NF.makeMaybeTupleType(span, toJavaList(argTypes))
  }

  /**
   * Given an arrow type, an expected type context, and a list of partitioned args (where Left is
   * checked and Right is an unchecked arg (usually an anonymous function)), determine if the arrow is applicable to these args.
   * This method will infer static arguments on the arrow and parameter types on any FnExpr args.
   * The result is an AppCandidate, which contains the inferred arrow type, the list of static
   * args that were inferred, and the checked arguments.
   */
  def checkApplicable(preCandidate: PreAppCandidate,
                      context: Option[Type],
                      args: List[Either[Expr, FnExpr]],
                      mOpName: Option[Op])
                     (implicit errorFactory: ApplicationErrorFactory)
                      : Either[AppCandidate, OverloadingError] = {
    val arrow = preCandidate.arrow

    // Make sure all uncheckable args correspond to arrow type params.
    zipWithDomain(args, arrow.getDomain).foreach {
      case (Right(_), pt) if !possiblyArrows(pt, getStaticParams(arrow)) =>
        return Right(errorFactory.makeNotApplicableError(arrow, args))
      case _ =>
    }

    // Infer lifted static params.
    val argType = getArgType(args)
    /* If we are checking an operator we need to get the name of the overloading in order
     * to check if it is a parametric operator
     */
    val mOpNames = mOpName match {
      case Some(opName) => 
        // If we are checking an operator the overloading in preCandidate should be some
        val PreAppCandidate(_, Some(ovName), _) = preCandidate // DRC breakpoint
        Some((opName, ovName.getOriginalName.asInstanceOf[Op]))
      case None => None
    }
    // The op parameter corresponding to a parametric operator must be lifted
    val (liftedArrow, liftedSargs) =
      inferLiftedStaticParams(arrow, argType, mOpNames).getOrElse{
        return Right(errorFactory.makeNotApplicableError(arrow, args))
      }

    // Are there static params? i.e., do we need more inference?
    val candidateOrError =
      if (hasStaticParams(liftedArrow))
        checkApplicableWithInference(liftedArrow, preCandidate, context, args)
      else
        checkApplicableWithoutInference(liftedArrow, preCandidate, args)

    // If applicable, then inject the lifted static args into the result.
    candidateOrError.left.map { c =>
      AppCandidate(c.arrow, liftedSargs ++ c.sargs, c.args, c.overloading, c.fnl)
    }
  }

  /**
   * Check that the arrow type is applicable to these args with static argument
   * inference and no coercion.
   */
  def checkApplicableWithInference(
          arrow: ArrowType,
          preCandidate: PreAppCandidate,
          context: Option[Type],
          args: List[Either[Expr, FnExpr]])
          (implicit errorFactory: ApplicationErrorFactory)
           : Either[AppCandidate, OverloadingError] = {

    val originalArrow = preCandidate.arrow

    // Try to check the unchecked args, constructing a new list of
    // (checked, unchecked) arg pairs.
    def updateArgs(argsAndParam: (Either[Expr, FnExpr], Type))
                     : (Either[Expr, FnExpr], Option[BodyError]) = argsAndParam match {

      // This arg is checkable if there are no more inference vars
      // in the param type (which must be an arrow).
      case (Right(unchecked), paramType:ArrowType) =>
        if (hasInferenceVars(paramType.getDomain))
          (Right(unchecked), None)
        else {

          // Try to check the arg given this new expected type.
          inferFnExprParams(unchecked, paramType, false) match {
            // Move this arg out of unchecked and into checked.
            case Left(checked) => (Left(checked), None)
            // This arg might be checkable later, so keep going.
            case Right(bodyError) => (Right(unchecked), Some(bodyError))
          }

        }

      // If the parameter type is not an arrow, skip it.
      case (Right(unchecked), _) => (Right(unchecked), None)

      // Skip args that are already checked.
      case (Left(checked), _) => (Left(checked), None)
    }

    // Update all args until we have checked as much as possible.
    def recurOnArgs(args: List[Either[Expr, FnExpr]])
                    : Either[AppCandidate, OverloadingError] = {

      // Build the single type for all the args, inserting the least arrow
      // type for any that aren't checkable.
      val argType = getArgType(args)

      // Do type inference to get the inferred static args.
      val (resultArrow, sargs) =
        inferStaticParams(arrow, argType, context).getOrElse {
          return Right(errorFactory.makeNotApplicableError(originalArrow, args))
        }

      // Match up checked/unchecked args and param types and try to check the unchecked args,
      // constructing a new list of (checked, unchecked) arg pairs.
      val newArgsAndErrors = zipWithDomain(args, resultArrow.getDomain).
                               map(updateArgs)
      val (newArgs, maybeErrors) = (newArgsAndErrors).unzip

      // If progress was made, keep going. Otherwise return.
      if (newArgs.count(_.isRight) < args.count(_.isRight))
        recurOnArgs(newArgs)
      else {

        // If not all args were checked, gather the errors.
        if (newArgs.count(_.isRight) != 0) {
          return Right(makeOverloadingError(originalArrow,
                                            resultArrow.getDomain,
                                            newArgsAndErrors))
        }

        // If there are inference variables left, inform the user that there
        // wasn't enough context.
        if (hasInferenceVars(resultArrow)) {
          return Right(errorFactory.makeNoContextError(originalArrow, sargs))
        }

        // We've reached a fixed point and all args are checked!
        Left(AppCandidate(resultArrow,
                          sargs,
                          newArgs.map(_.left.get),
                          preCandidate.overloading,
                          preCandidate.fnl))
      }
    }

    // Do the recursion to check the args.
    recurOnArgs(args)
  }

  /**
   * Check that the arrow type is applicable to these args without any static
   * argument inference. The resulting args may have coercions.
   */
  def checkApplicableWithoutInference(
          arrow: ArrowType,
          preCandidate: PreAppCandidate,
          args: List[Either[Expr, FnExpr]])
          (implicit errorFactory: ApplicationErrorFactory)
           : Either[AppCandidate, OverloadingError] = {
    
    val originalArrow = preCandidate.arrow

    // Gather up either-args and option-errors.
    val newArgsAndErrors = zipWithDomain(args, arrow.getDomain).map {

      // If `checked` is a subtype of the param type, use it. Otherwise, try to
      // build a coercion to the param type and use that. If it does not coerce
      // to the param type, this is a NotApplicableError.
      case (Left(checked), paramType) =>
        if (isSubtype(getType(checked).get, paramType))
          (Left(checked), None)
        else coercions.buildCoercion(checked, paramType) match {
          case Some(coercion) => (Left(coercion), None)
          case None =>
            return Right(errorFactory.makeNotApplicableError(originalArrow, args))
        }

      // For each unchecked FnExpr arg, try to infer its parameter type and
      // check it. We don't need to add coercions because arrow types don't have them.
      case (Right(unchecked), paramType:ArrowType) =>
        inferFnExprParams(unchecked, paramType, true).
          fold(checked => (Left(checked), None),
               err => (Right(unchecked), Some(err)))

      // Unchecked FnExpr arg with a non-arrow parameter type -- error.
      case (Right(unchecked), _) =>
        return Right(errorFactory.makeNotApplicableError(originalArrow, args))
    }

    // Make sure that all the args were checked. If any remain unchecked, gather
    // up all the inference errors into an overloading error.
    val (newArgs, maybeErrors) = arrow.getDomain match {
          case _:AnyType => (args, None)
          case _ => (newArgsAndErrors).unzip
        }
    if (newArgs.count(_.isRight) != 0) {
      Right(makeOverloadingError(originalArrow,
                                 arrow.getDomain,
                                 newArgsAndErrors))
    } else {

      // We need to make sure there are the right number of args for the domain.
      // Now that coercions are in place, we can simply check if the new arg
      // type is a subtype of the domain type.
      val argType = getArgType(newArgs)
      if (isSubtype(argType,arrow.getDomain) && (newArgs.size == args.size))
        Left(AppCandidate(arrow,
                          Nil,
                          newArgs.map(_.left.get),
                          preCandidate.overloading,
                          preCandidate.fnl))
      else
        Right(errorFactory.makeNotApplicableError(originalArrow, args))
    }
  }

  /**
   * Try to infer the parameter type on the given unchecked FnExpr. This will
   * check the expression, then return either the checked expression (possibly
   * applying a coercion) or a BodyError on failure.
   */
  def inferFnExprParams(unchecked: FnExpr,
                        paramType: ArrowType,
                        doCoercion: Boolean)
                       (implicit errorFactory: ApplicationErrorFactory)
                        : Either[Expr, BodyError] = {

    // Try to check the arg given this new expected type.
    val domain = paramType.getDomain
    val expectedArrow = NF.makeArrowType(NU.getSpan(paramType),
                                         domain,
                                         Types.ANY)
    val tryChecker = STypeCheckerFactory.makeTryChecker(this)
    val result : Option[Expr] =
      if (doCoercion)
        // Coercion is applied since we are passing in a type.
        tryChecker.tryCheckExpr(unchecked, expectedArrow)
      else
        // Coercion is NOT applied since we are passing in an option type.
        tryChecker.tryCheckExpr(unchecked, Some(expectedArrow))

    result match {
      case Some(checked) => Left(checked)
      case None =>
        Right(errorFactory.makeBodyError(unchecked,
                                         domain,
                                         tryChecker.getError.get))
    }
  }

  /**
   * Make an overloading error that collects up errors from uncheckable
   * arguments, or makes a NotApplicableError.
   */
  def makeOverloadingError(originalArrow: ArrowType,
                           infDomain: Type,
                           newArgsAndErrors: List[(Either[Expr, FnExpr], Option[BodyError])])
                          (implicit errorFactory: ApplicationErrorFactory)
                           : OverloadingError = {
    // For each unchecked arg, get its body error if it had one. If it did
    // not but the parameter type was an arrow, it is a parameter
    // inference error. Otherwise it is a not applicable error.
    val argsErrorsParams = zipWithDomain(newArgsAndErrors, infDomain)
    val fnErrors = argsErrorsParams flatMap {
      case ((Right(_), Some(bodyError)), _) => Some(bodyError)

      // If the parameter type is an inference variable or an arrow, then a
      // parameter could have been inferred but wasn't.
      case ((Right(unchecked), None), _:_InferenceVarType) =>
        Some(errorFactory.makeParameterError(unchecked))
      case ((Right(unchecked), None), _:ArrowType) =>
        Some(errorFactory.makeParameterError(unchecked))

      // If the parameter type was not one of those, then there is no FnExpr
      // parameter that could have made this applicable.
      case ((Right(_), None), _) =>
        return errorFactory.makeNotApplicableError(originalArrow, newArgsAndErrors.map(_._1))

      case ((Left(_), _), _) => None
    }
    errorFactory.makeFnInferenceError(originalArrow, fnErrors)
  }

  /**
   * Type check the application of the given arrow candidates to the given arg.
   * This returns the statically applicable candidates (with their corresponding
   * inferred static args and updated arguments) with the most specific one at
   * the head. The resulting AppCandidates will contain the single arg as a
   * singleton list.
   */
  def checkApplication(preCandidates: List[PreAppCandidate],
                       arg: Expr,
                       context: Option[Type])
                      (implicit errorFactory: ApplicationErrorFactory)
                       : Option[List[AppCandidate]] = {

    // Check the application using the args extrapolated from arg.
    val args = getArgList(arg)
    checkApplication(preCandidates, args, context) map { candidates =>

      // Combine the separated args back into a single arg in the resulting app
      // candidate.
      candidates.map(_.mergeArgs(NU.getSpan(arg)))
    }

  }

  /**
   * Type check the application of the given arrow candidates to the given args.
   * This returns the statically applicable candidates (with their corresponding
   * inferred static args and updated arguments) with the most specific one at
   * the head.
   */
  def checkApplication(preCandidates: List[PreAppCandidate],
                       iargs: List[Expr],
                       context: Option[Type],
                       mOpName: Option[Op] = None)
                      (implicit errorFactory: ApplicationErrorFactory)
                       : Option[List[AppCandidate]] = {

    // Check all the checkable args and make sure they all have types.
    val args = partitionArgs(iargs).getOrElse(return None)

    // Filter the overloadings that are applicable.
    val es = preCandidates.map(pc => checkApplicable(pc, context, args, mOpName))
    val (candidates, overloadingErrors) =
        (for (Left(x) <- es) yield x, for (Right(x) <- es) yield x)

    // If there were no candidates, report errors.
    if (candidates.isEmpty) {
      errors.signal(errorFactory.makeApplicationError(overloadingErrors))
      return None
    }

    // Sort the arrows and instantiations to find the statically most
    // applicable. Then update each candidate's Overloading node.
    val sorted = Some(candidates.sortWith(moreSpecificCandidate))
    // ensure that head is actually more specific.
    
    sorted
  }

  /**
   * Given a receiver type and a method name, return the list of all the arrow types for each
   * method overloading. Ignores any arrows that were for getters or setters.
   */
  def getCandidatesForMethod(recvrType: Type,
                             name: IdOrOp,
                             sargs: List[StaticArg],
                             loc: HasAt): Option[List[PreAppCandidate]] = {
    def noGetterSetter(m: Method): Option[Method] = m match {
      case g:FieldGetterMethod => None
      case s:FieldSetterMethod => None
      case m => Some(m)
    }
    val methods = findMethodsInTraitHierarchy(name, recvrType).toList.flatMap(noGetterSetter)
    var arrowsAndSchemataAndMethods = methods.flatMap{ m =>
      (makeArrowFromFunctional(m), makeArrowFromFunctional(m.originalMethod)) match {
        case (Some(s), Some(t)) => Some((s,t,m))
        case _ => None
      }
    }
    // Make sure all of the functions had return types declared or inferred
    // TODO: This could be handled more gracefully
    if (arrowsAndSchemataAndMethods.size != methods.size) {
      signal(loc, "The return type for %s could not be inferred".format(name))
      return None
    }

    // Instantiate the arrows if you were given static args
    if (!sargs.isEmpty) {
      arrowsAndSchemataAndMethods = arrowsAndSchemataAndMethods.
        flatMap(a => staticInstantiationForApp(sargs, a._1).
            map(x => (x.asInstanceOf[ArrowType],a._2, a._3)))
    }

    // Methods have no Overloading nodes.
    Some(arrowsAndSchemataAndMethods.map(a => PreAppCandidate(a._1, 
        Some(SOverloading(a._1.getInfo, name, name, Some(a._1), Some(a._2))), Some(a._3))))
  }

  /**
   * Given an applicand, return the list of all arrow type candidates for each
   * overloading.
   */
  def getCandidatesForFunction(fn: Expr, loc: HasAt): Option[List[PreAppCandidate]] = {
    val fnType = getType(fn).getOrElse(return None)
    if (!isArrows(fnType)) {
      signal(loc, "Applicand has a type that is not an arrow: %s".format(normalize(fnType)))
      return None
    }
    Some(fn match {
      case f:FunctionalRef =>
        toListFromImmutable(f.getNewOverloadings).map { ov =>
          // The fn has already been type checked, so each overloading has an
          // arrow type.
          val arrow = ov.getType.get.asInstanceOf[ArrowType]
          PreAppCandidate(arrow, Some(ov), None) //DRC break here, None is wrong
        }
      case _ =>
        conjuncts(fnType).toList.map { t =>
          val arrow = t.asInstanceOf[ArrowType]
          PreAppCandidate(arrow, None, None) //DRC break here, None is wrong
        }
    })
  }

  // ---------------------------------------------------------------------------
  // CHECK IMPLEMENTATION ------------------------------------------------------

  def checkFunctionals(node: Node): Node = node match {

    case SOverloading(info, unambigName, origName, _, _) => {
      val checkedName = check(unambigName).asInstanceOf[IdOrOp]

      // Should have one arrow type for this unambiguous name.
      getTypeFromName(checkedName) match {
        case Some(arrow: ArrowType) =>
          SOverloading(info, checkedName, origName, Some(arrow), Some(arrow))
        case Some(typ) =>
          bug("type env binds unambiguous name %s to non-arrow type %s".format(unambigName, typ))
        case None => node
      }
    }

    case _ => throw new Error(errorMsg("not yet implemented: ", node.getClass))
  }

  // ---------------------------------------------------------------------------
  // CHECKEXPR IMPLEMENTATION --------------------------------------------------

  def checkExprFunctionals(expr: Expr,
                           expected: Option[Type]): Expr = expr match {

    case SSubscriptExpr(SExprInfo(span, paren, _), obj, subs, Some(op), sargs) => {
      val checkedObj = checkExpr(obj)
      val recvrType = getType(checkedObj).getOrElse(return expr)
      val preCandidates = getCandidatesForMethod(recvrType, op, sargs, expr).getOrElse(return expr)
      if (preCandidates.isEmpty) {
        signal(new NoSuchMethod(expr, recvrType))
        return expr
      }
      implicit val errorFactory =
        new ApplicationErrorFactory(expr,
                                    Some(recvrType),
                                    preCandidates.length > 1)

      // Type check the application to get the checked candidates.
      val candidates =
        checkApplication(preCandidates, subs, expected).getOrElse(return expr)

      // We only care about the most specific one.
      val AppCandidate(bestArrow, bestSargs, bestSubs, _, _), _ = candidates.head
      val newSargs = if (sargs.isEmpty) bestSargs.filter(!_.isLifted) else sargs

      // Rewrite the new expression with its type and checked args.
      SSubscriptExpr(SExprInfo(span, paren, Some(bestArrow.getRange)),
                     checkedObj,
                     bestSubs,
                     Some(op),
                     newSargs)
    }

    case SMethodInvocation(SExprInfo(span, paren, _), obj, method, sargs, arg, _, _) =>{
      val checkedObj = checkExpr(obj)
      val recvrType = getType(checkedObj).getOrElse(return expr)
      val preCandidates = getCandidatesForMethod(recvrType, method, sargs, expr).getOrElse(return expr)
      if (preCandidates.isEmpty) {
        signal(new NoSuchMethod(expr, recvrType))
        return expr
      }
      implicit val errorFactory =
        new ApplicationErrorFactory(expr,
                                    Some(recvrType),
                                    preCandidates.length > 1)

      // Type check the application.
      val candidates =
        checkApplication(preCandidates, arg, expected).getOrElse(return expr)

      // We only care about the most specific one. We know the args pattern
      // match succeeds because all app candidates generated for method
      // invocations include only a single arg.
      val AppCandidate(bestArrow, bestSargs, List(bestArg), Some(bestOver), bestFnl) = candidates.head
      val newSargs = if (sargs.isEmpty) bestSargs.filter(!_.isLifted) else sargs

      if (bestFnl.isNone) {
         signal(expr, errorMsg("Method Invocation best Fnl is none"))
      }
      val modifiedSchema = Some(new FnNameInfo(bestFnl.get.asInstanceOf[DeclaredMethod].originalMethod, null).normalizedSchema(bestOver.getSchema.get));

      // Rewrite the new expression with its type and checked args.
      SMethodInvocation(SExprInfo(span, paren, Some(bestArrow.getRange)),
                        checkedObj,
                        method,
                        newSargs,
                        bestArg,
                        Some(bestArrow),
                        modifiedSchema)
    }

    case fn@SFunctionalRef(_, sargs, _, name, _, _, overloadings, _, _) => {
      // Error if this is a getter
      val thisEnv = getRealName(name, toListFromImmutable(current.ast.getImports)) match {
        case id@SIdOrOpOrAnonymousName(_, Some(api)) => getEnvFromApi(api)
        case _ => env
      }
      thisEnv.getMods(name) match {
        case Some(mods) =>
          if (mods.isGetter) {
            signal(expr,
                   errorMsg("Getter " + name + " must be called with the field reference syntax."))
            return expr
          }
        case _ =>
      }

      // Note that ExprDisambiguator inserts the explicit static args from a
      // FunctionalRef into each of its Overloadings.

      // Check all the overloadings and filter out any that have the wrong
      // number or kind of static parameters.
      var hadNoType = false
      def rewriteOverloading(o: Overloading): Option[Overloading] = {
        o match {
        case ov@SOverloading(_, _, _, Some(ty), _) if sargs.isEmpty => Some(ov)

        case SOverloading(info, name, origName, Some(ty), schema) =>
          staticInstantiation(sargs, ty).map { t =>
            SOverloading(info, name, origName, Some(t.asInstanceOf[ArrowType]), schema)
          }

        case _ => hadNoType = true; None
      }}
      def checkOverloading(o: Overloading): Option[Overloading] = Some(check(o).asInstanceOf[Overloading])
      //def checkOverloading(o: Overloading): Overloading = check(o).asInstanceOf[Overloading]

      val justcheckedOverloadings = overloadings.flatMap(checkOverloading)
      val checkedOverloadings = justcheckedOverloadings.flatMap(rewriteOverloading)

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
      val intersectionType = NF.makeMaybeIntersectionType(toJavaList(overloadingTypes))
      // val intersectionType = analyzer.meet(overloadingTypes)
      
      addType(addOverloadings(fn, checkedOverloadings, false), intersectionType)
    }

    case app @ S_RewriteFnApp(SExprInfo(span, paren, _), fn, arg) => {
      val checkedFn = checkExpr(fn)
      val preCandidates = getCandidatesForFunction(checkedFn, expr).getOrElse(return expr)
      implicit val errorFactory =
        new ApplicationErrorFactory(expr,
                                    None,
                                    preCandidates.length > 1)

      // Type check the application.
      val candidates =
        checkApplication(preCandidates, arg, expected).getOrElse(return expr)

      // We know the arg pattern match succeeds because all app candidates
      // generated for functions include a single arg.
      // is method not None?
      val AppCandidate(bestArrow, bestSargs, List(bestArg),
                       opt_overloading, fnl) = candidates.head
      // Believe that fnl will be useless here, must do lookup instead below

      // Rewrite the applicand to include the arrow and unlifted static args
      // and update the application.
      val newFn = rewriteApplicand(checkedFn, candidates, false)
      val info = SExprInfo(span, paren, Some(bestArrow.getRange))

      newFn match {
        // Detect FnApp that is really method application with implicit self,
        // and rewrite it to a MethodInvocation.  We do this *after* typechecking
        // to work around a certain amount of bogosity in the treatment of self
        // in object expressions [self refers to the intersection of supertypes,
        // and therefore doesn't include any locally-defined methods unless they
        // implement or override superclass methods].
        // TODO: is method overloading going to cause this pattern match to fail?
      case SFnRef(exprInfo@
              SExprInfo(span, paren,
                      Some(SArrowType(typeInfo, dom, rng, effect, io,
                              Some(mi@SMethodInfo(selfType, selfPos))))),
                              staticArgs, _, origName: Id,
                              names, iOverloadings, newOverloadings, overloadingType, overloadingSchema) if selfPos == -1 =>
        val selfRef = checkExpr(EF.makeVarRef(span, "self"))
        if (opt_overloading.isSome()) {
            val ov = opt_overloading.get.getUnambiguousName()
            val xxx = env.lookup(ov).get.fnIndices
            if (ProjectProperties.DEBUG_METHOD_TAGGING) 
               System.err.println(xxx);
            if (xxx.size != 1) {
                signal(expr, "Lookup for " + ov + " for method invocation failed");
            }
        } else {
            if (ProjectProperties.DEBUG_METHOD_TAGGING) 
                System.err.println("No overloading seen for " + expr)
            signal(expr, "No overloading for for method invocation ");
        }

        val declaredMethod = env.lookup(opt_overloading.get.getUnambiguousName()).get.fnIndices.head
        // Not happy about passing null for defaultApi, but don't think it is necessary for schema normalization
        val modifiedSchema = Some(new FnNameInfo(declaredMethod, null).normalizedSchema(overloadingSchema.get));
        
        val res : MethodInvocation =
          SMethodInvocation(info, selfRef, origName, staticArgs, bestArg,
                  overloadingType, modifiedSchema)
                  // System.err.println(span+": app of "+checkedFn+
                  //                    "\n  selfType="+selfType+
                  //                    "\n  self: "+getType(selfRef)+
                  //                    "\n  REWRITTEN TO: "+res.toStringReadable())
                  res
      case _ =>
          S_RewriteFnApp(info, newFn, bestArg)
      }
    }

    case app @ SOpExpr(SExprInfo(span, paren, _), op, args) => {
      val checkedOp = checkExpr(op).asInstanceOf[FunctionalRef]
      val preCandidates = getCandidatesForFunction(checkedOp, expr).getOrElse(return expr)
      val opName = checkedOp.getOriginalName.asInstanceOf[Op]
      implicit val errorFactory = new ApplicationErrorFactory(expr, None, preCandidates.length > 1)

      // Type check the application.
      val candidates = checkApplication(preCandidates, args, expected, Some(opName)).
                         getOrElse(return expr)
      val AppCandidate(bestArrow, bestSargs, bestArgs, _, _) = candidates.head

      // Rewrite the applicand to include the arrow and static args
      // and update the application.
      val newOp = rewriteApplicand(checkedOp, candidates, false).asInstanceOf[FunctionalRef]
      SOpExpr(SExprInfo(span, paren, Some(bestArrow.getRange)), newOp, bestArgs)
    }

    case SFnExpr(SExprInfo(span, paren, _),
                 SFnHeader(a, b, c, d, e, f, tempParams, declaredRetType), body) => {
      // If expecting an arrow type, use its domain to infer param types.
      val (params, expectedRetType) = expected match {
        case Some(arrow:ArrowType) =>
          (addParamTypes(arrow.getDomain, tempParams).getOrElse(tempParams), Some(arrow.getRange))
        case _ => (tempParams, None)
      }

      val bodyType = declaredRetType.orElse(expectedRetType)

      // Make sure all params have a type.
      val domain = makeDomainType(params).getOrElse {
        signal(expr, "Could not determine all parameter types of function expression.")
        return expr
      }

      val checkedBody = bodyType match {
        // If there is a declared return type, use it.
        case Some(typ) =>
          this.extend(params).checkExpr(body,
                                         typ,
                                         errorString("Function body",
                                                     "declared return"))
        case None =>
          this.extend(params).checkExpr(body)
      }

      val range = declaredRetType.getOrElse(getType(checkedBody).getOrElse(return expr))
      val arrow = NF.makeArrowType(span, domain, range)
      val newHeader = SFnHeader(a, b, c, d, e, f, params, Some(range))
      SFnExpr(SExprInfo(span, paren, Some(arrow)), newHeader, checkedBody)
    }

    case SCaseExpr(SExprInfo(span, paren, _), param, compare, equalsOp, inOp,
                   clauses, elseClause) => {
      var newClauses =
          clauses.map(c => c match {
                      case SCaseClause(info, matchE, body, op) =>
                        SCaseClause(info, checkExpr(matchE),
                                    checkExpr(body).asInstanceOf[Block],
                                    op.map(checkExpr).asInstanceOf[Option[FunctionalRef]])})
      var checkedExprs =
          newClauses.flatMap(c => c match {
                             case SCaseClause(_,m,b,Some(o)) => List(m,b,o)
                             case SCaseClause(_,m,b,None) => List(m,b)})
      def handleExpr(e: Expr) = {
        val newE = checkExpr(e)
        checkedExprs ::= newE
        newE
      }
      val newParam = param.map(handleExpr)
      val newCompare = compare.map(handleExpr).asInstanceOf[Option[FunctionalRef]]
      val newEquals = handleExpr(equalsOp).asInstanceOf[FunctionalRef]
      val newIn = handleExpr(inOp).asInstanceOf[FunctionalRef]
      val newElse = elseClause.map(handleExpr).asInstanceOf[Option[Block]]
      // Check that subexpressions all typechecked properly
      if (!haveTypes(checkedExprs)) return expr
      var body_types: List[Type] = newClauses.map(c => getType(c.getBody).get)
      newParam match {
        // Handle regular (non-extremum) case expressions
        case Some(p) =>
          // During inference, we'll try to apply the given compare op
          // (if there is one) and otherwise try equals and in.
          def checkCaseClause(c: CaseClause): CaseClause = c match {
            case SCaseClause(info@SASTNodeInfo(span), matchE, block, _) =>
              val args = List(p, c.getMatchClause)
              def checkOp(op: FunctionalRef): FunctionalRef = {
                val preCandidates = getCandidatesForFunction(op, expr).getOrElse(return op)

                // Type check the application without reporting an error.
                implicit val errorFactory = DummyApplicationErrorFactory
                val checker = STypeCheckerFactory.makeDummyChecker(this)
                val candidates =
                  checker.checkApplication(preCandidates, args, Some(Types.BOOLEAN))
                         .getOrElse(return op)

                // Rewrite the applicand to include the arrow and static args
                // and update the application.
                rewriteApplicand(op, candidates, false).asInstanceOf[FunctionalRef]
              }
              newCompare match {
                // If compare is some, we use that operator
                case Some(op) => SCaseClause(info, matchE, block, Some(checkOp(op)))
                case None =>
                  // Check both = and IN operators
                  // we first want to do <: generator test.
                  // If both are sat, we use =, if only IN is sat, we use IN
                  val isG_match =
                      isSubtype(getType(matchE).get,
                                Types.makeGeneratorZZ32Type(span))
                                // Types.makeGeneratorType(NF.make_InferenceVarType(span)))
                  val isG_cond =
                      isSubtype(getType(p).get,
                                Types.makeGeneratorZZ32Type(span))
                                // Types.makeGeneratorType(NF.make_InferenceVarType(span)))
                  val newOp = if (isG_match && !isG_cond) Some(checkOp(newIn))
                              else Some(checkOp(newEquals))
                  SCaseClause(info, matchE, block, newOp)
              }
          }
          newClauses = newClauses.map(checkCaseClause)
          newElse.foreach(body_types ::= getType(_).get)
        // Extremum expressions: case most < of ... end
        case None =>
          val match_types =
              newClauses.map(c => getType(c.getMatchClause).get)
          val unionTy = self.analyzer.join(match_types)
          val opName = newCompare.get.getOriginalName.asInstanceOf[Op]
          isSubtype(unionTy,
                    Types.makeTotalOperatorOrder(unionTy, opName), expr,
                    "In an extremum expression, the union of all candidate " +
                    "types must be a subtype of TotalOperatorOrder[\\union," +
                    "<,<=,>=,>," + opName + "\\] but it is not.  " +
                    "The union is " + unionTy + ".")
      }
      val newTy = self.analyzer.join(body_types)
      SCaseExpr(SExprInfo(span, paren, Some(newTy)), newParam, newCompare, newEquals,
                newIn, newClauses, newElse)
    }

    case _ => throw new Error(errorMsg("Not yet implemented: ", expr.getClass))
  }

}
