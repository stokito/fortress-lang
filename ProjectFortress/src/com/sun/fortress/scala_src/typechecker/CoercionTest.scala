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

import com.sun.fortress.exceptions.StaticError
import com.sun.fortress.scala_src.types.TypeAnalyzer
import com.sun.fortress.scala_src.useful.Lists._


/**
 * Tests for compiler error message quality.
 * These tests deliberately call the compiler just as it would be called by a user.
 * Error messages are read via stderr.
 * Even the ordering of multiple error messages is tested, to ensure deterministic compilation results.
 */
class CoercionTest(analyzer:TypeAnalyzer) {
  def run() = {
    toJavaList(List[StaticError]())
  }
}
