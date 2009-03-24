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

package com.sun.fortress.scala_src.typechecker

import com.sun.fortress.compiler.typechecker.TypeAnalyzerJUTest._
import _root_.junit.framework.TestCase
import com.sun.fortress.compiler.typechecker.SubtypeHistory
import com.sun.fortress.compiler.typechecker.TypeAnalyzer
import com.sun.fortress.nodes_util.NodeFactory._

class ScalaConstraintJUTest extends TestCase {

  def testAnd() = {
    var analyzer = makeAnalyzer()
    var history = new SubtypeHistory(analyzer)
    //Test basic formulas
    assert(CnFalse==CnFalse.scalaAnd(CnFalse,history), "AND(CnFalse,CnFalse)=/=CnFalse")
    assert(CnFalse==CnFalse.scalaAnd(CnTrue,history), "AND(CnFalse,CnTrue)=/=CnFalse")
    assert(CnFalse==CnTrue.scalaAnd(CnFalse,history), "AND(CnTrue,CnFalse)=/=CnFalse")
    assert(CnTrue==CnTrue.scalaAnd(CnTrue,history), "AND(CnTrue,CnTrue)=/=CnTrue")
    //Test that CnAnd(Empty,Empty) acts like CnTrue
    val alternateTrue = CnAnd(Map.empty,Map.empty,history)
    assert(alternateTrue.isTrue,"!CnAnd(Empty,Empty).isTrue")
    assert(alternateTrue.scalaAnd(CnTrue,history)==CnTrue, "AND(alternateTrue,CnTrue) =/= CnTrue")
    assert(alternateTrue.scalaAnd(CnFalse,history)==CnFalse, "AND(alternateTrue,CnFalse) =/= CnFalse")
    assert(CnTrue.scalaAnd(alternateTrue,history)==CnTrue, "AND(CnTrue, alternateTrue) =/= CnTrue")
    assert(CnFalse.scalaAnd(alternateTrue,history)==CnFalse, "AND(CnFalse, alternateTrue) =/= CnFalse")
    //Test that CnOr(CnAnd(Empty,Empty)) acts like CnTrue
    val alternateTrue2 = CnOr(List(CnAnd(Map.empty,Map.empty,history)),history)
    assert(alternateTrue2.isTrue,"CnOr(CnAnd(Empty,Empty)).isTrue")
    assert(alternateTrue2.scalaAnd(CnTrue,history)==CnTrue, "AND(alternateTrue2,CnTrue) =/= CnTrue")
    assert(alternateTrue2.scalaAnd(CnFalse,history)==CnFalse, "AND(alternateTrue2,CnFalse) =/= CnFalse")
    assert(CnTrue.scalaAnd(alternateTrue2,history)==CnTrue, "AND(CnTrue, alternateTrue2) =/= CnTrue")
    assert(CnFalse.scalaAnd(alternateTrue2,history)==CnFalse, "AND(CnFalse, alternateTrue2) =/= CnFalse")
    //Test that CnOr(Empty) acts like CnFalse
    val alternateFalse = CnOr(List(),history)
    assert(alternateFalse.isFalse,"CnOr(Empty).isFalse")
    assert(alternateFalse.scalaAnd(CnTrue,history)==CnFalse, "AND(alternateFalse,CnTrue) =/= CnTrue")
    assert(alternateFalse.scalaAnd(CnFalse,history)==CnFalse, "AND(alternateFalse,CnFalse) =/= CnFalse")
    assert(CnTrue.scalaAnd(alternateFalse,history)==CnFalse, "AND(CnFalse, alternateFalse) =/= CnFalse")
    assert(CnFalse.scalaAnd(alternateFalse,history)==CnFalse, "AND(CnFalse, alternateFalse) =/= CnFalse")
    //Test test that all alternate forms are eliminated
    assert(alternateTrue.scalaAnd(alternateTrue,history) == CnTrue, "AND(alternateTrue,alternateTrue) =/= CnTrue")
    assert(alternateTrue.scalaAnd(alternateTrue2,history) == CnTrue, "AND(alternateTrue,alternateTrue2) =/= CnTrue")
    assert(alternateTrue2.scalaAnd(alternateTrue,history) == CnTrue, "AND(alternateTrue2,alternateTrue) =/= CnTrue")
    assert(alternateTrue2.scalaAnd(alternateTrue2,history) == CnTrue, "AND(alternateTrue2,alternateTrue2) =/= CnTrue")
    assert(alternateTrue.scalaAnd(alternateFalse,history) == CnFalse, "AND(alternateTrue,alternateFalse) =/= CnFalse")
    assert(alternateFalse.scalaAnd(alternateTrue,history) == CnFalse, "AND(alternateFalse,alternateTrue) =/= CnFalse")
    assert(alternateTrue2.scalaAnd(alternateFalse,history) == CnFalse, "AND(alternateTrue2,alternateFalse) =/= CnFalse")
    assert(alternateFalse.scalaAnd(alternateTrue2,history) == CnFalse, "AND(alternateFalse,alternateTrue2) =/= CnFalse")
    assert(alternateFalse.scalaAnd(alternateFalse,history) == CnFalse, "AND(alternateFalse,alternateFalse) =/= CnFalse")
  }

