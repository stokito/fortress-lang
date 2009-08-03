/*******************************************************************************
    Copyright 2008 Sun Microsystems, Inc.,
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

package com.sun.fortress.exceptions

import com.sun.fortress.compiler.Types
import com.sun.fortress.compiler.index._
import com.sun.fortress.nodes._
import com.sun.fortress.nodes_util.{NodeFactory => NF}
import com.sun.fortress.nodes_util.{NodeUtil => NU}
import com.sun.fortress.nodes_util.Span
import com.sun.fortress.scala_src.nodes._
import com.sun.fortress.scala_src.useful.Lists._
import com.sun.fortress.scala_src.useful.SExprUtil
import com.sun.fortress.scala_src.useful.STypesUtil._
import edu.rice.cs.plt.iter.IterUtil

import ApplicationError._

/**
 * A type checking exception that occurs when there is a function application
 * that is not well-typed.
 */
class ApplicationError(app: Expr,
                       fnType: Type,
                       argTypes: List[Type],
                       overloadingErrors: List[OverloadingError])
    extends TypeError("", NU.getSpan(app)) {
  
  override def toString = {
    val kind = app match {
      case S_RewriteFnApp(_, f:FunctionalRef, _) =>
        "call to function %s".format(f.getOriginalName)
      case _:_RewriteFnApp =>
        "function application"
      case o:OpExpr =>
        "call to operator %s".format(o.getOp.getOriginalName)
      case f:MethodInvocation =>
        "method invocation %s.%s".format(SExprUtil.getType(f.getObj).toString,
                                         f.getMethod)
      case o:SubscriptExpr =>
        "call to subscript operator %s.%s".format(SExprUtil.getType(o.getObj),
                                                  o.getOp.unwrap)
    }
    val subs = overloadingErrors.map(_.toStringIndentedAll("- "))
    "Could not check %s:\n%s".format(kind, subs)
  }
}

object ApplicationError {

  /**
   * Get the String representation of a signature from a FunctionalRef (for the
   * name and static args) and an arrow type.
   */
  def signatureToString(fn: FunctionalRef, arrow: ArrowType): String = {
    val sb = new StringBuffer
    val sargs = fn.getStaticArgs
    val sparams = toJavaList(getStaticParams(arrow).filter(!_.isLifted))

    // Append the name.
    sb.append(fn.getOriginalName)

    // Append static params, if any.
    if (!sargs.isEmpty)
      sb.append(IterUtil.toString(sargs, "[\\", ", ", "\\]"))
    else if (!sparams.isEmpty)
      sb.append(IterUtil.toString(sparams, "[\\", ", ", "\\]"))

    // Append parameters.
    val domain = arrow.getDomain match {
      case t:TupleType => t.toString
      case t => "(%s)".format(t)
    }
    sb.append(domain)

    // Append return type.
    sb.append(":" + arrow.getRange);
    sb.toString
  }

  /** Format a list of argument types as a single type String. */
  def argTypesToString(argTypes: List[Type]) =
    NF.makeMaybeTupleType(NF.typeSpan, toJavaList(argTypes)).toString

  /** Base class for an exception for a particular overloading. */
  sealed abstract class OverloadingError extends TypeError

  /** This overloading is not applicable to the argument type. */
  case class NotApplicableError(fn: FunctionalRef,
                                arrow: ArrowType,
                                argTypes: List[Type])
      extends OverloadingError {

    override def toString =
      "%s is not applicable to argument of type %s\n".
        format(signatureToString(fn, arrow), argTypesToString(argTypes))
  }

  /** An error relating to the inferred domain type of a FnExpr. */
  case class FnInferenceError(fn: FunctionalRef,
                              arrow: ArrowType,
                              errors: List[FnInferenceErrorKind])
      extends OverloadingError {
    
    override def toString = {
      val subs = errors.map(_.toString+"\n")
      "%s:\n- %s".format(signatureToString(fn, arrow), subs)
    }
  }

  /* Which kind of FnExpr inference error. */
  abstract sealed class FnInferenceErrorKind

  /** Failed to infer the parameter types. */
  case class ParameterError(expr: FnExpr) extends FnInferenceErrorKind {
    override def toString =
      "Could not infer parameter type for function expression %s".
        format(NU.getSpan(expr))
  }

  case class BodyError(expr: FnExpr, error: TypeError)
      extends FnInferenceErrorKind {
    override def toString = {
      val domain = makeDomainType(toList(expr.getHeader.getParams)).get
      val base = "Inferred parameter type %s for function expression %s and got error".
         format(domain, NU.getSpan(expr))
      val subloc = error.location.unwrap match {
        case s:Span => s.toStringWithoutFiles
        case l => l.toString
      }
      "%s:\n- %s: %s".format(base, subloc, error.descriptionIndented("  - "))
    }
  }
}

