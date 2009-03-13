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
import _root_.java.util.LinkedList
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

  val STATIC_TESTS_DIR =
    ProjectProperties.BASEDIR + "compiler_tests"

  val LIBRARY_DIR =
    ProjectProperties.ROOTDIR + "Library"

  def compilerResult(s:String):String = {
    val s_ = STATIC_TESTS_DIR + "/" + s
    val args = new Array[String](2)
    args(0) = "compile"
    args(1) = s_
    compilerResult(args)
  }

  def compilerResult():String = {
    val args = new Array[String](1)
    args(0) = "compile"
    compilerResult(args)
  }

  def compilerResult(args:Array[String]):String = {
    val stderr = WireTappedPrintStream.make(System.err)
    System.setErr(stderr)

    Shell.main(true, args) // Call main in test mode (indicated by boolean arg)
    stderr.getString()
  }


  def compile(s:String) = {
    val s_ = STATIC_TESTS_DIR + "/" + s
    val name = NodeUtil.apiName(s_)
    val path = Shell.sourcePath(s_, name)

    WellKnownNames.useCompilerLibraries()
    Types.useCompilerLibraries()
    Shell.setTypeChecking(true)
    Shell.setPhase(PhaseOrder.CODEGEN)
    Shell.compilerPhases(path, name + ".fss")
  }

  def assertNormalCompletion(file:String) = {
    val expected = ""
    val result = compilerResult(file)
    assert(expected.equals(result))
  }

  def assertErrorSignaled(error:String,file:String) = {
    val result = compilerResult(file)
    assert(error.equals(result))
  }

  def testCompileNothing() = {
    val expected = "The compile command must be given a file.\n"
    val result = compilerResult()
    assert(expected.equals(result))
  }

  def testCompileCompilerLibraryApi() = {
   assertNormalCompletion("../../Library/CompilerLibrary.fsi")
  }

  def testCompileCompilerLibraryComponent() = {
   assertNormalCompletion("../../Library/CompilerLibrary.fss")
  }

  def testCompileCompilerBuiltinApi() = {
   assertNormalCompletion("../LibraryBuiltin/CompilerBuiltin.fsi")
  }

  def testCompileCompilerBuiltinComponent() = {
   assertNormalCompletion("../LibraryBuiltin/CompilerBuiltin.fss")
  }

  def testCompiled0a() = {
    val expected =
      "\n" +
      STATIC_TESTS_DIR + "/Compiled0.a.fss:17:11-15\n" +
      "    Component/API names must match their enclosing file names.\n" +
      "    File name: " + STATIC_TESTS_DIR + "/Compiled0.a.fss\n" +
      "    Component/API name: Hello\n" +
      "Turn on \"-debug interpreter\" for Java-level stack trace.\n"
    assertErrorSignaled(expected,"Compiled0.a.fss")
  }

  def testCompiled0b() = {
    val expected =
      STATIC_TESTS_DIR + "/Compiled0.b.fss:18:8-16\n" +
      "    Could not find API Runnable in file named Runnable.fsi on path\n    " +
      STATIC_TESTS_DIR + ":" + ProjectProperties.SOURCE_PATH + "\n"
      assertErrorSignaled(expected,"Compiled0.b.fss")
  }

  def testCompiled0c() = {
    val expected =
      STATIC_TESTS_DIR + "/Compiled0.c.fss:20:14-25\n" +
      "    Variable printlnSimple is not defined.\n" +
      "File Compiled0.c.fss has 1 error.\n"
      assertErrorSignaled(expected,"Compiled0.c.fss")
  }

  def testCompiled0d() = {
    val expected =
      STATIC_TESTS_DIR + "/Compiled0.d.fss:20:3-36\n" +
      "    Unmatched delimiter \"component\".\n" +
      "File Compiled0.d.fss has 1 error.\n"
     assertErrorSignaled(expected,"Compiled0.d.fss")
  }

  def testCompiled0e() = {
    val expected =
      STATIC_TESTS_DIR + "/Compiled0.e.fss:24:1-3\n" +
      "    Unmatched delimiter \"end\".\n" +
      "File Compiled0.e.fss has 1 error.\n"
     assertErrorSignaled(expected,"Compiled0.e.fss")
  }

  def testCompiled0f() = {
    val expected =
      STATIC_TESTS_DIR + "/Compiled0.f.fss:20:9\n" +
      "    Unmatched delimiter \"(\".\n" +
      "File Compiled0.f.fss has 1 error.\n"
      assertErrorSignaled(expected,"Compiled0.f.fss")
  }

  def testCompiled0g() = {
    val expected =
      STATIC_TESTS_DIR + "/Compiled0.g.fss:20:9\n" +
      "    Unmatched delimiter \"(\".\n" +
      STATIC_TESTS_DIR + "/Compiled0.g.fss:24:1-3\n" +
      "    Unmatched delimiter \"end\".\n" +
      "File Compiled0.g.fss has 2 errors.\n"
      assertErrorSignaled(expected,"Compiled0.g.fss")
  }

  def testCompiled0h() = {
    val expected =
      STATIC_TESTS_DIR + "/Compiled0.h.fss:20:11\n" +
      "    Unmatched delimiter \")\".\n" +
      "File Compiled0.h.fss has 1 error.\n"
      assertErrorSignaled(expected,"Compiled0.h.fss")
  }

  def testCompiled0i() = {
    val expected =
      STATIC_TESTS_DIR + "/Compiled0.i.fss:20:6\n" +
      "    Unmatched delimiter \"[\\\".\n" +
      "File Compiled0.i.fss has 1 error.\n"
      assertErrorSignaled(expected,"Compiled0.i.fss")
  }

  def testCompiled0j() = {
    val expected =
      STATIC_TESTS_DIR + "/Compiled0.j.fss:20:6-7\n" +
      "    Unmatched delimiter \"[\" and \"\\]\".\n" +
      "File Compiled0.j.fss has 1 error.\n"
    assertErrorSignaled(expected,"Compiled0.j.fss")
  }

  def testCompiled0k() = {
    val expected =
      STATIC_TESTS_DIR + "/Compiled0.k.fss:20:23-36\n" +
      "    Unmatched delimiter \"\\\"\".\n" +
      "File Compiled0.k.fss has 1 error.\n"
    assertErrorSignaled(expected,"Compiled0.k.fss")
  }

  def testCompiled0l() = {
    val expected =
      STATIC_TESTS_DIR + "/Compiled0.l.fss:20:14-21:25\n" +
      "    Unmatched delimiter \"do\".\n" +
      STATIC_TESTS_DIR + "/Compiled0.l.fss:20:3-21:25\n" +
      "    Unmatched delimiter \"component\".\n" +
      STATIC_TESTS_DIR + "/Compiled0.l.fss:21:25\n" +
      "    Unmatched delimiter \"\\\"\".\n" +
      "File Compiled0.l.fss has 3 errors.\n"
    assertErrorSignaled(expected,"Compiled0.l.fss")
  }

  def testCompiled0m() = {
    val expected =
      STATIC_TESTS_DIR + "/Compiled0.m.fss:20:3-22:3\n" +
      "    Unmatched delimiter \"component\".\n" +
      STATIC_TESTS_DIR + "/Compiled0.m.fss:21:13-25\n" +
      "    Unmatched delimiter \"\\\"\".\n" +
      "File Compiled0.m.fss has 2 errors.\n"
      assertErrorSignaled(expected,"Compiled0.m.fss")
  }

  def testCompiled0n() = {
    val expected =
      STATIC_TESTS_DIR + "/Compiled0.n.fss:20:3-17\n" +
      "    Function body has type ()->(), but declared return type is ()\n" +
      "File Compiled0.n.fss has 1 error.\n"
      assertErrorSignaled(expected,"Compiled0.n.fss")
  }

  def testCompiled0o() = {
    val expected =
      STATIC_TESTS_DIR + "/Compiled0.o.fss:20:3-22:3\n" +
      "    Missing function body.\n" +
      "File Compiled0.o.fss has 1 error.\n"
      assertErrorSignaled(expected,"Compiled0.o.fss")
  }

  def testCompiled0p() = {
    val expected =
      STATIC_TESTS_DIR + "/Compiled0.p.fss:17:11-21\n" +
      "    Component Compiled0.p exports API Executable\n" +
      "    but does not define all declarations in Executable.\n" +
      "    Missing declarations: {run():() at " +
      LIBRARY_DIR + "/Executable.fsi:22:3-23:1}"
    Shell.assertStaticErrors(compile("Compiled0.p.fss"), expected)
  }

  def testCompiled0q() = {
    val expected =
      STATIC_TESTS_DIR + "/Compiled0.q.fss:17:11-21\n" +
      "    Component Compiled0.q exports API Executable\n" +
      "    but does not define all declarations in Executable.\n" +
      "    Missing declarations: {run():() at " +
      LIBRARY_DIR + "/Executable.fsi:22:3-23:1}"
    Shell.assertStaticErrors(compile("Compiled0.q.fss"), expected)
  }

  def testCompiled0r() = {
    val expected =
      STATIC_TESTS_DIR + "/Compiled0.r.fss:17:11-21\n" +
      "    Component Compiled0.r exports API Executable\n" +
      "    but does not define all declarations in Executable.\n" +
      "    Missing declarations: {run():() at " +
      LIBRARY_DIR + "/Executable.fsi:22:3-23:1}"
    Shell.assertStaticErrors(compile("Compiled0.r.fss"), expected)
  }

  def testCompiled0s() = {
    val expected =
      STATIC_TESTS_DIR + "/Compiled0.s.fss:17:1-24:3\n" +
      "    Components should have at least one export statement.\n" +
      STATIC_TESTS_DIR + "/Compiled0.s.fss:18:1-24:3\n" +
      "    Nested component definitions are not allowed."
    Shell.assertStaticErrors(compile("Compiled0.s.fss"), expected)
  }

  def testCompiled0t() = {
    val expected =
      STATIC_TESTS_DIR + "/Compiled0.t.fss:20:8-18\n" +
      "    Component Compiled0.t imports and exports API Executable.\n" +
      "    An API must not be imported and exported by the same component."
    Shell.assertStaticErrors(compile("Compiled0.t.fss"), expected)
  }

  def testCompiled0u() = {
    val expected =
      STATIC_TESTS_DIR + "/Compiled0.u.fss:21:1-3\n" +
      "    Unmatched delimiter \"end\"."
    Shell.assertStaticErrors(compile("Compiled0.u.fss"), expected)
  }

  def testCompiled0v() = {
    val expected =
      STATIC_TESTS_DIR + "/Compiled0.v.fss:17:1-20:24\n" +
      "    Component Compiled0.v exports API Executable\n" +
      "    but does not define all declarations in Executable.\n" +
      "    Missing declarations: {run():() at " +
      LIBRARY_DIR + "/Executable.fsi:22:3-23:1}\n" +
      STATIC_TESTS_DIR + "/Compiled0.v.fss:19:1-24\n" +
      STATIC_TESTS_DIR + "/Compiled0.v.fss:20:1-24\n" +
      "    There are multiple declarations of run with the same signature: String -> ()"
    Shell.assertStaticErrors(compile("Compiled0.v.fss"), expected)
  }

  def testCompiled1a() = {
    val expected =
      STATIC_TESTS_DIR + "/Compiled1.a.fss:21:1-25:3\n" +
      "    Unmatched delimiter \"component\"."
    Shell.assertStaticErrors(compile("Compiled1.a.fss"), expected)
  }

  def testCompiled1b() = {
    val expected =
      STATIC_TESTS_DIR + "/Compiled1.b.fss:21:1-25:3\n" +
      "    Missing = in a function declaration."
    Shell.assertStaticErrors(compile("Compiled1.b.fss"), expected)
  }

  def testCompiled1c() = {
    val expected =
      STATIC_TESTS_DIR + "/Compiled1.c.fss:25:1-3\n" +
      "    Unmatched delimiter \"end\"."
    Shell.assertStaticErrors(compile("Compiled1.c.fss"), expected)
  }

