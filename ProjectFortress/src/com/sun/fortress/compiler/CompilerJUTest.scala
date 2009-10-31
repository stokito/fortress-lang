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
package com.sun.fortress.compiler

import junit.framework.TestCase
import junit.framework.TestSuite
import edu.rice.cs.plt.tuple.Option

import com.sun.fortress.Shell
import com.sun.fortress.compiler.{NamingCzar => JavaNamingCzar}
import com.sun.fortress.compiler.phases.PhaseOrder
import com.sun.fortress.exceptions.ProgramError
import com.sun.fortress.exceptions.StaticError
import com.sun.fortress.exceptions.WrappedException
import com.sun.fortress.exceptions.shell.UserError
import com.sun.fortress.nodes_util.ASTIO
import com.sun.fortress.nodes_util.NodeFactory
import com.sun.fortress.nodes_util.NodeUtil
import com.sun.fortress.repository.ProjectProperties
import com.sun.fortress.useful.Path
import com.sun.fortress.useful.TestCaseWrapper
import com.sun.fortress.useful.WireTappedPrintStream

/**
 * Tests for compiler error message quality.
 * These tests deliberately call the compiler just as it would be called by a user.
 * Error messages are read via stderr.
 * Even the ordering of multiple error messages is tested, to ensure deterministic compilation results.
 */
class CompilerJUTest() extends TestCaseWrapper {

  def compilerResult():String = {
    val args = new Array[String](1)
    args(0) = "compile"
    val stderr = WireTappedPrintStream.make(System.err)
    System.setErr(stderr)
    Shell.main(true, args) // Call main in test mode (indicated by boolean arg)
    stderr.getString()
  }

  def testCompileNothing() = {
    val expected = "The compile command must be given a file.\n"
    val result = compilerResult()
    assert(expected.equals(result))
  }

}
