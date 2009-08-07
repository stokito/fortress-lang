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
 * Stores the error and then throws it as an exception. Error messages should be printed with
 * nested spacing so that any errors from the tryCheck that are actually reported will be nested
 * inside an outer error from the type checker.
 */
class TryErrorLog extends ErrorLog {
  override def signal(error: StaticError) = {
    super.signal(error)
    throw error
  }
}
