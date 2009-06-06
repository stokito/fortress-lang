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
import com.sun.fortress.nodes._

class ScalaConstraintJUTest extends TestCase {

  def testAnd() = {
    val analyzer = makeAnalyzer()
    val history = new SubtypeHistory(analyzer)
    def subtype(t1: Type, t2: Type) = history.subtypeNormal(t1,t2).isTrue
    //Test basic formulas
    assert(CnFalse==CnFalse.scalaAnd(CnFalse,subtype), "AND(CnFalse,CnFalse)=/=CnFalse")
    assert(CnFalse==CnFalse.scalaAnd(CnTrue,subtype), "AND(CnFalse,CnTrue)=/=CnFalse")
    assert(CnFalse==CnTrue.scalaAnd(CnFalse,subtype), "AND(CnTrue,CnFalse)=/=CnFalse")
    assert(CnTrue==CnTrue.scalaAnd(CnTrue,subtype), "AND(CnTrue,CnTrue)=/=CnTrue")
    //Test that CnAnd(Empty,Empty) acts like CnTrue
    val alternateTrue = CnAnd(Map.empty,Map.empty,subtype)
    assert(alternateTrue.isTrue,"!CnAnd(Empty,Empty).isTrue")
    assert(alternateTrue.scalaAnd(CnTrue,subtype)==CnTrue, "AND(alternateTrue,CnTrue) =/= CnTrue")
    assert(alternateTrue.scalaAnd(CnFalse,subtype)==CnFalse, "AND(alternateTrue,CnFalse) =/= CnFalse")
    assert(CnTrue.scalaAnd(alternateTrue,subtype)==CnTrue, "AND(CnTrue, alternateTrue) =/= CnTrue")
    assert(CnFalse.scalaAnd(alternateTrue,subtype)==CnFalse, "AND(CnFalse, alternateTrue) =/= CnFalse")
    //Test that CnOr(CnAnd(Empty,Empty)) acts like CnTrue
    val alternateTrue2 = CnOr(List(CnAnd(Map.empty,Map.empty,subtype)),subtype)
    assert(alternateTrue2.isTrue,"CnOr(CnAnd(Empty,Empty)).isTrue")
    assert(alternateTrue2.scalaAnd(CnTrue,subtype)==CnTrue, "AND(alternateTrue2,CnTrue) =/= CnTrue")
    assert(alternateTrue2.scalaAnd(CnFalse,subtype)==CnFalse, "AND(alternateTrue2,CnFalse) =/= CnFalse")
    assert(CnTrue.scalaAnd(alternateTrue2,subtype)==CnTrue, "AND(CnTrue, alternateTrue2) =/= CnTrue")
    assert(CnFalse.scalaAnd(alternateTrue2,subtype)==CnFalse, "AND(CnFalse, alternateTrue2) =/= CnFalse")
    //Test that CnOr(Empty) acts like CnFalse
    val alternateFalse = CnOr(List(),subtype)
    assert(alternateFalse.isFalse,"CnOr(Empty).isFalse")
    assert(alternateFalse.scalaAnd(CnTrue,subtype)==CnFalse, "AND(alternateFalse,CnTrue) =/= CnTrue")
    assert(alternateFalse.scalaAnd(CnFalse,subtype)==CnFalse, "AND(alternateFalse,CnFalse) =/= CnFalse")
    assert(CnTrue.scalaAnd(alternateFalse,subtype)==CnFalse, "AND(CnFalse, alternateFalse) =/= CnFalse")
    assert(CnFalse.scalaAnd(alternateFalse,subtype)==CnFalse, "AND(CnFalse, alternateFalse) =/= CnFalse")
    //Test test that all alternate forms are eliminated
    assert(alternateTrue.scalaAnd(alternateTrue,subtype) == CnTrue, "AND(alternateTrue,alternateTrue) =/= CnTrue")
    assert(alternateTrue.scalaAnd(alternateTrue2,subtype) == CnTrue, "AND(alternateTrue,alternateTrue2) =/= CnTrue")
    assert(alternateTrue2.scalaAnd(alternateTrue,subtype) == CnTrue, "AND(alternateTrue2,alternateTrue) =/= CnTrue")
    assert(alternateTrue2.scalaAnd(alternateTrue2,subtype) == CnTrue, "AND(alternateTrue2,alternateTrue2) =/= CnTrue")
    assert(alternateTrue.scalaAnd(alternateFalse,subtype) == CnFalse, "AND(alternateTrue,alternateFalse) =/= CnFalse")
    assert(alternateFalse.scalaAnd(alternateTrue,subtype) == CnFalse, "AND(alternateFalse,alternateTrue) =/= CnFalse")
    assert(alternateTrue2.scalaAnd(alternateFalse,subtype) == CnFalse, "AND(alternateTrue2,alternateFalse) =/= CnFalse")
    assert(alternateFalse.scalaAnd(alternateTrue2,subtype) == CnFalse, "AND(alternateFalse,alternateTrue2) =/= CnFalse")
    assert(alternateFalse.scalaAnd(alternateFalse,subtype) == CnFalse, "AND(alternateFalse,alternateFalse) =/= CnFalse")
  }

