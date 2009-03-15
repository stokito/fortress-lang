package com.sun.fortress.scala_src.typechecker

import com.sun.fortress.compiler.typechecker.TypeAnalyzerJUTest._
import _root_.junit.framework.TestCase
import com.sun.fortress.compiler.typechecker.SubtypeHistory
import com.sun.fortress.compiler.typechecker.TypeAnalyzer

class ScalaConstraintJUTest extends TestCase {
  
  def testAnd() = {
    var analyzer = makeAnalyzer()
    var history = new SubtypeHistory(analyzer)
    //Test basic formulas
    assert(CnFalse==CnFalse.and(CnFalse,history), "AND(CnFalse,CnFalse)=/=CnFalse")
    assert(CnFalse==CnFalse.and(CnTrue,history), "AND(CnFalse,CnTrue)=/=CnFalse")
    assert(CnFalse==CnTrue.and(CnFalse,history), "AND(CnTrue,CnFalse)=/=CnFalse")
    assert(CnTrue==CnTrue.and(CnTrue,history), "AND(CnTrue,CnTrue)=/=CnTrue")
    //Test that CnAnd(Empty,Empty) acts like CnTrue
    val alternateTrue = CnAnd(Map.empty,Map.empty,history)
    assert(alternateTrue.isTrue,"!CnAnd(Empty,Empty).isTrue")
    assert(alternateTrue.and(CnTrue,history)==CnTrue, "AND(alternateTrue,CnTrue) =/= CnTrue")
    assert(alternateTrue.and(CnFalse,history)==CnFalse, "AND(alternateTrue,CnFalse) =/= CnFalse")
    assert(CnTrue.and(alternateTrue,history)==CnTrue, "AND(CnTrue, alternateTrue) =/= CnTrue")
    assert(CnFalse.and(alternateTrue,history)==CnFalse, "AND(CnFalse, alternateTrue) =/= CnFalse")
    //Test that CnOr(CnAnd(Empty,Empty)) acts like CnTrue
    val alternateTrue2 = CnOr(List(CnAnd(Map.empty,Map.empty,history)),history)
    assert(alternateTrue2.isTrue,"CnOr(CnAnd(Empty,Empty)).isTrue")
    assert(alternateTrue2.and(CnTrue,history)==CnTrue, "AND(alternateTrue2,CnTrue) =/= CnTrue")
    assert(alternateTrue2.and(CnFalse,history)==CnFalse, "AND(alternateTrue2,CnFalse) =/= CnFalse")
    assert(CnTrue.and(alternateTrue2,history)==CnTrue, "AND(CnTrue, alternateTrue2) =/= CnTrue")
    assert(CnFalse.and(alternateTrue2,history)==CnFalse, "AND(CnFalse, alternateTrue2) =/= CnFalse")
    //Test that CnOr(Empty) acts like CnFalse
    val alternateFalse = CnOr(List(),history)
    assert(alternateFalse.isFalse,"CnOr(Empty).isFalse")
    assert(alternateFalse.and(CnTrue,history)==CnFalse, "AND(alternateFalse,CnTrue) =/= CnTrue")
    assert(alternateFalse.and(CnFalse,history)==CnFalse, "AND(alternateFalse,CnFalse) =/= CnFalse")    
    assert(CnTrue.and(alternateFalse,history)==CnFalse, "AND(CnFalse, alternateFalse) =/= CnFalse")
    assert(CnFalse.and(alternateFalse,history)==CnFalse, "AND(CnFalse, alternateFalse) =/= CnFalse") 
    //Test test that all alternate forms are eliminated
    assert(alternateTrue.and(alternateTrue,history) == CnTrue, "AND(alternateTrue,alternateTrue) =/= CnTrue")
    assert(alternateTrue.and(alternateTrue2,history) == CnTrue, "AND(alternateTrue,alternateTrue2) =/= CnTrue")
    assert(alternateTrue2.and(alternateTrue,history) == CnTrue, "AND(alternateTrue2,alternateTrue) =/= CnTrue")
    assert(alternateTrue2.and(alternateTrue2,history) == CnTrue, "AND(alternateTrue2,alternateTrue2) =/= CnTrue")
    assert(alternateTrue.and(alternateFalse,history) == CnFalse, "AND(alternateTrue,alternateFalse) =/= CnFalse")
    assert(alternateFalse.and(alternateTrue,history) == CnFalse, "AND(alternateFalse,alternateTrue) =/= CnFalse")
    assert(alternateTrue2.and(alternateFalse,history) == CnFalse, "AND(alternateTrue2,alternateFalse) =/= CnFalse")
    assert(alternateFalse.and(alternateTrue2,history) == CnFalse, "AND(alternateFalse,alternateTrue2) =/= CnFalse")
    assert(alternateFalse.and(alternateFalse,history) == CnFalse, "AND(alternateFalse,alternateFalse) =/= CnFalse")
  }
  
