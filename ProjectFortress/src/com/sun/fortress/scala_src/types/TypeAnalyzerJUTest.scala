/*******************************************************************************
    Copyright 2010 Sun Microsystems, Inc.,
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

import _root_.junit.framework._
import com.sun.fortress.nodes._
import com.sun.fortress.scala_src.useful.TypeParser

import Assert._

class TypeAnalyzerJUTest extends TestCase {
  
  def typeAnalyzer(str:String) = TypeParser.parse(TypeParser.typeAnalyzer, str).get
  def overloadingSet(str: String) = TypeParser.parse(TypeParser.overloadingSet, str).get
  def typ(str: String) = TypeParser.parse(TypeParser.typ, str).get
  
  def testNormalize() = {
    val ta = typeAnalyzer("{trait Tt comprises {Oo, Pp}, object Oo extends {Tt}, object Pp extends {Tt}}")
    assertTrue(ta.normalize(typ("&&{Tt, ||{Oo, Pp}}")) == typ("||{Oo, Pp}"))
    assertTrue(ta.normalize(typ("(BOTTOM, Tt)")) == typ("BOTTOM"))
  }
  
  def testMeet() = {
    val ta = typeAnalyzer("{trait Aa, trait Bb extends {Aa}, trait Cc excludes {Aa}}")
    assertTrue(ta.meet(typ("(Aa, Bb)"), typ("(Bb, Aa)")) == typ("(Bb, Bb)"))
    assertTrue(ta.meet(typ("(Aa, Bb, Bb)"), typ("(Aa, Bb)")) == typ("BOTTOM"))
    assertTrue(ta.meet(typ("Bb"), typ("Cc")) == typ("BOTTOM"))
    assertTrue(ta.meet(typ("((Aa, Bb), Cc)"), typ("((Bb, Aa), Cc)")) == typ("((Bb, Bb), Cc)"))
  }
  
  def testExcludes() = {
    val ta = typeAnalyzer("""{
      trait Tt excludes {Ss},
      trait Ss,
      trait Uu extends {Tt},
      trait Vv extends {Uu, Ss}}""")
    assertTrue(ta.excludes(typ("Vv"), typ("Uu")))
    assertTrue(ta.excludes(typ("Vv"), typ("Ss")))
  }
  
  def testCovering() = {
    val ta = typeAnalyzer("""{
      trait Aa comprises {Bb, Cc},
      trait Bb extends {Aa} comprises {Dd},
      object Cc extends {Aa},
      object Dd extends {Bb, Ff},
      trait Ee comprises {Ff, Gg},
      trait Ff extends {Ee},
      object Gg extends {Ee}}""")
    
    assertTrue(ta.minimalCovering(typ("&&{Aa, Ee}")) == typ("Dd"))
  }
  
}