  def testOr() = {
    val analyzer = makeAnalyzer()
    val history = new SubtypeHistory(analyzer)
    def subtype(t1: Type, t2: Type) = history.subtypeNormal(t1,t2).isTrue
    //Test basic formulas
    assert(CnFalse==CnFalse.scalaOr(CnFalse,subtype), "OR(CnFalse,CnFalse)=/=CnFalse")
    assert(CnTrue==CnFalse.scalaOr(CnTrue,subtype), "OR(CnFalse,CnTrue)=/=CnTrue")
    assert(CnTrue==CnTrue.scalaOr(CnFalse,subtype), "OR(CnTrue,CnFalse)=/=CnTrue")
    assert(CnTrue==CnTrue.scalaOr(CnTrue,subtype), "OR(CnTrue,CnTrue)=/=CnTrue")
    //Test that CnAnd(Empty,Empty) acts like CnTrue
    val alternateTrue = CnAnd(Map.empty,Map.empty,subtype)
    assert(alternateTrue.scalaOr(CnTrue,subtype)==CnTrue, "OR(alternateTrue,CnTrue) == CnTrue")
    assert(alternateTrue.scalaOr(CnFalse,subtype)==CnTrue, "OR(alternateTrue,CnFalse) == CnTrue")
    assert(CnTrue.scalaOr(alternateTrue,subtype)==CnTrue, "OR(CnTrue, alternateTrue) == CnTrue")
    assert(CnFalse.scalaOr(alternateTrue,subtype)==CnTrue, "OR(CnFalse, alternateTrue) == CnTrue")
    //Test that CnOr(CnAnd(Empty,Empty)) acts like CnTrue
    val alternateTrue2 = CnOr(List(alternateTrue),subtype)
    assert(alternateTrue2.isTrue,"CnOr(CnAnd(Empty,Empty)).isTrue")
    assert(alternateTrue2.scalaOr(CnTrue,subtype)==CnTrue, "OR(alternateTrue2,CnTrue) == CnTrue")
    assert(alternateTrue2.scalaOr(CnFalse,subtype)==CnTrue, "OR(alternateTrue2,CnFalse) == CnTrue")
    assert(CnTrue.scalaOr(alternateTrue2,subtype)==CnTrue, "OR(CnTrue, alternateTrue2) == CnTrue")
    assert(CnFalse.scalaOr(alternateTrue2,subtype)==CnTrue, "OR(CnFalse, alternateTrue2) == CnTrue")
    //Test that CnOr(Empty) acts like CnFalse
    val alternateFalse = CnOr(List(),subtype)
    assert(alternateFalse.scalaOr(CnTrue,subtype)==CnTrue, "OR(alternateFalse,CnTrue) == CnTrue")
    assert(alternateFalse.scalaOr(CnFalse,subtype)==CnFalse, "OR(alternateFalse,CnFalse) == CnFalse")
    assert(CnTrue.scalaOr(alternateFalse,subtype)==CnTrue, "OR(CnFalse, alternateFalse) == CnTrue")
    assert(CnFalse.scalaOr(alternateFalse,subtype)==CnFalse, "OR(CnFalse, alternateFalse) == CnFalse")
    //Test test that all alternate forms are eliminated
    assert(alternateTrue.scalaOr(alternateTrue,subtype) == CnTrue, "OR(alternateTrue,alternateTrue) =/= CnTrue")
    assert(alternateTrue.scalaOr(alternateTrue2,subtype) == CnTrue, "OR(alternateTrue,alternateTrue2) =/= CnTrue")
    assert(alternateTrue2.scalaOr(alternateTrue,subtype) == CnTrue, "OR(alternateTrue2,alternateTrue) =/= CnTrue")
    assert(alternateTrue2.scalaOr(alternateTrue2,subtype) == CnTrue, "OR(alternateTrue2,alternateTrue2) =/= CnTrue")
    assert(alternateTrue.scalaOr(alternateFalse,subtype) == CnTrue, "OR(alternateTrue,alternateFalse) =/= CnTrue")
    assert(alternateFalse.scalaOr(alternateTrue,subtype) == CnTrue, "OR(alternateFalse,alternateTrue) =/= CnTrue")
    assert(alternateTrue2.scalaOr(alternateFalse,subtype) == CnTrue, "OR(alternateTrue2,alternateFalse) =/= CnTrue")
    assert(alternateFalse.scalaOr(alternateTrue2,subtype) == CnTrue, "OR(alternateFalse,alternateTrue2) =/= CnTrue")
    assert(alternateFalse.scalaOr(alternateFalse,subtype) == CnFalse, "OR(alternateFalse,alternateFalse) =/= CnFalse")
  }

