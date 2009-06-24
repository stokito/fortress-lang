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

import _root_.java.util.Arrays
import _root_.java.util.LinkedList
import junit.framework.TestCase
import junit.framework.TestSuite
import edu.rice.cs.plt.tuple.Option

import com.sun.fortress.Shell
import com.sun.fortress.compiler.{NamingCzar => JavaNamingCzar}
import com.sun.fortress.compiler.typechecker.TypeAnalyzer
import com.sun.fortress.exceptions.ProgramError
import com.sun.fortress.exceptions.StaticError
import com.sun.fortress.exceptions.WrappedException
import com.sun.fortress.exceptions.shell.UserError
import com.sun.fortress.nodes_util.ASTIO
import com.sun.fortress.nodes_util.NodeFactory
import com.sun.fortress.nodes_util.NodeUtil
import com.sun.fortress.repository.ProjectProperties
import com.sun.fortress.scala_src.useful.ErrorLog
import com.sun.fortress.useful.Path
import com.sun.fortress.useful.TestCaseWrapper
import com.sun.fortress.useful.WireTappedPrintStream


/**
 * Tests for compiler error message quality.
 * These tests deliberately call the compiler just as it would be called by a user.
 * Error messages are read via stderr.
 * Even the ordering of multiple error messages is tested, to ensure deterministic compilation results.
 */
class CoercionTest(analyzer:TypeAnalyzer, exclusionOracle:ExclusionOracle) {
  def run() = {
    val factory = new CoercionOracleFactory(analyzer.traitTable, analyzer,
                                            exclusionOracle, exclusionOracle.errors)
    val oracle = factory.makeOracle(analyzer.typeEnv)
    factory.getErrors
  }
}