  def testOr() = {
    var analyzer = makeAnalyzer()
    var history = new SubtypeHistory(analyzer)
    //Test basic formulas
    assert(CnFalse==CnFalse.scalaOr(CnFalse,history), "OR(CnFalse,CnFalse)=/=CnFalse")
    assert(CnTrue==CnFalse.scalaOr(CnTrue,history), "OR(CnFalse,CnTrue)=/=CnTrue")
    assert(CnTrue==CnTrue.scalaOr(CnFalse,history), "OR(CnTrue,CnFalse)=/=CnTrue")
    assert(CnTrue==CnTrue.scalaOr(CnTrue,history), "OR(CnTrue,CnTrue)=/=CnTrue")
    //Test that CnAnd(Empty,Empty) acts like CnTrue
    val alternateTrue = CnAnd(Map.empty,Map.empty,history)
    assert(alternateTrue.scalaOr(CnTrue,history)==CnTrue, "OR(alternateTrue,CnTrue) == CnTrue")
    assert(alternateTrue.scalaOr(CnFalse,history)==CnTrue, "OR(alternateTrue,CnFalse) == CnTrue")
    assert(CnTrue.scalaOr(alternateTrue,history)==CnTrue, "OR(CnTrue, alternateTrue) == CnTrue")
    assert(CnFalse.scalaOr(alternateTrue,history)==CnTrue, "OR(CnFalse, alternateTrue) == CnTrue")
    //Test that CnOr(CnAnd(Empty,Empty)) acts like CnTrue
    val alternateTrue2 = CnOr(List(alternateTrue),history)
    assert(alternateTrue2.isTrue,"CnOr(CnAnd(Empty,Empty)).isTrue")
    assert(alternateTrue2.scalaOr(CnTrue,history)==CnTrue, "OR(alternateTrue2,CnTrue) == CnTrue")
    assert(alternateTrue2.scalaOr(CnFalse,history)==CnTrue, "OR(alternateTrue2,CnFalse) == CnTrue")
    assert(CnTrue.scalaOr(alternateTrue2,history)==CnTrue, "OR(CnTrue, alternateTrue2) == CnTrue")
    assert(CnFalse.scalaOr(alternateTrue2,history)==CnTrue, "OR(CnFalse, alternateTrue2) == CnTrue")
    //Test that CnOr(Empty) acts like CnFalse
    val alternateFalse = CnOr(List(),history)
    assert(alternateFalse.scalaOr(CnTrue,history)==CnTrue, "OR(alternateFalse,CnTrue) == CnTrue")
    assert(alternateFalse.scalaOr(CnFalse,history)==CnFalse, "OR(alternateFalse,CnFalse) == CnFalse")
    assert(CnTrue.scalaOr(alternateFalse,history)==CnTrue, "OR(CnFalse, alternateFalse) == CnTrue")
    assert(CnFalse.scalaOr(alternateFalse,history)==CnFalse, "OR(CnFalse, alternateFalse) == CnFalse")
    //Test test that all alternate forms are eliminated
    assert(alternateTrue.scalaOr(alternateTrue,history) == CnTrue, "OR(alternateTrue,alternateTrue) =/= CnTrue")
    assert(alternateTrue.scalaOr(alternateTrue2,history) == CnTrue, "OR(alternateTrue,alternateTrue2) =/= CnTrue")
    assert(alternateTrue2.scalaOr(alternateTrue,history) == CnTrue, "OR(alternateTrue2,alternateTrue) =/= CnTrue")
    assert(alternateTrue2.scalaOr(alternateTrue2,history) == CnTrue, "OR(alternateTrue2,alternateTrue2) =/= CnTrue")
    assert(alternateTrue.scalaOr(alternateFalse,history) == CnTrue, "OR(alternateTrue,alternateFalse) =/= CnTrue")
    assert(alternateFalse.scalaOr(alternateTrue,history) == CnTrue, "OR(alternateFalse,alternateTrue) =/= CnTrue")
    assert(alternateTrue2.scalaOr(alternateFalse,history) == CnTrue, "OR(alternateTrue2,alternateFalse) =/= CnTrue")
    assert(alternateFalse.scalaOr(alternateTrue2,history) == CnTrue, "OR(alternateFalse,alternateTrue2) =/= CnTrue")
    assert(alternateFalse.scalaOr(alternateFalse,history) == CnFalse, "OR(alternateFalse,alternateFalse) =/= CnFalse")
  }

