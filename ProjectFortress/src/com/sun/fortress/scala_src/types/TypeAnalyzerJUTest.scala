package com.sun.fortress.scala_src.types

import _root_.junit.framework.TestCase
import com.sun.fortress.nodes._
import com.sun.fortress.scala_src.useful.TypeParser

class TypeAnalyzerJUTest extends TestCase {
  
  def typeAnalyzer(str:String) = TypeParser.parse(TypeParser.typeAnalyzer, str).get
  def overloadingSet(str: String) = TypeParser.parse(TypeParser.overloadingSet, str).get
  def typ(str: String) = TypeParser.parse(TypeParser.typ, str).get
  
  def testNormalize() = {
    val ta = typeAnalyzer("{String, Intliteral}")
    val test = ta.normalize(typ("||{BOTTOM, String}"))
    assert(test == typ("String"))
  }
  
  def testExcludes() = {
    val ta = typeAnalyzer("{ Tt excludes {Ss}, Ss, Uu extends {Tt}, Vv extends {Uu, Ss} }")
    assert(ta.excludes(typ("Vv"), typ("Uu")))
    assert(ta.excludes(typ("Vv"), typ("Ss")))
  }
  
}
