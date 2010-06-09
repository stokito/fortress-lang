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

package com.sun.fortress.scala_src.typechecker

import com.sun.fortress.compiler.typechecker.TypeAnalyzerJUTest._
import _root_.junit.framework.TestCase
import com.sun.fortress.scala_src.types.TypeAnalyzer
import com.sun.fortress.nodes_util.NodeFactory._
import com.sun.fortress.nodes._
import com.sun.fortress.scala_src.useful.TypeParser
import com.sun.fortress.scala_src.typechecker.Formula._

class FormulaJUTest extends TestCase {

  def typeAnalyzer(str:String) = TypeParser.parse(TypeParser.typeAnalyzer, str).get
  def typ(str: String) = TypeParser.parse(TypeParser.typ, str).get
  
  def testAnd() = {
    implicit val analyzer = typeAnalyzer("{}")
    //Test basic formulas
    assert(False==and(False.asInstanceOf[CFormula], False.asInstanceOf[CFormula]), "AND(False,False)=/=False")
    assert(False==and(False.asInstanceOf[CFormula], True.asInstanceOf[CFormula]), "AND(False,True)=/=False")
    assert(False==and(True.asInstanceOf[CFormula], False.asInstanceOf[CFormula]), "AND(True,False)=/=False")
    assert(True==and(True.asInstanceOf[CFormula], True.asInstanceOf[CFormula]), "AND(True,True)=/=True")
    //Test that And(Empty,Empty) acts like True
    val alternateTrue = And(Map(),Map())
    assert(isTrue(alternateTrue),"!And(Empty,Empty).isTrue")
    assert(and(alternateTrue, True)==True, "AND(alternateTrue,True) =/= True")
    assert(and(alternateTrue, False)==False, "AND(alternateTrue,False) =/= False")
    assert(and(True, alternateTrue)==True, "AND(True, alternateTrue) =/= True")
    assert(and(False, alternateTrue)==False, "AND(False, alternateTrue) =/= False")
    //Test that Or(And(Empty,Empty)) acts like True
    val alternateTrue2 = Or(Set(And(Map(),Map())))
    assert(isTrue(alternateTrue2),"Or(And(Empty,Empty)).isTrue")
    assert(and(alternateTrue2, True)==True, "AND(alternateTrue2,True) =/= True")
    assert(and(alternateTrue2, False)==False, "AND(alternateTrue2,False) =/= False")
    assert(and(True, alternateTrue2)==True, "AND(True, alternateTrue2) =/= True")
    assert(and(False, alternateTrue2)==False, "AND(False, alternateTrue2) =/= False")
    //Test that Or(Empty) acts like False
    val alternateFalse = Or(Set())
    assert(isFalse(alternateFalse),"Or(Empty).isFalse")
    assert(and(alternateFalse, True)==False, "AND(alternateFalse,True) =/= True")
    assert(and(alternateFalse, False)==False, "AND(alternateFalse,False) =/= False")
    assert(and(True, alternateFalse)==False, "AND(False, alternateFalse) =/= False")
    assert(and(False, alternateFalse)==False, "AND(False, alternateFalse) =/= False")
    //Test test that all alternate forms are eliminated
    assert(and(alternateTrue, alternateTrue) == True, "AND(alternateTrue,alternateTrue) =/= True")
    assert(and(alternateTrue, alternateTrue2) == True, "AND(alternateTrue,alternateTrue2) =/= True")
    assert(and(alternateTrue2, alternateTrue) == True, "AND(alternateTrue2,alternateTrue) =/= True")
    assert(and(alternateTrue2, alternateTrue2) == True, "AND(alternateTrue2,alternateTrue2) =/= True")
    assert(and(alternateTrue, alternateFalse) == False, "AND(alternateTrue,alternateFalse) =/= False")
    assert(and(alternateFalse, alternateTrue) == False, "AND(alternateFalse,alternateTrue) =/= False")
    assert(and(alternateTrue2, alternateFalse) == False, "AND(alternateTrue2,alternateFalse) =/= False")
    assert(and(alternateFalse, alternateTrue2) == False, "AND(alternateFalse,alternateTrue2) =/= False")
    assert(and(alternateFalse, alternateFalse) == False, "AND(alternateFalse,alternateFalse) =/= False")
  }

