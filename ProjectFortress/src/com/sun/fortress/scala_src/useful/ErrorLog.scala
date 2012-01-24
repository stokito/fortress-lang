/*******************************************************************************
Copyright 2009, Oracle and/or its affiliates.
All rights reserved.


Use is subject to license terms.

This distribution may include materials developed by third parties.

 ******************************************************************************/
package com.sun.fortress.scala_src.useful

import com.sun.fortress.useful.HasAt
import com.sun.fortress.scala_src.useful.Lists._
import com.sun.fortress.exceptions.StaticError
import com.sun.fortress.exceptions.TypeError

class ErrorLog() {
  var errors = List[StaticError]()

  def signal(msg: String, hasAt: HasAt): Unit =
    signal(TypeError.make(msg, hasAt))

  def signal(error: StaticError) = {
    errors = error :: errors
  }

  def asList() = {Errors.removeDuplicates(errors)}

  def asJavaList() = {toJavaList(asList())}
}

object Errors {
  def removeDuplicates(errors: List[StaticError]): List[StaticError] = {
    errors match {
      case Nil => errors
      case fst :: rst =>
        if (rst contains fst) {removeDuplicates(rst)}
        else {fst :: removeDuplicates(rst)}
    }
  }
}

/**
 * Stores the error and then throws it as an exception. Error messages should be
 * printed with nested spacing so that any errors from the tryCheck that are
 * actually reported will be nested inside an outer error from the type checker.
 */
class TryErrorLog extends ErrorLog {
  override def signal(error: StaticError) = {
    super.signal(error)
    throw error
  }
}

/** Does not maintain any errors; no throwing, no storing. */
object DummyErrorLog extends ErrorLog {
  override def signal(error: StaticError) = ()
}
