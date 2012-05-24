/*******************************************************************************
    Copyright 2009,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.exceptions

import com.sun.fortress.compiler.Types
import com.sun.fortress.compiler.index._
import com.sun.fortress.nodes._
import com.sun.fortress.nodes_util.{NodeFactory => NF}
import com.sun.fortress.nodes_util.{NodeUtil => NU}
import com.sun.fortress.nodes_util.Span
import com.sun.fortress.scala_src.nodes._
import com.sun.fortress.scala_src.useful.Lists._
import com.sun.fortress.scala_src.useful.Options._
import com.sun.fortress.scala_src.useful.SExprUtil._
import com.sun.fortress.scala_src.useful.STypesUtil._
import edu.rice.cs.plt.iter.IterUtil

/**
 * A type checking exception that occurs when there is a function application
 * that is not well-typed. The optional receiver type is necessary because
 * the application node won't have been checked, so we won't be able
 * to get the receiver type out of it.
 */
class ApplicationError(app: Expr,
                       overloadingErrors: List[OverloadingError],
                       recvrType: Option[Type],
                       isOverloaded: Boolean)
    extends TypeError("", NU.getSpan(app)) {

  // Overriding description because toString includes this and the location.
  override def description = {
    val kind = app match {
      case S_RewriteFnApp(_, f:FunctionalRef, _) =>
        "call to function %s".format(f.getOriginalName)
      case _:_RewriteFnApp =>
        "function application"
      case o:OpExpr =>
        "call to operator %s".format(o.getOp.getOriginalName)
      case f:MethodInvocation =>
        "method invocation %s.%s".format(recvrType.get, f.getMethod)
      case o:SubscriptExpr =>
        "call to subscript operator %s.%s".format(recvrType.get, o.getOp.unwrap)
    }
    val sortedSubErrors = overloadingErrors.sortWith((x,y) => x.compareTo(y) < 0)
    val substrs = sortedSubErrors.map(_.toString())
    "Could not check %s\n%s".
      format(kind, sortedSubErrors.map("    - " + _.toStringIndented("      ")).mkString("\n"))
  }

  override def toString = {
    if (isOverloaded || overloadingErrors.exists(! _.isInstanceOf[FnInferenceError])) {
      super.toString
    } else {
      overloadingErrors.
          flatMap(_.asInstanceOf[FnInferenceError].errors.map(_.toString)).mkString("\n")
    }
  }
}

/** Application failed because there is no method with that name. */
class NoSuchMethod(app: Expr, recvrType: Type)
    extends ApplicationError(app: Expr, Nil, Some(recvrType), true) {

  override def description = app match {
    case f:MethodInvocation =>
      "No such method %s.%s.".format(recvrType, f.getMethod)
    case o:SubscriptExpr =>
      "No such subscript operator %s.%s.".format(recvrType, o.getOp.unwrap)
  }
}

/**
 * Created once for an entire application, an instance of this class is used
 * for creating the constituent overloading errors.
 */
class ApplicationErrorFactory(val app: Expr, val recvrType: Option[Type], isOverloaded: Boolean) {

  /** Create the top level ApplicationError for this application. */
  def makeApplicationError(errors: List[OverloadingError]): TypeError =
    new ApplicationError(app, errors, recvrType, isOverloaded)

  /** Create a NotApplicableError for this application. */
  def makeNotApplicableError(arrow: ArrowType,
                             args: List[Either[Expr, FnExpr]]) = {

    // Get all the arg types, using UnknownType for FnExpr unknowns.
    val argTypes = args.map {
      case Left(checked) => getType(checked).get
      case Right(unchecked) =>

        // Replace the domain (and range if not given too) with unknown types.
        val SArrowType(info, _, _, effect, io, methodInfo) = arrow
        val domain = Types.UNKNOWN
        val range = toOption(NU.getReturnType(unchecked)).getOrElse(Types.UNKNOWN)
        SArrowType(info, domain, range, effect, io, methodInfo)
    }
    val sargs = getStaticArgsFromApp(app).getOrElse(Nil)
    NotApplicableError(arrow, sargs, argTypes)
  }

  def makeNoContextError(arrow: ArrowType, infSargs: List[StaticArg]) = {
    // Gather up the static params that correspond to uninferred static args.
    val missing = (infSargs zip getStaticParams(arrow)) flatMap {
      case (STypeArg(_, false, _:_InferenceVarType), sparam) => Some(sparam)
      case _ => None
    }
    NoContextError(arrow, missing)
  }