  def testImplies() = {
    val analyzer = makeAnalyzer()
    val history = new SubtypeHistory(analyzer)
    def subtype(t1: Type, t2: Type) = history.subtypeNormal(t1,t2).isTrue
    //Test basic formulas
    assert(CnFalse.implies(CnFalse,subtype), "CnFalse -> CnFalse =/= true")
    assert(CnFalse.implies(CnTrue,subtype), "CnFalse -> CnTrue =/= true")
    assert(!CnTrue.implies(CnFalse,subtype), "CnTrue -> CnFalse =/= false")
    assert(CnTrue.implies(CnTrue,subtype), "CnTrue -> CnTrue =/= true")
    //Test that CnAnd(Empty,Empty) is equivalent to CnTrue
    val alternateTrue = CnAnd(Map.empty,Map.empty,subtype)
    assert(alternateTrue.isTrue,"!CnAnd(Empty,Empty).isTrue")
    //Test that CnAnd(Empty,Empty) is equivalent to CnTrue
    val alternateTrue2 = CnOr(List(alternateTrue),subtype)
    assert(alternateTrue2.isTrue,"CnOr(CnAnd(Empty,Empty)).isTrue")
    //Test that CnOr(Empty) is equivalent to CnFalse
    val alternateFalse = CnOr(List(),subtype)
    assert(alternateFalse.isFalse,"CnOr(Empty).isFalse")
    val analyzer2 = makeAnalyzer(makeTrait("A"),makeTrait("B","A"),makeTrait("C"),makeTrait("D","C"))
    val history2 = new SubtypeHistory(analyzer2)
    def subtype2(t1: Type, t2: Type) = history2.subtypeNormal(t1,t2).isTrue
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
    val c1b = CnAnd(map1b,map1c,subtype2)
    assert(c1b.isFalse,"c <: 1 <: b is not false ")
    //Test that a satisfiable formula is not false
    val b1a = CnAnd(map1a,map1b,subtype2)
    assert(!b1a.isFalse,"b <: 1 <: a is false")
    //Test that implication works for conjunctions and disjunctions
    val bot1b = CnAnd(map1b,Map.empty,subtype2)
    val bot1a = CnAnd(map1a,Map.empty,subtype2)
    assert(bot1b.implies(bot1a,subtype2),"(1 <: B) -> (1 <: A) =/= true")
    val a1any = CnAnd(Map.empty, map1a, subtype2)
    val b1any = CnAnd(Map.empty, map1b, subtype2)
    assert(a1any.implies(b1any,subtype2),"(A <: 1) -> (B <: 1) =/= true")
    val a2any = CnAnd(Map.empty, map2a, subtype2)
    assert(!a1any.implies(a2any,subtype2),"(A <: 1) -> (A <: 2) =/= false")
    val b2any = CnAnd(Map.empty, map2b, subtype2)
    val a1anyAnda2any = a1any.scalaAnd(a2any,subtype2)
    val b1anyAndb2any = b1any.scalaAnd(b2any,subtype2)
    assert(a1anyAnda2any.implies(b1anyAndb2any,subtype2),"(A<:1 and A<:2)->(B<:1 and B<:2) =/= true")
    assert(!b1anyAndb2any.implies(a1anyAnda2any,subtype2),"(B<:1 and B<:2)->(A<:1 and A<:2) =/= false")
    val b1anyOrb2any = b1any.scalaOr(b2any,subtype2)
    assert(a1anyAnda2any.implies(b1anyOrb2any,subtype2),"(A<:1 and A<:2)->(B<:1 or B<:2) =/= true")
    val a1anyOrb1any = a1any.scalaOr(a1any,subtype2)
    assert(a1anyOrb1any.implies(b1any,subtype2),"(A<:1 or B<:1)->(B<:1) =/= true")
    assert(a1anyOrb1any.implies(a1any,subtype2),"(A<:1 or B<:1)->(A<:1) =/= false")
  }
  
  def testSolve() = {
    val analyzer = makeAnalyzer()
    val history = new SubtypeHistory(analyzer)
    def subtype(t1: Type, t2: Type) = history.subtypeNormal(t1,t2).isTrue
    assert(CnFalse.scalaSolve(Map.empty).isEmpty, "SOLVE(CnFalse) =/= None")
    assert(CnTrue.scalaSolve(Map.empty).get.isEmpty, "SOLVE(CnTrue) =/= Some(Nil)")
    val alternateTrue = CnAnd(Map.empty,Map.empty,subtype)
    val alternateTrue2 = CnOr(List(alternateTrue),subtype)
    val alternateFalse = CnOr(List(),subtype)
    assert(alternateFalse.scalaSolve(Map.empty).isEmpty, "SOLVE(CnFalse) =/= None")
    assert(alternateTrue.scalaSolve(Map.empty).get.isEmpty, "SOLVE(CnTrue) =/= Some(Nil)")
    assert(alternateTrue2.scalaSolve(Map.empty).get.isEmpty, "SOLVE(CnTrue) =/= Some(Nil)")
    val analyzer2 = makeAnalyzer(makeTrait("A"),makeTrait("B","A"),makeTrait("C"),makeTrait("D","C"))
    val history2 = new SubtypeHistory(analyzer2) 
    def subtype2(t1: Type, t2: Type) = history2.subtypeNormal(t1,t2).isTrue
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
    val b1a = CnAnd(map1a,map1b,subtype2)
    //Check that solving a contradictory formula gives you nothing
    val c1b = CnAnd(map1b,map1c,subtype2)
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