  def testImplies() = {
    var analyzer = makeAnalyzer()
    var history = new SubtypeHistory(analyzer)
    //Test basic formulas
    assert(CnFalse.implies(CnFalse,history), "CnFalse -> CnFalse =/= true")
    assert(CnFalse.implies(CnTrue,history), "CnFalse -> CnTrue =/= true")
    assert(!CnTrue.implies(CnFalse,history), "CnTrue -> CnFalse =/= false")
    assert(CnTrue.implies(CnTrue,history), "CnTrue -> CnTrue =/= true")
    //Test that CnAnd(Empty,Empty) is equivalent to CnTrue
    val alternateTrue = CnAnd(Map.empty,Map.empty,history)
    assert(alternateTrue.isTrue,"!CnAnd(Empty,Empty).isTrue")
    //Test that CnAnd(Empty,Empty) is equivalent to CnTrue
    val alternateTrue2 = CnOr(List(alternateTrue),history)
    assert(alternateTrue2.isTrue,"CnOr(CnAnd(Empty,Empty)).isTrue")
    //Test that CnOr(Empty) is equivalent to CnFalse
    val alternateFalse = CnOr(List(),history)
    assert(alternateFalse.isFalse,"CnOr(Empty).isFalse")
    analyzer = makeAnalyzer(makeTrait("A"),makeTrait("B","A"),makeTrait("C"),makeTrait("D","C"))
    history = new SubtypeHistory(analyzer)
    //Declarations
    val ivar1 = make_InferenceVarType(typeSpan)
    val ivar2 = make_InferenceVarType(typeSpan)
    val typea = makeType("A")
    val typeb = makeType("B")
    val typec = makeType("C")
    val typed = makeType("D")
    val map1a = Map.empty.update(ivar1,typea)
    val map1b = Map.empty.update(ivar1,typeb)
    val map1c = Map.empty.update(ivar1,typec)
    val map1d = Map.empty.update(ivar1,typed)
    val map2a = Map.empty.update(ivar2,typea)
    val map2b = Map.empty.update(ivar2,typeb)
    val map2c = Map.empty.update(ivar2,typec)
    val map2d = Map.empty.update(ivar2,typed)
    
    //Test that unsatisfiable formulas are equivalent to CnFalse
    val c1b = CnAnd(map1b,map1c,history)
    assert(c1b.isFalse,"c <: 1 <: b is not false ")
    //Test that a satisfiable formula is not false
    val b1a = CnAnd(map1a,map1b,history)
    assert(!b1a.isFalse,"b <: 1 <: a is false")
    //Test that implication works for conjunctions and disjunctions
    val bot1b = CnAnd(map1b,Map.empty,history)
    val bot1a = CnAnd(map1a,Map.empty,history)
    assert(bot1b.implies(bot1a,history),"(1 <: B) -> (1 <: A) =/= true")
    val a1any = CnAnd(Map.empty, map1a, history)
    val b1any = CnAnd(Map.empty, map1b, history)
    assert(a1any.implies(b1any,history),"(A <: 1) -> (B <: 1) =/= true")
    val a2any = CnAnd(Map.empty, map2a, history)
    assert(!a1any.implies(a2any,history),"(A <: 1) -> (A <: 2) =/= false")
    val b2any = CnAnd(Map.empty, map2b, history)
    val a1anyAnda2any = a1any.scalaAnd(a2any,history)
    val b1anyAndb2any = b1any.scalaAnd(b2any,history)
    assert(a1anyAnda2any.implies(b1anyAndb2any,history),"(A<:1 and A<:2)->(B<:1 and B<:2) =/= true")
    assert(!b1anyAndb2any.implies(a1anyAnda2any,history),"(B<:1 and B<:2)->(A<:1 and A<:2) =/= false")
    val b1anyOrb2any = b1any.scalaOr(b2any,history)
    assert(a1anyAnda2any.implies(b1anyOrb2any,history),"(A<:1 and A<:2)->(B<:1 or B<:2) =/= true")
    val a1anyOrb1any = a1any.scalaOr(a1any,history)
    assert(a1anyOrb1any.implies(b1any,history),"(A<:1 or B<:1)->(B<:1) =/= true")
    assert(a1anyOrb1any.implies(a1any,history),"(A<:1 or B<:1)->(A<:1) =/= false")
  }
  
