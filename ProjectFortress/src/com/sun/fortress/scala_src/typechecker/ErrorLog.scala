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
package com.sun.fortress.scala_src.typechecker

import com.sun.fortress.nodes._
import com.sun.fortress.useful.HasAt
import com.sun.fortress.scala_src.nodes._
import com.sun.fortress.exceptions.StaticError
import com.sun.fortress.exceptions.TypeError

class ErrorLog() {
  var errors = List[StaticError]()

  def signal(msg:String,hasAt:HasAt) = {
    errors = errors ::: List(TypeError.make(msg,hasAt))
  }
}