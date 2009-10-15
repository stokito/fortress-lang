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
  
  def testExcludes() = {
    val ta = typeAnalyzer("{trait Tt excludes {Ss}, trait Ss, trait Uu extends {Tt}, trait Vv extends {Uu, Ss}}")
    assert(ta.excludes(typ("Vv"), typ("Uu")))
    assert(ta.excludes(typ("Vv"), typ("Ss")))
  }
  
}
