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

import _root_.java.util.Arrays

import junit.framework.TestSuite

import com.sun.fortress.compiler.phases.PhaseOrder
import com.sun.fortress.exceptions.ProgramError
import com.sun.fortress.exceptions.WrappedException
import com.sun.fortress.Shell
import com.sun.fortress.exceptions.StaticError
import com.sun.fortress.repository.ProjectProperties
import com.sun.fortress.useful.TestCaseWrapper
import com.sun.fortress.nodes_util.NodeUtil
import com.sun.fortress.interpreter.glue.WellKnownNames

import edu.rice.cs.plt.tuple.Option


class CompilerJUTest() extends TestCaseWrapper {

  val STATIC_TESTS_DIR =
    ProjectProperties.BASEDIR + "compiler_tests"

  def compile(s:String) = {
    val s_ = STATIC_TESTS_DIR + "/" + s
    val name = NodeUtil.apiName(s_)
    val path = Shell.sourcePath(s_, name)

    WellKnownNames.useCompilerLibraries()
    Shell.setTypeChecking(true)
    Shell.setPhase(PhaseOrder.CODEGEN)
    Shell.compile(path, name + ".fss")
  }

  def testXXXCompiled0() = {
    val expected =
      "\n" + STATIC_TESTS_DIR + "/XXXCompiled0.fss:17:11-15\n" +
      "    Component/API names must match their enclosing file names.\n" +
      "    File name: " + STATIC_TESTS_DIR + "/XXXCompiled0.fss\n" +
      "    Component/API name: Hello"
    try {
      compile("XXXCompiled0.fss").iterator()
      assert(false, "Compilation should have signaled an error")
    }
    catch {
      case e:ProgramError =>
        assert (e.getMessage().equals(expected),
                "Bad error message: " + e.getMessage() + "\n" +
                "Should be: " + expected)
    }
  }

  def testXXXCompiled1() = {
    val expected =
      STATIC_TESTS_DIR + "/XXXCompiled1.fss:18:8-16\n" +
      "    Could not find API Runnable in file named Runnable.fsi on path\n    " +
      STATIC_TESTS_DIR + ":" + ProjectProperties.SOURCE_PATH

    try {
      compile("XXXCompiled1.fss").iterator()
      assert(false, "Compilation should have signaled an error")
    }
    catch {
      case e:WrappedException => {
        assert (e.getMessage().equals(expected),
                "Bad error message: " + e.getMessage() + "\n" +
                "Should be: " + expected)
      }
    }
  }

  def testXXXCompiled2() = {
    val expected =
      STATIC_TESTS_DIR + "/XXXCompiled2.fss:20:28-39\n" +
      "    Variable printlnSimple is not defined."
    Shell.assertStaticErrors(compile("XXXCompiled2.fss"), expected)
  }

  def testXXXCompiled3() = {
    val expected =
      STATIC_TESTS_DIR + "/XXXCompiled3.fss:20:3-50\n" +
      "    Unmatched delimiter \"component\"."
    Shell.assertStaticErrors(compile("XXXCompiled3.fss"), expected)
  }

  def testXXXCompiled4() = {
    val expected =
      STATIC_TESTS_DIR + "/XXXCompiled4.fss:24:1-2\n" +
      "    Unmatched delimiter \"end\"."
    Shell.assertStaticErrors(compile("XXXCompiled4.fss"), expected)
  }

  def testXXXCompiled5() = {
    val expected =
      STATIC_TESTS_DIR + "/XXXCompiled5.fss:20:23\n" +
      "    Unmatched delimiter \"(\"."
    Shell.assertStaticErrors(compile("XXXCompiled5.fss"), expected)
  }

  def testXXXCompiled6() = {
    val expected =
      STATIC_TESTS_DIR + "/XXXCompiled6.fss:20:23\n" +
      "    Unmatched delimiter \"(\".\n" +
      STATIC_TESTS_DIR + "/XXXCompiled6.fss:24:1-2\n" +
      "    Unmatched delimiter \"end\"."
    Shell.assertStaticErrors(compile("XXXCompiled6.fss"), expected)
  }

  def testXXXCompiled7() = {
    val expected =
      STATIC_TESTS_DIR + "/XXXCompiled7.fss:20:25\n" +
      "    Unmatched delimiter \")\"."
    Shell.assertStaticErrors(compile("XXXCompiled7.fss"), expected)
  }

  def testXXXCompiled8() = {
    val expected =
      STATIC_TESTS_DIR + "/XXXCompiled8.fss:20:6\n" +
      "    Unmatched delimiter \"[\\\"."
    Shell.assertStaticErrors(compile("XXXCompiled8.fss"), expected)
  }

  def testXXXCompiled9() = {
    val expected =
      STATIC_TESTS_DIR + "/XXXCompiled9.fss:20:6-7\n" +
      "    Unmatched delimiter \"[\" and \"\\]\"."
    Shell.assertStaticErrors(compile("XXXCompiled9.fss"), expected)
  }

  def testXXXCompiled10() = {
    val expected =
      STATIC_TESTS_DIR + "/XXXCompiled10.fss:20:37-50\n" +
      "    Unmatched delimiter \"\\\"\"."
    Shell.assertStaticErrors(compile("XXXCompiled10.fss"), expected)
  }

  def testXXXCompiled11() = {
    val expected =
      STATIC_TESTS_DIR + "/XXXCompiled11.fss:20:28-21:24\n" +
      "    Unmatched delimiter \"do\".\n" +
      STATIC_TESTS_DIR + "/XXXCompiled11.fss:20:3-21:24\n" +
      "    Unmatched delimiter \"component\".\n" +
      STATIC_TESTS_DIR + "/XXXCompiled11.fss:21:25\n" +
      "    Unmatched delimiter \"\\\"\"."
    Shell.assertStaticErrors(compile("XXXCompiled11.fss"), expected)
  }

  def testXXXCompiled12() = {
    val expected =
      STATIC_TESTS_DIR + "/XXXCompiled12.fss:20:3-22:2\n" +
      "    Unmatched delimiter \"component\".\n" +
      STATIC_TESTS_DIR + "/XXXCompiled12.fss:21:13-25\n" +
      "    Unmatched delimiter \"\\\"\"."
    Shell.assertStaticErrors(compile("XXXCompiled12.fss"), expected)
  }

  def testXXXCompiled13() = {
    val expected =
      STATIC_TESTS_DIR + "/XXXCompiled13.fss:20:3-35\n" +
      "    Function body has type FlatString->(), but declared return type is ()" 
    Shell.assertStaticErrors(compile("XXXCompiled13.fss"), expected)
  }


  def testXXXCompiled14() = {
    val expected =
      STATIC_TESTS_DIR + "/XXXCompiled14.fss:20:3-22:2\n" +
      "    Missing function body."
    Shell.assertStaticErrors(compile("XXXCompiled14.fss"), expected)
  }

  def testXXXCompiled20() = {
    val expected =
      STATIC_TESTS_DIR + "/XXXCompiled20.fss:21:1-2\n" +
      "    Unmatched delimiter \"end\"."
    Shell.assertStaticErrors(compile("XXXCompiled20.fss"), expected)
  }

}
