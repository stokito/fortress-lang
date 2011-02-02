/*******************************************************************************
    Copyright 2009, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

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
