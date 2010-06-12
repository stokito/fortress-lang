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
import junit.framework._
import com.sun.fortress.scala_src.types.TypeAnalyzer
import com.sun.fortress.nodes_util.NodeFactory._
import com.sun.fortress.nodes._
import com.sun.fortress.scala_src.useful.TypeParser
import com.sun.fortress.scala_src.typechecker.Formula._
import Assert._

class FormulaJUTest extends TestCase {

  def typeAnalyzer(str:String) = TypeParser.parse(TypeParser.typeAnalyzer, str).get
  def typ(str: String) = TypeParser.parse(TypeParser.typ, str).get
  
  def testAnd() = {
    implicit val analyzer = typeAnalyzer("{}")
    //Test basic formulas
    assertTrue(False==and(False.asInstanceOf[CFormula], False.asInstanceOf[CFormula]))
    assertTrue(False==and(False.asInstanceOf[CFormula], True.asInstanceOf[CFormula]))
    assertTrue(False==and(True.asInstanceOf[CFormula], False.asInstanceOf[CFormula]))
    assertTrue(True==and(True.asInstanceOf[CFormula], True.asInstanceOf[CFormula]))
    //Test that And(Empty,Empty) acts like True
    val alternateTrue = And(Map(),Map())
    assertTrue(isTrue(alternateTrue))
    assertTrue(and(alternateTrue, True)==True)
    assertTrue(and(alternateTrue, False)==False)
    assertTrue(and(True, alternateTrue)==True)
    assertTrue(and(False, alternateTrue)==False)
    //Test that Or(And(Empty,Empty)) acts like True
    val alternateTrue2 = Or(Set(And(Map(),Map())))
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
    val alternateTrue = And(Map(),Map())
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
      val alternateTrue = And(Map(),Map())
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
      assertTrue(isFalse(c1b))
      //Test that a satisfiable formula is not false
      val b1a = And(map1b,map1a)
      assertTrue(!isFalse(b1a))
      //Test that implication works for conjunctions and disjunctions
      val bot1b = And(Map(), map1b)
      val bot1a = And(Map(), map1a)
      assertTrue(implies(bot1b, bot1a))
      val a1any = And(map1a, Map())
      val b1any = And(map1b, Map())
      assertTrue(implies(a1any, b1any))
      val a2any = And(map2a, Map())
      assertTrue(!implies(a1any, a2any))
      val b2any = And(map2b, Map())
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
      assertTrue(reduce(And(Map(ivar -> Set[Type](ivar)), Map()))==True)
      assertTrue(reduce(And(Map(), Map(ivar -> Set[Type](ivar))))==True)
    }
  }
  
  
  def testSolve() = {
    {
      implicit val analyzer = typeAnalyzer("{ }")
      val tans = Some(Substitution(Map()))
      assertTrue(solve(False) == None)
      assertTrue(solve(True) ==  tans)
      val alternateTrue = And(Map(),Map())
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
      val map1a = Map((ivar1, Set(typea)))
      val map1b = Map((ivar1, Set(typeb)))
      val map1c = Map((ivar1, Set(typec)))
      val map1d = Map((ivar1, Set(typed)))
      val map2a = Map((ivar2, Set(typea)))
      val map2b = Map((ivar2, Set(typeb)))
      val map2c = Map((ivar2, Set(typec)))
      val map2d = Map((ivar2, Set(typed)))
      // Check that unification works
      val id = (x: Type) => x
      assertTrue(solve(And(map1a, map1a)).getOrElse(id)(ivar1) == typea)
      val b1a = And(map1b, map1a)
      //Check that solving a contradictory formula gives you nothing
      val c1b = And(map1c, map1b)
      //Check that solving a constraint with no bounds works
      assertTrue(solve(c1b) == None)
      assertTrue(solve(b1a).getOrElse(id)(ivar1) == typeb)
      //Check that if the solution to a constraint is out of bounds it fails
      assertTrue(solve(and(b1a, And(Map(), map1c))) == None)
      //Check that if the solution to a constraint is in bounds it succeeds
      assertTrue(solve(and(b1a, And(Map(), map1a))).getOrElse(id)(ivar1) == typeb)
    }
  } 
}
