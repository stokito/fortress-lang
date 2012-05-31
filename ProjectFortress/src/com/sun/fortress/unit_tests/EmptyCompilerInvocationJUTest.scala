/*******************************************************************************
    Copyright 2008,2009, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

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
 * Test compiler when invoked without enough args.
 * This can't be done by CompilerJUTests.
 */
class EmptyCompilerInvocationJUTest() extends TestCaseWrapper {

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
