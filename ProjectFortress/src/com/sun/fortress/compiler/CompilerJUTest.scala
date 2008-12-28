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
package com.sun.fortress.compiler

import _root_.java.util.Arrays

import junit.framework.TestSuite

import com.sun.fortress.compiler.phases.PhaseOrder
import com.sun.fortress.exceptions.ProgramError
import com.sun.fortress.Shell
import com.sun.fortress.exceptions.StaticError
import com.sun.fortress.repository.ProjectProperties
import com.sun.fortress.useful.TestCaseWrapper
import com.sun.fortress.nodes_util.NodeUtil
import com.sun.fortress.interpreter.glue.WellKnownNames

import edu.rice.cs.plt.tuple.Option


class CompilerJUTest() extends TestCaseWrapper {

  val STATIC_TESTS_DIR =
    ProjectProperties.BASEDIR + "compiler_tests/"

  def compile(s:String) = {
    val s_ = STATIC_TESTS_DIR + s
    val name = NodeUtil.apiName(s_)
    val path = Shell.sourcePath(s_, name)

    WellKnownNames.useCompilerLibraries()
    Shell.setTypeChecking(true)
    Shell.setPhase(PhaseOrder.CODEGEN)
    Shell.compile(path, name + ".fss")
  }

  def testXXXCompiled0() = {
    val expected =
      "\n" + STATIC_TESTS_DIR + "XXXCompiled0.fss:17:11-15\n" +
      "    Component/API names must match their enclosing file names.\n" +
      "    File name: " + STATIC_TESTS_DIR + "XXXCompiled0.fss\n" +
      "    Component/API name: Hello"
    try {
      compile("XXXCompiled0.fss").iterator()
      assert(false, "Compilation should have signaled an error")
    }
    catch {
      case e:ProgramError =>
        assert (e.getMessage().equals(expected),
                "Bad error message: " + e.getMessage() + "\n" +
                "Should be:" + expected)
    }
  }
}