  def testOr() = {
    implicit val analyzer = typeAnalyzer("{}")
    //Test basic formulas
    assert(False==or(False, False), "OR(False,False)=/=False")
    assert(True==or(False, True), "OR(False,True)=/=True")
    assert(True==or(True, False), "OR(True,False)=/=True")
    assert(True==or(True, True), "OR(True,True)=/=True")
    //Test that And(Empty,Empty) acts like True
    val alternateTrue = And(Map(),Map())
    assert(or(alternateTrue, True)==True, "OR(alternateTrue,True) == True")
    assert(or(alternateTrue, False)==True, "OR(alternateTrue,False) == True")
    assert(or(True, alternateTrue)==True, "OR(True, alternateTrue) == True")
    assert(or(False, alternateTrue)==True, "OR(False, alternateTrue) == True")
    //Test that Or(And(Empty,Empty)) acts like True
    val alternateTrue2 = Or(Set(alternateTrue))
    assert(isTrue(alternateTrue2),"Or(And(Empty,Empty)).isTrue")
    assert(or(alternateTrue2, True)==True, "OR(alternateTrue2,True) == True")
    assert(or(alternateTrue2, False)==True, "OR(alternateTrue2,False) == True")
    assert(or(True, alternateTrue2)==True, "OR(True, alternateTrue2) == True")
    assert(or(False, alternateTrue2)==True, "OR(False, alternateTrue2) == True")
    //Test that Or(Empty) acts like False
    val alternateFalse = Or(Set())
    assert(or(alternateFalse, True)==True, "OR(alternateFalse,True) == True")
    assert(or(alternateFalse, False)==False, "OR(alternateFalse,False) == False")
    assert(or(True, alternateFalse)==True, "OR(False, alternateFalse) == True")
    assert(or(False, alternateFalse)==False, "OR(False, alternateFalse) == False")
    //Test test that all alternate forms are eliminated
    assert(or(alternateTrue, alternateTrue) == True, "OR(alternateTrue,alternateTrue) =/= True")
    assert(or(alternateTrue, alternateTrue2) == True, "OR(alternateTrue,alternateTrue2) =/= True")
    assert(or(alternateTrue2, alternateTrue) == True, "OR(alternateTrue2,alternateTrue) =/= True")
    assert(or(alternateTrue2, alternateTrue2) == True, "OR(alternateTrue2,alternateTrue2) =/= True")
    assert(or(alternateTrue, alternateFalse) == True, "OR(alternateTrue,alternateFalse) =/= True")
    assert(or(alternateFalse, alternateTrue) == True, "OR(alternateFalse,alternateTrue) =/= True")
    assert(or(alternateTrue2, alternateFalse) == True, "OR(alternateTrue2,alternateFalse) =/= True")
    assert(or(alternateFalse, alternateTrue2) == True, "OR(alternateFalse,alternateTrue2) =/= True")
    assert(or(alternateFalse, alternateFalse) == False, "OR(alternateFalse,alternateFalse) =/= False")
  }

  def testImplies() = {
    {
      implicit val analyzer = typeAnalyzer("{}")
      //Test basic formulas
      assert(implies(False.asInstanceOf[CFormula], False.asInstanceOf[CFormula]),
             "False -> False =/= true")
      assert(implies(False.asInstanceOf[CFormula], True.asInstanceOf[CFormula]),
             "False -> True =/= true")
      assert(!implies(True.asInstanceOf[CFormula], False.asInstanceOf[CFormula]),
             "True -> False =/= false")
      assert(implies(True.asInstanceOf[CFormula], True.asInstanceOf[CFormula]),
             "True -> True =/= true")
      //Test that And(Empty,Empty) is equivalent to True
      val alternateTrue = And(Map(),Map())
      assert(isTrue(alternateTrue),"!And(Empty,Empty).isTrue")
      //Test that And(Empty,Empty) is equivalent to True
      val alternateTrue2 = Or(Set(alternateTrue))
      assert(isTrue(alternateTrue2),"Or(And(Empty,Empty)).isTrue")
      //Test that Or(Empty) is equivalent to False
      val alternateFalse = Or(Set())
      assert(isFalse(alternateFalse),"Or(Empty).isFalse")
    }
    {
      implicit val analyzer = typeAnalyzer("{trait Aa, trait Bb extends {Aa}, trait Cc, trait Dd extends {Cc}}")
      //Declarations
      val ivar1 = make_InferenceVarType(typeSpan)
      val ivar2 = make_InferenceVarType(typeSpan)
      val typea = Set(typ("Aa"))
      val typeb = Set(typ("Bb"))
      val typec = Set(typ("Cc"))
      val typed = Set(typ("Dd"))
      val map1a = Map().updated(ivar1,typea)
      val map1b = Map().updated(ivar1,typeb)
      val map1c = Map().updated(ivar1,typec)
      val map1d = Map().updated(ivar1,typed)
      val map2a = Map().updated(ivar2,typea)
      val map2b = Map().updated(ivar2,typeb)
      val map2c = Map().updated(ivar2,typec)
      val map2d = Map().updated(ivar2,typed)
      
      //Test that unsatisfiable formulas are equivalent to False
      val c1b = And(map1b,map1c)
      assert(isFalse(c1b),"c <: 1 <: b is not false ")
      //Test that a satisfiable formula is not false
      val b1a = And(map1b,map1a)
      assert(!isFalse(b1a),"b <: 1 <: a is false")
      //Test that implication works for conjunctions and disjunctions
      val bot1b = And(Map(), map1b)
      val bot1a = And(Map(), map1a)
      assert(implies(bot1b, bot1a),"(1 <: B) -> (1 <: A) =/= true")
      val a1any = And(map1a, Map())
      val b1any = And(map1b, Map())
      assert(implies(a1any, b1any),"(A <: 1) -> (B <: 1) =/= true")
      val a2any = And(map2a, Map())
      assert(!implies(a1any, a2any),"(A <: 1) -> (A <: 2) =/= false")
      val b2any = And(map2b, Map())
      val a1anyAnda2any = and(a1any, a2any)
      val b1anyAndb2any = and(b1any, b2any)
      assert(implies(a1anyAnda2any, b1anyAndb2any),"(A<:1 and A<:2)->(B<:1 and B<:2) =/= true")
      assert(!implies(b1anyAndb2any, a1anyAnda2any),"(B<:1 and B<:2)->(A<:1 and A<:2) =/= false")
      val b1anyOrb2any = or(b1any, b2any)
      assert(implies(a1anyAnda2any, b1anyOrb2any),"(A<:1 and A<:2)->(B<:1 or B<:2) =/= true")
      val a1anyOrb1any = or(a1any, a1any)
      assert(implies(a1anyOrb1any, b1any),"(A<:1 or B<:1)->(B<:1) =/= true")
      assert(implies(a1anyOrb1any, a1any),"(A<:1 or B<:1)->(A<:1) =/= false")
    }
  }
  