/*
  def testCompiled1d() = {
    val expected =
      STATIC_TESTS_DIR + "/Compiled1.d.fss:\n" +
    Shell.assertStaticErrors(compile("Compiled1.d.fss"), expected)
  }
*/

  def testCompiled1e() = {
    val expected =
      STATIC_TESTS_DIR + "/Compiled1.e.fss:22:11-24\n" +
      "    Unmatched delimiter \"\\\"\"."
    Shell.assertStaticErrors(compile("Compiled1.e.fss"), expected)
  }

  def testCompiled1f() = {
    val expected =
      STATIC_TESTS_DIR + "/Compiled1.f.fss:22:24\n" +
      "    Unmatched delimiter \"\\\"\"."
    Shell.assertStaticErrors(compile("Compiled1.f.fss"), expected)
  }

/*
  def testCompiled1g() = {
    val expected =
      STATIC_TESTS_DIR + "/Compiled1.g.fss:\n" +
    Shell.assertStaticErrors(compile("Compiled1.g.fss"), expected)
  }
*/

  def testCompiled1h() = {
    val expected =
      STATIC_TESTS_DIR + "/Compiled1.h.fss:22:26\n" +
      "    Unmatched delimiter \"\\\"\"."
    Shell.assertStaticErrors(compile("Compiled1.h.fss"), expected)
  }

}