  /** Create a FnInferenceError for this application. */
  def makeFnInferenceError(arrow: ArrowType,
                           errors: List[FnInferenceErrorKind]) = {
    val sargs = getStaticArgsFromApp(app).getOrElse(Nil)
    FnInferenceError(arrow, sargs, errors)
  }

  /** Create a ParameterError with the given FnExpr. */
  def makeParameterError(expr: FnExpr) = ParameterError(expr)

  /** Create a BodyError with the given FnExpr and static error. */
  def makeBodyError(expr: FnExpr, domain: Type, error: StaticError) =
    BodyError(expr, domain, error)
}

/** Base class for an exception for a particular overloading. */
sealed abstract class OverloadingError extends TypeError

/** This overloading is not applicable to the argument type. */
case class NotApplicableError(arrow: ArrowType,
                              sargs: List[StaticArg],
                              argTypes: List[Type])
    extends OverloadingError {

  override def toString =
    "%s is not applicable to %s %s.".
      format(OverloadingError.getSignature(sargs, arrow),
             if (argTypes.exists(hasUnknownType))
               "any type of the form"
             else
               "an argument of type",
             OverloadingError.argTypesToString(argTypes))
}

/** We could not infer all the static arguments of this overloading. */
case class NoContextError(arrow: ArrowType,
                          sparams: List[StaticParam])
    extends OverloadingError {

  override def toString = {
    val missingStr = sparams.mkString(", ")
    val plural = if (sparams.length == 1) "" else "s"
    "Could not infer static argument%s %s without context.".format(plural, missingStr)
  }
}

/** An error relating to the inferred domain type of a FnExpr. */
case class FnInferenceError(arrow: ArrowType,
                            sargs: List[StaticArg],
                            errors: List[FnInferenceErrorKind])
    extends OverloadingError {

  override def toString = {
    val sortedErrors = errors.sortWith((x,y) => x.toString < y.toString)
    val subs = sortedErrors.map(e => "%s".format(e.toString)).mkString("\n")
    "%s:\n%s".format(OverloadingError.getSignature(sargs, arrow), subs)
  }
}

/* Which kind of FnExpr inference error. */
abstract sealed class FnInferenceErrorKind

/** Failed to infer the parameter types. */
case class ParameterError(expr: FnExpr) extends FnInferenceErrorKind {
  override def toString =
    NU.getSpan(expr) + ":\n    Could not infer parameter type for function expression."
}

/** Inferred parameter types, but encountered error in body. */
case class BodyError(expr: FnExpr, domain: Type, error: StaticError)
    extends FnInferenceErrorKind {
  // This used to strip location information and embed it at the end
  // of an error message.  THIS IS WRONG.  DON'T EVER DO IT!!!!!!!!!
  // There's a reason location information is put at the *beginning*
  // by toString---so that you (and your tools) will have a prayer of
  // seeing it.
  override def toString : String = {
    val params = toListFromImmutable(NU.getParams(expr))
    if (params.forall(_.getIdType.isSome)) {
        return error.toString
    }
    val paramsPretty =
      if (params.length > 1) {
        "(%s)".format(params.map(_.toString).mkString(", "))
      } else {
        params(0).toString
      }
    "%s\n    %s:\n      Possibly because %s: %s after argument inference".
        format(error.toString, NU.getSpan(expr).toString, paramsPretty, domain)
  }
}

/** Contains static methods. */
object OverloadingError {

  /**
   * Get the String representation of a signature from the static args and the
   * arrow type.
   */
  def getSignature(sargs: List[StaticArg], arrow: ArrowType): String = {
    val sb = new StringBuilder
    val sparams = toJavaList(getStaticParams(arrow).filter(!_.isLifted))

    // Append static params, if any.
    if (!sargs.isEmpty)
      sb.append(IterUtil.toString(toJavaList(sargs), "[\\", ", ", "\\]"))
    else if (!sparams.isEmpty)
      sb.append(IterUtil.toString(sparams, "[\\", ", ", "\\]"))

    // Format it as an arrow.
    sb.append(arrow.toString)
    sb.toString
  }

  /** Format a list of argument types as a single type String. */
  def argTypesToString(argTypes: List[Type]) =
    NF.makeMaybeTupleType(NF.typeSpan, toJavaList(argTypes)).toString
}

/**
 * Creates useless errors. This should be used only if the errors resulting
 * from application checking are not needed.
 */
object DummyApplicationErrorFactory extends ApplicationErrorFactory(null, null, false) {

  /** Create a dummy ApplicationError.*/
  override def makeApplicationError(errors: List[OverloadingError]) =
    new TypeError("(dummy application error)", NF.typeSpan)

}
