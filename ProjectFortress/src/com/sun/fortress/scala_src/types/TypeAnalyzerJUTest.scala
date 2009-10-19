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

package com.sun.fortress.scala_src.types

import _root_.junit.framework.TestCase
import com.sun.fortress.nodes._
import com.sun.fortress.scala_src.useful.TypeParser

class TypeAnalyzerJUTest extends TestCase {
  
  def typeAnalyzer(str:String) = TypeParser.parse(TypeParser.typeAnalyzer, str).get
  def overloadingSet(str: String) = TypeParser.parse(TypeParser.overloadingSet, str).get
  def typ(str: String) = TypeParser.parse(TypeParser.typ, str).get
  
  def testNormalize() = {
    val ta = typeAnalyzer("{trait Tt comprises {Oo, Pp}, object Oo extends {Tt}, object Pp extends {Tt}}")
    assert(ta.normalize(typ("&&{Tt, ||{Oo, Pp}}")) == typ("||{Oo, Pp}"))
  }
  
  def testMeet() = {
    val ta = typeAnalyzer("{trait Aa, trait Bb extends {Aa}}")
    assert(ta.meet(typ("(Aa, Bb)"), typ("(Bb, Aa)")) == typ("(Bb, Bb)"))
  }
  
  def testExcludes() = {
    val ta = typeAnalyzer("{trait Tt excludes {Ss}, trait Ss, trait Uu extends {Tt}, trait Vv extends {Uu, Ss}}")
    assert(ta.excludes(typ("Vv"), typ("Uu")))
    assert(ta.excludes(typ("Vv"), typ("Ss")))
  }
  
}
