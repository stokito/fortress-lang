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

  def testCompiled0a() = {
    val expected =
      "\n" + STATIC_TESTS_DIR + "/Compiled0.a.fss:17:11-15\n" +
      "    Component/API names must match their enclosing file names.\n" +
      "    File name: " + STATIC_TESTS_DIR + "/Compiled0.a.fss\n" +
      "    Component/API name: Hello"
    try {
      compile("Compiled0.a.fss").iterator()
      assert(false, "Compilation should have signaled an error")
    }
    catch {
      case e:ProgramError =>
        assert (e.getMessage().equals(expected),
                "Bad error message: " + e.getMessage() + "\n" +
                "Should be: " + expected)
    }
  }

  def testCompiled0b() = {
    val expected =
      STATIC_TESTS_DIR + "/Compiled0.b.fss:18:8-16\n" +
      "    Could not find API Runnable in file named Runnable.fsi on path\n    " +
      STATIC_TESTS_DIR + ":" + ProjectProperties.SOURCE_PATH

    try {
      compile("Compiled0.b.fss").iterator()
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

  def testCompiled0c() = {
    val expected =
      STATIC_TESTS_DIR + "/Compiled0.c.fss:20:28-39\n" +
      "    Variable printlnSimple is not defined."
    Shell.assertStaticErrors(compile("Compiled0.c.fss"), expected)
  }

  def testCompiled0d() = {
    val expected =
      STATIC_TESTS_DIR + "/Compiled0.d.fss:20:3-50\n" +
      "    Unmatched delimiter \"component\"."
    Shell.assertStaticErrors(compile("Compiled0.d.fss"), expected)
  }

  def testCompiled0e() = {
    val expected =
      STATIC_TESTS_DIR + "/Compiled0.e.fss:24:1-3\n" +
      "    Unmatched delimiter \"end\"."
    Shell.assertStaticErrors(compile("Compiled0.e.fss"), expected)
  }

  def testCompiled0f() = {
    val expected =
      STATIC_TESTS_DIR + "/Compiled0.f.fss:20:23\n" +
      "    Unmatched delimiter \"(\"."
    Shell.assertStaticErrors(compile("Compiled0.f.fss"), expected)
  }

  def testCompiled0g() = {
    val expected =
      STATIC_TESTS_DIR + "/Compiled0.g.fss:20:23\n" +
      "    Unmatched delimiter \"(\".\n" +
      STATIC_TESTS_DIR + "/Compiled0.g.fss:24:1-3\n" +
      "    Unmatched delimiter \"end\"."
    Shell.assertStaticErrors(compile("Compiled0.g.fss"), expected)
  }

  def testCompiled0h() = {
    val expected =
      STATIC_TESTS_DIR + "/Compiled0.h.fss:20:25\n" +
      "    Unmatched delimiter \")\"."
    Shell.assertStaticErrors(compile("Compiled0.h.fss"), expected)
  }

  def testCompiled0i() = {
    val expected =
      STATIC_TESTS_DIR + "/Compiled0.i.fss:20:6\n" +
      "    Unmatched delimiter \"[\\\"."
    Shell.assertStaticErrors(compile("Compiled0.i.fss"), expected)
  }

  def testCompiled0j() = {
    val expected =
      STATIC_TESTS_DIR + "/Compiled0.j.fss:20:6-7\n" +
      "    Unmatched delimiter \"[\" and \"\\]\"."
    Shell.assertStaticErrors(compile("Compiled0.j.fss"), expected)
  }

  def testCompiled0k() = {
    val expected =
      STATIC_TESTS_DIR + "/Compiled0.k.fss:20:37-50\n" +
      "    Unmatched delimiter \"\\\"\"."
    Shell.assertStaticErrors(compile("Compiled0.k.fss"), expected)
  }

  def testCompiled0l() = {
    val expected =
      STATIC_TESTS_DIR + "/Compiled0.l.fss:20:28-21:25\n" +
      "    Unmatched delimiter \"do\".\n" +
      STATIC_TESTS_DIR + "/Compiled0.l.fss:20:3-21:25\n" +
      "    Unmatched delimiter \"component\".\n" +
      STATIC_TESTS_DIR + "/Compiled0.l.fss:21:25\n" +
      "    Unmatched delimiter \"\\\"\"."
    Shell.assertStaticErrors(compile("Compiled0.l.fss"), expected)
  }

  def testCompiled0m() = {
    val expected =
      STATIC_TESTS_DIR + "/Compiled0.m.fss:20:3-22:3\n" +
      "    Unmatched delimiter \"component\".\n" +
      STATIC_TESTS_DIR + "/Compiled0.m.fss:21:13-25\n" +
      "    Unmatched delimiter \"\\\"\"."
    Shell.assertStaticErrors(compile("Compiled0.m.fss"), expected)
  }

  def testCompiled0n() = {
    val expected =
      STATIC_TESTS_DIR + "/Compiled0.n.fss:20:3-31\n" +
      "    Function body has type (String...)->(), but declared return type is ()"
    Shell.assertStaticErrors(compile("Compiled0.n.fss"), expected)
  }

  def testCompiled0o() = {
    val expected =
      STATIC_TESTS_DIR + "/Compiled0.o.fss:20:3-22:3\n" +
      "    Missing function body."
    Shell.assertStaticErrors(compile("Compiled0.o.fss"), expected)
  }

  def testCompiled0s() = {
    val expected =
      STATIC_TESTS_DIR + "/Compiled0.s.fss:18:1-24:3\n" +
      "    Nested component definitions are not allowed."
    Shell.assertStaticErrors(compile("Compiled0.s.fss"), expected)
  }

  def testCompiled0u() = {
    val expected =
      STATIC_TESTS_DIR + "/Compiled0.u.fss:21:1-3\n" +
      "    Unmatched delimiter \"end\"."
    Shell.assertStaticErrors(compile("Compiled0.u.fss"), expected)
  }

}
