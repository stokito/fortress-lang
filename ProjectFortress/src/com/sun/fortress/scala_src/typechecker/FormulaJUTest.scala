/*******************************************************************************
    Copyright 2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.scala_src.typechecker

import _root_.junit.framework._
import _root_.junit.framework.Assert._
import com.sun.fortress.nodes._
import com.sun.fortress.nodes_util.NodeFactory._
import com.sun.fortress.scala_src.nodes._
import com.sun.fortress.scala_src.typechecker.Formula._
import com.sun.fortress.scala_src.types.TypeAnalyzer
import com.sun.fortress.scala_src.useful.TypeParser

class FormulaJUTest extends TestCase {

  def typeAnalyzer(str:String) = TypeParser.parse(TypeParser.typeAnalyzer, str).get
  def typ(str: String) = TypeParser.parse(TypeParser.typ, str).get
  def nTyp(str: String) = TypeParser.parse(TypeParser.namedType, str).get
  
  def testAnd() = {
    implicit val analyzer = typeAnalyzer("{}")
    //Test basic formulas
    assertTrue(False==and(False.asInstanceOf[CFormula], False.asInstanceOf[CFormula]))
    assertTrue(False==and(False.asInstanceOf[CFormula], True.asInstanceOf[CFormula]))
    assertTrue(False==and(True.asInstanceOf[CFormula], False.asInstanceOf[CFormula]))
    assertTrue(True==and(True.asInstanceOf[CFormula], True.asInstanceOf[CFormula]))
    //Test that And(Empty,Empty) acts like True
    val alternateTrue = And(Map(), Map())
    assertTrue(isTrue(alternateTrue))
    assertTrue(and(alternateTrue, True)==True)
    assertTrue(and(alternateTrue, False)==False)
    assertTrue(and(True, alternateTrue)==True)
    assertTrue(and(False, alternateTrue)==False)
    //Test that Or(And(Empty,Empty)) acts like True
    val alternateTrue2 = Or(Set(alternateTrue))
    assertTrue(isTrue(alternateTrue2))
    assertTrue(and(alternateTrue2, True)==True)
    assertTrue(and(alternateTrue2, False)==False)
    assertTrue(and(True, alternateTrue2)==True)
    assertTrue(and(False, alternateTrue2)==False)
    //Test that Or(Empty) acts like False
    val alternateFalse = Or(Set())
    assertTrue(isFalse(alternateFalse))
    assertTrue(and(alternateFalse, True)==False)
    assertTrue(and(alternateFalse, False)==False)
    assertTrue(and(True, alternateFalse)==False)
    assertTrue(and(False, alternateFalse)==False)
    //Test test that all alternate forms are eliminated
    assertTrue(and(alternateTrue, alternateTrue) == True)
    assertTrue(and(alternateTrue, alternateTrue2) == True)
    assertTrue(and(alternateTrue2, alternateTrue) == True)
    assertTrue(and(alternateTrue2, alternateTrue2) == True)
    assertTrue(and(alternateTrue, alternateFalse) == False)
    assertTrue(and(alternateFalse, alternateTrue) == False)
    assertTrue(and(alternateTrue2, alternateFalse) == False)
    assertTrue(and(alternateFalse, alternateTrue2) == False)
    assertTrue(and(alternateFalse, alternateFalse) == False)
  }

  def testOr() = {
    implicit val analyzer = typeAnalyzer("{}")
    //Test basic formulas
    assertTrue(False==or(False.asInstanceOf[CFormula], False.asInstanceOf[CFormula]))
    assertTrue(True==or(False.asInstanceOf[CFormula], True.asInstanceOf[CFormula]))
    assertTrue(True==or(True.asInstanceOf[CFormula], False.asInstanceOf[CFormula]))
    assertTrue(True==or(True.asInstanceOf[CFormula], True.asInstanceOf[CFormula]))
    //Test that And(Empty,Empty) acts like True
    val alternateTrue = And(Map(), Map())
    assertTrue(or(alternateTrue, True)==True)
    assertTrue(or(alternateTrue, False)==True)
    assertTrue(or(True, alternateTrue)==True)
    assertTrue(or(False, alternateTrue)==True)
    //Test that Or(And(Empty,Empty)) acts like True
    val alternateTrue2 = Or(Set(alternateTrue))
    assertTrue(isTrue(alternateTrue2))
    assertTrue(or(alternateTrue2, True)==True)
    assertTrue(or(alternateTrue2, False)==True)
    assertTrue(or(True, alternateTrue2)==True)
    assertTrue(or(False, alternateTrue2)==True)
    //Test that Or(Empty) acts like False
    val alternateFalse = Or(Set())
    assertTrue(or(alternateFalse, True)==True)
    assertTrue(or(alternateFalse, False)==False)
    assertTrue(or(True, alternateFalse)==True)
    assertTrue(or(False, alternateFalse)==False)
    //Test test that all alternate forms are eliminated
    assertTrue(or(alternateTrue, alternateTrue) == True)
    assertTrue(or(alternateTrue, alternateTrue2) == True)
    assertTrue(or(alternateTrue2, alternateTrue) == True)
    assertTrue(or(alternateTrue2, alternateTrue2) == True)
    assertTrue(or(alternateTrue, alternateFalse) == True)
    assertTrue(or(alternateFalse, alternateTrue) == True)
    assertTrue(or(alternateTrue2, alternateFalse) == True)
    assertTrue(or(alternateFalse, alternateTrue2) == True)
    assertTrue(or(alternateFalse, alternateFalse) == False)
  }

  def testImplies() = {
    {
      implicit val analyzer = typeAnalyzer("{}")
      //Test basic formulas
      assertTrue(implies(False.asInstanceOf[CFormula], False.asInstanceOf[CFormula]))
      assertTrue(implies(False.asInstanceOf[CFormula], True.asInstanceOf[CFormula]))
      assertTrue(!implies(True.asInstanceOf[CFormula], False.asInstanceOf[CFormula]))
      assertTrue(implies(True.asInstanceOf[CFormula], True.asInstanceOf[CFormula]))
      //Test that And(Empty,Empty) is equivalent to True
      val alternateTrue = And(Map(), Map())
      assertTrue(isTrue(alternateTrue))
      //Test that And(Empty,Empty) is equivalent to True
      val alternateTrue2 = Or(Set(alternateTrue))
      assertTrue(isTrue(alternateTrue2))
      //Test that Or(Empty) is equivalent to False
      val alternateFalse = Or(Set())
      assertTrue(isFalse(alternateFalse))
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
      //Test that unsatisfiable formulas are equivalent to False
      val c1b = And(Map(ivar1 -> TPrimitive(typeb, Set(), typec, Set(), Set(), Set())), Map())
      assertTrue(isFalse(c1b))
      //Test that a satisfiable formula is not false
      val b1a = And(Map(ivar1 -> TPrimitive(typeb, Set(), typea, Set(), Set(), Set())), Map())
      assertTrue(!isFalse(b1a))
      //Test that implication works for conjunctions and disjunctions
      val bot1b = And(Map(ivar1 -> TPrimitive(Set(), Set(), typeb, Set(), Set(), Set())), Map())
      val bot1a = And(Map(ivar1 -> TPrimitive(Set(), Set(), typea, Set(), Set(), Set())), Map())
      assertTrue(implies(bot1b, bot1a))
      val a1any = And(Map(ivar1 -> TPrimitive(typea, Set(), Set(), Set(), Set(), Set())), Map())
      val b1any = And(Map(ivar1 -> TPrimitive(typeb, Set(), Set(), Set(), Set(), Set())), Map())
      assertTrue(implies(a1any, b1any))
      val a2any = And(Map(ivar2 -> TPrimitive(typea, Set(), Set(), Set(), Set(), Set())), Map())
      assertTrue(!implies(a1any, a2any))
      val b2any = And(Map(ivar2 -> TPrimitive(typeb, Set(), Set(), Set(), Set(), Set())), Map())
      val a1anyAnda2any = and(a1any, a2any)
      val b1anyAndb2any = and(b1any, b2any)
      assertTrue(implies(a1anyAnda2any, b1anyAndb2any))
      assertTrue(!implies(b1anyAndb2any, a1anyAnda2any))
      val b1anyOrb2any = or(b1any, b2any)
      assertTrue(implies(a1anyAnda2any, b1anyOrb2any))
      val a1anyOrb1any = or(a1any, a1any)
      assertTrue(implies(a1anyOrb1any, b1any))
      assertTrue(implies(a1anyOrb1any, a1any))
    }
  }
  
  def testReduce(){
    {
      implicit val ta = typeAnalyzer("{}")
      val ivar = make_InferenceVarType(typeSpan)
      val alternateTrue = And(Map(ivar -> TPrimitive(Set(ivar), Set(), Set(), Set(), Set(), Set())), Map())
      assertTrue(reduce(alternateTrue)==True)
      val alternateTrue2 = And(Map(ivar -> TPrimitive(Set(), Set(), Set(ivar), Set(), Set(), Set())), Map())
      assertTrue(reduce(alternateTrue2)==True)
    }
  }
  
  
  def testSolve() = {
    {
      implicit val analyzer = typeAnalyzer("{ }")
      val tans = Some((tEmptySub, oEmptySub))
      assertTrue(solve(False) == None)
      assertTrue(solve(True) ==  tans)
      val alternateTrue = And(Map(), Map())
      val alternateTrue2 = Or(Set(alternateTrue))
      val alternateFalse = Or(Set())
      val t = solve(alternateFalse)
      assertTrue(t == None)
      assertTrue(solve(alternateTrue) == tans)
      assertTrue(solve(alternateTrue2) == tans)
    }
    {
      implicit val analyzer = typeAnalyzer("{trait Aa, trait Bb extends {Aa}, trait Cc, trait Dd extends {Cc}}")
          //Declarations
      val ivar1 = make_InferenceVarType(typeSpan)
      val ivar2 = make_InferenceVarType(typeSpan)
      val typea = typ("Aa")
      val typeb = typ("Bb")
      val typec = typ("Cc")
      val typed = typ("Dd")
      assertTrue(analyzer.equiv(typea, typea)) 
      // Check that unification works
      val id = (x: Type) => x
      val a1 = And(Map(ivar1 -> TPrimitive(Set(typea), Set(), Set(typea), Set(), Set(), Set())), Map())
      assertTrue(solve(a1).map(_._1).getOrElse(id)(ivar1) == typea)
      val b1a = And(Map(ivar1 -> TPrimitive(Set(typeb), Set(), Set(typea), Set(), Set(), Set())), Map())
      //Check that solving a contradictory formula gives you nothing
      val c1b = And(Map(ivar1 -> TPrimitive(Set(typec), Set(), Set(typeb), Set(), Set(), Set())), Map())
      //Check that solving a constraint with no bounds works
      assertTrue(solve(c1b) == None)
      assertTrue(solve(b1a).map(_._1).getOrElse(id)(ivar1) == typeb)
      //Check that if the solution to a constraint is out of bounds it fails
      val outOfBounds = and(b1a, And(Map(ivar1 -> TPrimitive(Set(), Set(), Set(typec), Set(), Set(), Set())), Map()))
      assertTrue(solve(outOfBounds) == None)
      //Check that if the solution to a constraint is in bounds it succeeds
      val inBounds = and(b1a, And(Map(ivar1 -> TPrimitive(Set(), Set(), Set(typea), Set(), Set(), Set())), Map()))
      assertTrue(solve(inBounds).map(_._1).getOrElse(id)(ivar1) == typeb)
    }
    {
      val ivar1 = make_InferenceVarType(typeSpan)
      val typea = nTyp("Zz")
      val typeb = nTyp("T")
      val typec = nTyp("Eq[T]")
      val sp = SStaticParam(typeb.getInfo, 0, typeb.getName, List(typec), List(typec), None, false, SKindType(), false)
      implicit val analyzer = typeAnalyzer("{trait Eq[T extends {Eq[T]}], trait Zz extends {Eq[Zz]}}").extend(List(sp), None)
      val c = and(analyzer.equivalent(ivar1, typeb), analyzer.subtype(ivar1, typea))
      assertTrue(solve(c) == None)
    }
    {
      val typea = typ("i")
      val typeb = typ("j")
      implicit val analyzer = typeAnalyzer("{}")
      val c = analyzer.equivalent(typea, typeb)
      val sc = solve(c)
      assertTrue(!sc.isEmpty)
      val s = sc.get._1
      assertTrue(s(typea) == s(typeb))
    }
  } 
}
