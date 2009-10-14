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

package com.sun.fortress.scala_src.overloading

import _root_.junit.framework.TestCase
import com.sun.fortress.compiler.typechecker.TypeAnalyzer
import com.sun.fortress.compiler.Types.ANY
import com.sun.fortress.compiler.Types.BOTTOM
import com.sun.fortress.compiler.Types.OBJECT
import com.sun.fortress.nodes._
import com.sun.fortress.scala_src.nodes._
import com.sun.fortress.scala_src.typechecker.ExclusionOracle
import com.sun.fortress.scala_src.useful.ErrorLog
import com.sun.fortress.scala_src.useful.TypeParser
import com.sun.fortress.scala_src.useful.STypesUtil

// Done: Test Harness
// ToDo: Meet Simplification Test

class OverloadingJUTest extends TestCase {
  
  def typeAnalyzer(str:String) = TypeParser.parse(TypeParser.typeAnalyzer, str).get
  def overloadingSet(str: String) = TypeParser.parse(TypeParser.overloadingSet, str).get
  def typ(str: String) = TypeParser.parse(TypeParser.typ, str).get
  
  def testExclusion() = {
    val ta = typeAnalyzer("{[T]List[T], [T]ArrayList[T] extends {List[T]} , Foo extends {Bar}, Bar}")
    val mt = ta.meet(typ("List[Foo]"), typ("List[Bar]"))
    if(ta.excludes(typ("List[Foo]"), typ("List[Bar]")))
      println("blah")
    else
      println("hoo")
  }
  
}