  def testSolve() = {
    var analyzer = makeAnalyzer()
    var history = new SubtypeHistory(analyzer)
    assert(CnFalse.scalaSolve(Map.empty).isEmpty, "SOLVE(CnFalse) =/= None")
    assert(CnTrue.scalaSolve(Map.empty).get.isEmpty, "SOLVE(CnTrue) =/= Some(Nil)")
    val alternateTrue = CnAnd(Map.empty,Map.empty,history)
    val alternateTrue2 = CnOr(List(alternateTrue),history)
    val alternateFalse = CnOr(List(),history)
    assert(alternateFalse.scalaSolve(Map.empty).isEmpty, "SOLVE(CnFalse) =/= None")
    assert(alternateTrue.scalaSolve(Map.empty).get.isEmpty, "SOLVE(CnTrue) =/= Some(Nil)")
    assert(alternateTrue2.scalaSolve(Map.empty).get.isEmpty, "SOLVE(CnTrue) =/= Some(Nil)")
    analyzer = makeAnalyzer(makeTrait("A"),makeTrait("B","A"),makeTrait("C"),makeTrait("D","C"))
    history = new SubtypeHistory(analyzer)
        //Declarations
    val ivar1 = make_InferenceVarType(typeSpan)
    val ivar2 = make_InferenceVarType(typeSpan)
    val typea = makeType("A")
    val typeb = makeType("B")
    val typec = makeType("C")
    val typed = makeType("D")
    val map1a = Map.empty.update(ivar1,typea)
    val map1b = Map.empty.update(ivar1,typeb)
    val map1c = Map.empty.update(ivar1,typec)
    val map1d = Map.empty.update(ivar1,typed)
    val map2a = Map.empty.update(ivar2,typea)
    val map2b = Map.empty.update(ivar2,typeb)
    val map2c = Map.empty.update(ivar2,typec)
    val map2d = Map.empty.update(ivar2,typed)
    val b1a = CnAnd(map1a,map1b,history)
    //Check that solving a contradictory formula gives you nothing
    val c1b = CnAnd(map1b,map1c,history)
    val solved_c1b = c1b.scalaSolve(Map.empty)
    //Check that solving a constraint with no bounds works
    assert(solved_c1b.isEmpty,"solve(C<:1<:B, {}) is not empty")
    val solved_b1a = b1a.scalaSolve(Map.empty)
    assert(solved_b1a.isDefined && solved_b1a.get==map1b,"SOLVE(B<:1<:A , {}) = 1:=B")
    //Check that if the solution to a constraint is out of bounds it fails
    val solved_b1a_1c = b1a.scalaSolve(map1c)
    assert(solved_b1a_1c.isEmpty,"Solve(B<:1<:A, {1<:C}) is not empty")
    //Check that if the solution to a constraint is in bounds it succeeds
    val solved_b1a_1a = b1a.scalaSolve(map1a)
    assert(solved_b1a_1a.isDefined && solved_b1a_1a.get==map1b,"SOLVE(B<:1<:A, {1<:A}) = 1:=B")
  }
}