  def testReduce(){
    {
      implicit val ta = typeAnalyzer("{}")
      val ivar = make_InferenceVarType(typeSpan)
      assert(reduce(And(Map(ivar -> Set[Type](ivar)), Map()))==True)
      assert(reduce(And(Map(), Map(ivar -> Set[Type](ivar))))==True)
    }
  }
  
  /*
  def testSolve() = {
    {
      val analyzer = typeAnalyzer("{ }")
      assert(False.solve(Map()).isEmpty, "SOLVE(False) =/= None")
      assert(True.solve(Map()).get.isEmpty, "SOLVE(True) =/= Some(Nil)")
      val alternateTrue = And(Map(),Map())
      val alternateTrue2 = Or(List(alternateTrue))
      val alternateFalse = Or(List())
      assert(alternateFalse.solve(Map()).isEmpty, "SOLVE(False) =/= None")
      assert(alternateTrue.solve(Map()).get.isEmpty, "SOLVE(True) =/= Some(Nil)")
      assert(alternateTrue2.solve(Map()).get.isEmpty, "SOLVE(True) =/= Some(Nil)")
    }
    {
      val analyzer = typeAnalyzer("{trait Aa, trait Bb extends {Aa}, trait Cc, trait Dd extends {Cc}}")
          //Declarations
      val ivar1 = make_InferenceVarType(typeSpan)
      val ivar2 = make_InferenceVarType(typeSpan)
      val typea = typ("Aa")
      val typeb = typ("Bb")
      val typec = typ("Cc")
      val typed = typ("Dd")
      val map1a = Map().updated(ivar1,typea)
      val map1b = Map().updated(ivar1,typeb)
      val map1c = Map().updated(ivar1,typec)
      val map1d = Map().updated(ivar1,typed)
      val map2a = Map().updated(ivar2,typea)
      val map2b = Map().updated(ivar2,typeb)
      val map2c = Map().updated(ivar2,typec)
      val map2d = Map().updated(ivar2,typed)
      val b1a = And(map1a,map1b)
      //Check that solving a contradictory formula gives you nothing
      val c1b = And(map1b,map1c)
      val solved_c1b = c1b.solve(Map())
      //Check that solving a constraint with no bounds works
      assert(solved_c1b.isEmpty,"solve(C<:1<:B, {}) is not empty")
      val solved_b1a = b1a.solve(Map())
      assert(solved_b1a.isDefined && solved_b1a.get==map1b,"SOLVE(B<:1<:A , {}) = 1:=B")
      //Check that if the solution to a constraint is out of bounds it fails
      val solved_b1a_1c = b1a.solve(map1c)
      assert(solved_b1a_1c.isEmpty,"Solve(B<:1<:A, {1<:C}) is not empty")
      //Check that if the solution to a constraint is in bounds it succeeds
      val solved_b1a_1a = b1a.solve(map1a)
      assert(solved_b1a_1a.isDefined && solved_b1a_1a.get==map1b,"SOLVE(B<:1<:A, {1<:A}) = 1:=B")
    }
  } */
}