  def testOr() = {
    var analyzer = makeAnalyzer()
    var history = new SubtypeHistory(analyzer)
    //Test basic formulas
    assert(CnFalse==CnFalse.or(CnFalse,history), "OR(CnFalse,CnFalse)=/=CnFalse")
    assert(CnTrue==CnFalse.or(CnTrue,history), "OR(CnFalse,CnTrue)=/=CnTrue")
    assert(CnTrue==CnTrue.or(CnFalse,history), "OR(CnTrue,CnFalse)=/=CnTrue")
    assert(CnTrue==CnTrue.or(CnTrue,history), "OR(CnTrue,CnTrue)=/=CnTrue")
    //Test that CnAnd(Empty,Empty) acts like CnTrue
    val alternateTrue = CnAnd(Map.empty,Map.empty,history)
    assert(alternateTrue.isTrue,"!CnAnd(Empty,Empty).isTrue")
    assert(alternateTrue.or(CnTrue,history)==CnTrue, "OR(alternateTrue,CnTrue) == CnTrue")
    assert(alternateTrue.or(CnFalse,history)==CnTrue, "OR(alternateTrue,CnFalse) == CnTrue")
    assert(CnTrue.or(alternateTrue,history)==CnTrue, "OR(CnTrue, alternateTrue) == CnTrue")
    assert(CnFalse.or(alternateTrue,history)==CnTrue, "OR(CnFalse, alternateTrue) == CnTrue")
    //Test that CnOr(CnAnd(Empty,Empty)) acts like CnTrue
    val alternateTrue2 = CnOr(List(CnAnd(Map.empty,Map.empty,history)),history)
    assert(alternateTrue2.isTrue,"CnOr(CnAnd(Empty,Empty)).isTrue")
    assert(alternateTrue2.or(CnTrue,history)==CnTrue, "OR(alternateTrue2,CnTrue) == CnTrue")
    assert(alternateTrue2.or(CnFalse,history)==CnTrue, "OR(alternateTrue2,CnFalse) == CnTrue")
    assert(CnTrue.or(alternateTrue2,history)==CnTrue, "OR(CnTrue, alternateTrue2) == CnTrue")
    assert(CnFalse.or(alternateTrue2,history)==CnTrue, "OR(CnFalse, alternateTrue2) == CnTrue")
    //Test that CnOr(Empty) acts like CnFalse
    val alternateFalse = CnOr(List(),history)
    assert(alternateFalse.isFalse,"CnOr(Empty).isFalse")
    assert(alternateFalse.or(CnTrue,history)==CnTrue, "OR(alternateFalse,CnTrue) == CnTrue")
    assert(alternateFalse.or(CnFalse,history)==CnFalse, "OR(alternateFalse,CnFalse) == CnFalse")    
    assert(CnTrue.or(alternateFalse,history)==CnTrue, "OR(CnFalse, alternateFalse) == CnTrue")
    assert(CnFalse.or(alternateFalse,history)==CnFalse, "OR(CnFalse, alternateFalse) == CnFalse") 
    //Test test that all alternate forms are eliminated
    assert(alternateTrue.or(alternateTrue,history) == CnTrue, "OR(alternateTrue,alternateTrue) =/= CnTrue")
    assert(alternateTrue.or(alternateTrue2,history) == CnTrue, "OR(alternateTrue,alternateTrue2) =/= CnTrue")
    assert(alternateTrue2.or(alternateTrue,history) == CnTrue, "OR(alternateTrue2,alternateTrue) =/= CnTrue")
    assert(alternateTrue2.or(alternateTrue2,history) == CnTrue, "OR(alternateTrue2,alternateTrue2) =/= CnTrue")
    assert(alternateTrue.or(alternateFalse,history) == CnTrue, "OR(alternateTrue,alternateFalse) =/= CnTrue")
    assert(alternateFalse.or(alternateTrue,history) == CnTrue, "OR(alternateFalse,alternateTrue) =/= CnTrue")
    assert(alternateTrue2.or(alternateFalse,history) == CnTrue, "OR(alternateTrue2,alternateFalse) =/= CnTrue")
    assert(alternateFalse.or(alternateTrue2,history) == CnTrue, "OR(alternateFalse,alternateTrue2) =/= CnTrue")
    assert(alternateFalse.or(alternateFalse,history) == CnFalse, "OR(alternateFalse,alternateFalse) =/= CnFalse")

  }
  
  def testImplies() = {
    var analyzer = makeAnalyzer()
    var history = new SubtypeHistory(analyzer)
    //Test basic formulas
    assert(CnFalse.implies(CnFalse,history), "CnFalse -> CnFalse =/= true")
    assert(CnFalse.implies(CnTrue,history), "CnFalse -> CnTrue =/= true")
    assert(!CnTrue.implies(CnFalse,history), "CnTrue -> CnFalse =/= false")
    assert(CnTrue.implies(CnTrue,history), "CnTrue -> CnTrue =/= true")
  }
  def testSolve() = {
    assert(CnFalse.scalaSolve(Map.empty).isEmpty, "scalaSolve(CnFalse) =/= None")
    assert(CnTrue.scalaSolve(Map.empty).get.isEmpty, "scalaSolve(CnTrue) =/= Some(Nil)")
  }
}
