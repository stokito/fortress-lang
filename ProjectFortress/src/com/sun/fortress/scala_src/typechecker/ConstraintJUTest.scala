/*******************************************************************************
    Copyright 2009,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.scala_src.typechecker

import _root_.junit.framework.TestCase
import com.sun.fortress.scala_src.types.TypeAnalyzer
import com.sun.fortress.nodes_util.NodeFactory._
import com.sun.fortress.nodes._
import com.sun.fortress.scala_src.useful.TypeParser

class ConstraintJUTest extends TestCase {

  def typeAnalyzer(str:String) = TypeParser.parse(TypeParser.typeAnalyzer, str).get
  def typ(str: String) = TypeParser.parse(TypeParser.typ, str).get
  
  def testAnd() = {
    val analyzer = typeAnalyzer("{}")
    //Test basic formulas
    assert(CnFalse==CnFalse.and(CnFalse,analyzer), "AND(CnFalse,CnFalse)=/=CnFalse")
    assert(CnFalse==CnFalse.and(CnTrue,analyzer), "AND(CnFalse,CnTrue)=/=CnFalse")
    assert(CnFalse==CnTrue.and(CnFalse,analyzer), "AND(CnTrue,CnFalse)=/=CnFalse")
    assert(CnTrue==CnTrue.and(CnTrue,analyzer), "AND(CnTrue,CnTrue)=/=CnTrue")
    //Test that CnAnd(Empty,Empty) acts like CnTrue
    val alternateTrue = CnAnd(Map.empty,Map.empty,analyzer)
    assert(alternateTrue.isTrue,"!CnAnd(Empty,Empty).isTrue")
    assert(alternateTrue.and(CnTrue,analyzer)==CnTrue, "AND(alternateTrue,CnTrue) =/= CnTrue")
    assert(alternateTrue.and(CnFalse,analyzer)==CnFalse, "AND(alternateTrue,CnFalse) =/= CnFalse")
    assert(CnTrue.and(alternateTrue,analyzer)==CnTrue, "AND(CnTrue, alternateTrue) =/= CnTrue")
    assert(CnFalse.and(alternateTrue,analyzer)==CnFalse, "AND(CnFalse, alternateTrue) =/= CnFalse")
    //Test that CnOr(CnAnd(Empty,Empty)) acts like CnTrue
    val alternateTrue2 = CnOr(List(CnAnd(Map.empty,Map.empty,analyzer)),analyzer)
    assert(alternateTrue2.isTrue,"CnOr(CnAnd(Empty,Empty)).isTrue")
    assert(alternateTrue2.and(CnTrue,analyzer)==CnTrue, "AND(alternateTrue2,CnTrue) =/= CnTrue")
    assert(alternateTrue2.and(CnFalse,analyzer)==CnFalse, "AND(alternateTrue2,CnFalse) =/= CnFalse")
    assert(CnTrue.and(alternateTrue2,analyzer)==CnTrue, "AND(CnTrue, alternateTrue2) =/= CnTrue")
    assert(CnFalse.and(alternateTrue2,analyzer)==CnFalse, "AND(CnFalse, alternateTrue2) =/= CnFalse")
    //Test that CnOr(Empty) acts like CnFalse
    val alternateFalse = CnOr(List(),analyzer)
    assert(alternateFalse.isFalse,"CnOr(Empty).isFalse")
    assert(alternateFalse.and(CnTrue,analyzer)==CnFalse, "AND(alternateFalse,CnTrue) =/= CnTrue")
    assert(alternateFalse.and(CnFalse,analyzer)==CnFalse, "AND(alternateFalse,CnFalse) =/= CnFalse")
    assert(CnTrue.and(alternateFalse,analyzer)==CnFalse, "AND(CnFalse, alternateFalse) =/= CnFalse")
    assert(CnFalse.and(alternateFalse,analyzer)==CnFalse, "AND(CnFalse, alternateFalse) =/= CnFalse")
    //Test test that all alternate forms are eliminated
    assert(alternateTrue.and(alternateTrue,analyzer) == CnTrue, "AND(alternateTrue,alternateTrue) =/= CnTrue")
    assert(alternateTrue.and(alternateTrue2,analyzer) == CnTrue, "AND(alternateTrue,alternateTrue2) =/= CnTrue")
    assert(alternateTrue2.and(alternateTrue,analyzer) == CnTrue, "AND(alternateTrue2,alternateTrue) =/= CnTrue")
    assert(alternateTrue2.and(alternateTrue2,analyzer) == CnTrue, "AND(alternateTrue2,alternateTrue2) =/= CnTrue")
    assert(alternateTrue.and(alternateFalse,analyzer) == CnFalse, "AND(alternateTrue,alternateFalse) =/= CnFalse")
    assert(alternateFalse.and(alternateTrue,analyzer) == CnFalse, "AND(alternateFalse,alternateTrue) =/= CnFalse")
    assert(alternateTrue2.and(alternateFalse,analyzer) == CnFalse, "AND(alternateTrue2,alternateFalse) =/= CnFalse")
    assert(alternateFalse.and(alternateTrue2,analyzer) == CnFalse, "AND(alternateFalse,alternateTrue2) =/= CnFalse")
    assert(alternateFalse.and(alternateFalse,analyzer) == CnFalse, "AND(alternateFalse,alternateFalse) =/= CnFalse")
  }

  def testOr() = {
    val analyzer = typeAnalyzer("{}")
    //Test basic formulas
    assert(CnFalse==CnFalse.or(CnFalse,analyzer), "OR(CnFalse,CnFalse)=/=CnFalse")
    assert(CnTrue==CnFalse.or(CnTrue,analyzer), "OR(CnFalse,CnTrue)=/=CnTrue")
    assert(CnTrue==CnTrue.or(CnFalse,analyzer), "OR(CnTrue,CnFalse)=/=CnTrue")
    assert(CnTrue==CnTrue.or(CnTrue,analyzer), "OR(CnTrue,CnTrue)=/=CnTrue")
    //Test that CnAnd(Empty,Empty) acts like CnTrue
    val alternateTrue = CnAnd(Map.empty,Map.empty,analyzer)
    assert(alternateTrue.or(CnTrue,analyzer)==CnTrue, "OR(alternateTrue,CnTrue) == CnTrue")
    assert(alternateTrue.or(CnFalse,analyzer)==CnTrue, "OR(alternateTrue,CnFalse) == CnTrue")
    assert(CnTrue.or(alternateTrue,analyzer)==CnTrue, "OR(CnTrue, alternateTrue) == CnTrue")
    assert(CnFalse.or(alternateTrue,analyzer)==CnTrue, "OR(CnFalse, alternateTrue) == CnTrue")
    //Test that CnOr(CnAnd(Empty,Empty)) acts like CnTrue
    val alternateTrue2 = CnOr(List(alternateTrue),analyzer)
    assert(alternateTrue2.isTrue,"CnOr(CnAnd(Empty,Empty)).isTrue")
    assert(alternateTrue2.or(CnTrue,analyzer)==CnTrue, "OR(alternateTrue2,CnTrue) == CnTrue")
    assert(alternateTrue2.or(CnFalse,analyzer)==CnTrue, "OR(alternateTrue2,CnFalse) == CnTrue")
    assert(CnTrue.or(alternateTrue2,analyzer)==CnTrue, "OR(CnTrue, alternateTrue2) == CnTrue")
    assert(CnFalse.or(alternateTrue2,analyzer)==CnTrue, "OR(CnFalse, alternateTrue2) == CnTrue")
    //Test that CnOr(Empty) acts like CnFalse
    val alternateFalse = CnOr(List(),analyzer)
    assert(alternateFalse.or(CnTrue,analyzer)==CnTrue, "OR(alternateFalse,CnTrue) == CnTrue")
    assert(alternateFalse.or(CnFalse,analyzer)==CnFalse, "OR(alternateFalse,CnFalse) == CnFalse")
    assert(CnTrue.or(alternateFalse,analyzer)==CnTrue, "OR(CnFalse, alternateFalse) == CnTrue")
    assert(CnFalse.or(alternateFalse,analyzer)==CnFalse, "OR(CnFalse, alternateFalse) == CnFalse")
    //Test test that all alternate forms are eliminated
    assert(alternateTrue.or(alternateTrue,analyzer) == CnTrue, "OR(alternateTrue,alternateTrue) =/= CnTrue")
    assert(alternateTrue.or(alternateTrue2,analyzer) == CnTrue, "OR(alternateTrue,alternateTrue2) =/= CnTrue")
    assert(alternateTrue2.or(alternateTrue,analyzer) == CnTrue, "OR(alternateTrue2,alternateTrue) =/= CnTrue")
    assert(alternateTrue2.or(alternateTrue2,analyzer) == CnTrue, "OR(alternateTrue2,alternateTrue2) =/= CnTrue")
    assert(alternateTrue.or(alternateFalse,analyzer) == CnTrue, "OR(alternateTrue,alternateFalse) =/= CnTrue")
    assert(alternateFalse.or(alternateTrue,analyzer) == CnTrue, "OR(alternateFalse,alternateTrue) =/= CnTrue")
    assert(alternateTrue2.or(alternateFalse,analyzer) == CnTrue, "OR(alternateTrue2,alternateFalse) =/= CnTrue")
    assert(alternateFalse.or(alternateTrue2,analyzer) == CnTrue, "OR(alternateFalse,alternateTrue2) =/= CnTrue")
    assert(alternateFalse.or(alternateFalse,analyzer) == CnFalse, "OR(alternateFalse,alternateFalse) =/= CnFalse")
  }

  def testImplies() = {
    val analyzer = typeAnalyzer("{}")
    //Test basic formulas
    assert(CnFalse.implies(CnFalse,analyzer), "CnFalse -> CnFalse =/= true")
    assert(CnFalse.implies(CnTrue,analyzer), "CnFalse -> CnTrue =/= true")
    assert(!CnTrue.implies(CnFalse,analyzer), "CnTrue -> CnFalse =/= false")
    assert(CnTrue.implies(CnTrue,analyzer), "CnTrue -> CnTrue =/= true")
    //Test that CnAnd(Empty,Empty) is equivalent to CnTrue
    val alternateTrue = CnAnd(Map.empty,Map.empty,analyzer)
    assert(alternateTrue.isTrue,"!CnAnd(Empty,Empty).isTrue")
    //Test that CnAnd(Empty,Empty) is equivalent to CnTrue
    val alternateTrue2 = CnOr(List(alternateTrue),analyzer)
    assert(alternateTrue2.isTrue,"CnOr(CnAnd(Empty,Empty)).isTrue")
    //Test that CnOr(Empty) is equivalent to CnFalse
    val alternateFalse = CnOr(List(),analyzer)
    assert(alternateFalse.isFalse,"CnOr(Empty).isFalse")
    val analyzer2 = typeAnalyzer("{trait Aa, trait Bb extends {Aa}, trait Cc, trait Dd extends {Cc}}")
    //Declarations
    val ivar1 = make_InferenceVarType(typeSpan)
    val ivar2 = make_InferenceVarType(typeSpan)
    val typea = typ("Aa")
    val typeb = typ("Bb")
    val typec = typ("Cc")
    val typed = typ("Dd")
    val map1a = Map.empty.updated(ivar1,typea)
    val map1b = Map.empty.updated(ivar1,typeb)
    val map1c = Map.empty.updated(ivar1,typec)
    val map1d = Map.empty.updated(ivar1,typed)
    val map2a = Map.empty.updated(ivar2,typea)
    val map2b = Map.empty.updated(ivar2,typeb)
    val map2c = Map.empty.updated(ivar2,typec)
    val map2d = Map.empty.updated(ivar2,typed)
    
    //Test that unsatisfiable formulas are equivalent to CnFalse
    val c1b = CnAnd(map1b,map1c,analyzer2)
    assert(c1b.isFalse,"c <: 1 <: b is not false ")
    //Test that a satisfiable formula is not false
    val b1a = CnAnd(map1a,map1b,analyzer2)
    assert(!b1a.isFalse,"b <: 1 <: a is false")
    //Test that implication works for conjunctions and disjunctions
    val bot1b = CnAnd(map1b,Map.empty,analyzer2)
    val bot1a = CnAnd(map1a,Map.empty,analyzer2)
    assert(bot1b.implies(bot1a,analyzer2),"(1 <: B) -> (1 <: A) =/= true")
    val a1any = CnAnd(Map.empty, map1a, analyzer2)
    val b1any = CnAnd(Map.empty, map1b, analyzer2)
    assert(a1any.implies(b1any,analyzer2),"(A <: 1) -> (B <: 1) =/= true")
    val a2any = CnAnd(Map.empty, map2a, analyzer2)
    assert(!a1any.implies(a2any,analyzer2),"(A <: 1) -> (A <: 2) =/= false")
    val b2any = CnAnd(Map.empty, map2b, analyzer2)
    val a1anyAnda2any = a1any.and(a2any,analyzer2)
    val b1anyAndb2any = b1any.and(b2any,analyzer2)
    assert(a1anyAnda2any.implies(b1anyAndb2any,analyzer2),"(A<:1 and A<:2)->(B<:1 and B<:2) =/= true")
    assert(!b1anyAndb2any.implies(a1anyAnda2any,analyzer2),"(B<:1 and B<:2)->(A<:1 and A<:2) =/= false")
    val b1anyOrb2any = b1any.or(b2any,analyzer2)
    assert(a1anyAnda2any.implies(b1anyOrb2any,analyzer2),"(A<:1 and A<:2)->(B<:1 or B<:2) =/= true")
    val a1anyOrb1any = a1any.or(a1any,analyzer2)
    assert(a1anyOrb1any.implies(b1any,analyzer2),"(A<:1 or B<:1)->(B<:1) =/= true")
    assert(a1anyOrb1any.implies(a1any,analyzer2),"(A<:1 or B<:1)->(A<:1) =/= false")
  }
  
  def testSolve() = {
    val analyzer = typeAnalyzer("{ }")
    assert(CnFalse.solve(Map.empty).isEmpty, "SOLVE(CnFalse) =/= None")
    assert(CnTrue.solve(Map.empty).get.isEmpty, "SOLVE(CnTrue) =/= Some(Nil)")
    val alternateTrue = CnAnd(Map.empty,Map.empty,analyzer)
    val alternateTrue2 = CnOr(List(alternateTrue),analyzer)
    val alternateFalse = CnOr(List(),analyzer)
    assert(alternateFalse.solve(Map.empty).isEmpty, "SOLVE(CnFalse) =/= None")
    assert(alternateTrue.solve(Map.empty).get.isEmpty, "SOLVE(CnTrue) =/= Some(Nil)")
    assert(alternateTrue2.solve(Map.empty).get.isEmpty, "SOLVE(CnTrue) =/= Some(Nil)")
    val analyzer2 = typeAnalyzer("{trait Aa, trait Bb extends {Aa}, trait Cc, trait Dd extends {Cc}}")
        //Declarations
    val ivar1 = make_InferenceVarType(typeSpan)
    val ivar2 = make_InferenceVarType(typeSpan)
    val typea = typ("Aa")
    val typeb = typ("Bb")
    val typec = typ("Cc")
    val typed = typ("Dd")
    val map1a = Map.empty.updated(ivar1,typea)
    val map1b = Map.empty.updated(ivar1,typeb)
    val map1c = Map.empty.updated(ivar1,typec)
    val map1d = Map.empty.updated(ivar1,typed)
    val map2a = Map.empty.updated(ivar2,typea)
    val map2b = Map.empty.updated(ivar2,typeb)
    val map2c = Map.empty.updated(ivar2,typec)
    val map2d = Map.empty.updated(ivar2,typed)
    val b1a = CnAnd(map1a,map1b,analyzer2)
    //Check that solving a contradictory formula gives you nothing
    val c1b = CnAnd(map1b,map1c,analyzer2)
    val solved_c1b = c1b.solve(Map.empty)
    //Check that solving a constraint with no bounds works
    assert(solved_c1b.isEmpty,"solve(C<:1<:B, {}) is not empty")
    val solved_b1a = b1a.solve(Map.empty)
    assert(solved_b1a.isDefined && solved_b1a.get==map1b,"SOLVE(B<:1<:A , {}) = 1:=B")
    //Check that if the solution to a constraint is out of bounds it fails
    val solved_b1a_1c = b1a.solve(map1c)
    assert(solved_b1a_1c.isEmpty,"Solve(B<:1<:A, {1<:C}) is not empty")
    //Check that if the solution to a constraint is in bounds it succeeds
    val solved_b1a_1a = b1a.solve(map1a)
    assert(solved_b1a_1a.isDefined && solved_b1a_1a.get==map1b,"SOLVE(B<:1<:A, {1<:A}) = 1:=B")
  }
}
