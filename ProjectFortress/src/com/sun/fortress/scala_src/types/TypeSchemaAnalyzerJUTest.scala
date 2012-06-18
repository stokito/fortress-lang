/*******************************************************************************
    Copyright 2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.scala_src.types

import _root_.junit.framework._
import _root_.junit.framework.Assert._
import com.sun.fortress.nodes._
import com.sun.fortress.scala_src.nodes._
import com.sun.fortress.scala_src.useful.STypesUtil._
import com.sun.fortress.scala_src.useful.TypeParser
import com.sun.fortress.nodes_util.{NodeFactory => NF}

class TypeSchemaAnalyzerJUTest extends TestCase {
  
  def typeAnalyzer(str:String) = TypeParser.parse(TypeParser.typeAnalyzer, str).get
  def overloadingSet(str: String) = TypeParser.parse(TypeParser.overloadingSet, str).get
  def typeSchema(str: String) = TypeParser.parse(TypeParser.typeSchema, str).get
  def typeSchemaAnalyzer(str: String) = new TypeSchemaAnalyzer()(typeAnalyzer(str))
  
  val basicTsa = typeSchemaAnalyzer("""{
    trait Aa,
    trait Bb extends {Aa},
    trait Eq[T],
    trait List[T],
    trait Array[T],
    trait ArrayList[T] extends {List[T], Array[T]},
    trait Zz extends {Eq[Zz]},
    trait String extends {Eq[String]} excludes {Zz}
  }""")
  
  def testRenaming() = {
    val tsa = typeSchemaAnalyzer("{trait Eq[T]}")
    
    {
      val t1 = typeSchema("[T]T")
      val t2 = typeSchema("[U]U")
      assertTrue(tsa.subtypeED(t1, t2))
      assertTrue(tsa.subtypeED(t2, t1))
    }
    
    {
      val t1 = typeSchema("[T extends {Eq[T]}]T")
      val t2 = typeSchema("[U extends {Eq[U]}]U")
      assertTrue(tsa.subtypeED(t1, t2))
      assertTrue(tsa.subtypeED(t2, t1))
    }
    
    {
      val t1 = typeSchema("[T extends {Eq[U]}, U extends {Eq[T]}](T, U)")
      val t2 = typeSchema("[A extends {Eq[B]}, B extends {Eq[A]}](A, B)")
      assertTrue(tsa.subtypeED(t1, t2))
      assertTrue(tsa.subtypeED(t2, t1))
    }
  }
  
  def testSubtypeUA() = {
    val tsa = basicTsa
    
    {
      val t1 = typeSchema("[T]() -> T").asInstanceOf[ArrowType]
      val t2 = typeSchema("() -> Object").asInstanceOf[ArrowType]
      assertTrue(tsa.subtypeUA(t1, t2))
      assertFalse(tsa.subtypeUA(t2, t1))
    }
    
    {
      val t1 = typeSchema("[T] List[T] -> ()").asInstanceOf[ArrowType]
      val t2 = typeSchema("[T] ArrayList[T] -> ()").asInstanceOf[ArrowType]
      assertTrue(tsa.subtypeUA(t1, t2))
      assertFalse(tsa.subtypeUA(t2, t1))
    }
    
    {
      val t1 = typeSchema("[T extends {Eq[T]}]() -> List[T]").asInstanceOf[ArrowType]
      val t2 = typeSchema("[T extends {Zz}]() -> List[T]").asInstanceOf[ArrowType]
      val t3 = typeSchema("() -> List[Zz]").asInstanceOf[ArrowType]
      assertFalse(tsa.subtypeUA(t1, t2))
      assertFalse(tsa.subtypeUA(t2, t1))
      
      assertTrue(tsa.subtypeUA(t1, t3))
      assertFalse(tsa.subtypeUA(t3, t1))
      
      assertTrue(tsa.subtypeUA(t2, t3))
      assertFalse(tsa.subtypeUA(t3, t2))
    }
  }
  
  
  def testSubtypeED() = {
    val tsa = basicTsa
    
    {
      val t1 = typeSchema("[T]T")
      val t2 = typeSchema("Object")
      assertTrue(tsa.subtypeED(t1, t2))
      assertTrue(tsa.subtypeED(t2, t1))
    }
    {
      val t1 = typeSchema("[T]ArrayList[T]")
      val t2 = typeSchema("[T]List[T]")
      assertTrue(tsa.subtypeED(t1, t2))
    }
    {
      val t1 = typeSchema("[T extends {Zz}]Array[T]")
      val t2 = typeSchema("[T extends {Eq[T]}] Array[T]")
      assertFalse(tsa.subtypeED(t1, t2))
      val t3 = typeSchema("Array[Zz]")
      assertTrue(tsa.subtypeED(t3, t2))
    }
  }
  
  /** MAGIC! WHOOOOOSSSHHHHH! */
  private def ln: String = "test on line %d"
    .format(Thread.currentThread().getStackTrace()(2).getLineNumber())
  
  def testExistentialReduction() = {
    val tsa = basicTsa
    val testCases = List[(String, String, String, String)](
      // (unique message for debugging, t1, t2, expected reduced meet)
      // - the expected reduced meet is Some/None according to result of
      //   existentialReduction
      
      // Between two base types, using polymorphic exclusion.
      (ln, "[T]ArrayList[T]", "[S]List[S]",
           "[T]ArrayList[T]"),
      (ln, "[T]ArrayList[T]", "[T]List[T]",
           "[T]ArrayList[T]"),
      (ln, "[T extends {Aa}]ArrayList[T]", "[S]List[S]",
           "[T extends {Aa}]ArrayList[T]"),
      (ln, "[T]ArrayList[T]", "[S extends {Aa}]List[S]",
           "[T extends {Aa}]ArrayList[T]"),
      (ln, "[T extends {Aa}]ArrayList[T]", "[S extends {Bb}]List[S]",
           "[T extends {Bb}]ArrayList[T]"),
      (ln, "[T extends {Aa}]ArrayList[T]", "[S extends {Bb}]List[S]",
           "[T extends {Bb}]ArrayList[T]"),
      
      // Same but with tuple types.
      (ln, "[T](ArrayList[T], Aa)", "[T](List[T], Bb)",
           "[T](ArrayList[T], Bb)"),
      (ln, "[T,U](ArrayList[T], List[U])", "[T,U](List[T], ArrayList[U])",
           "[T,U](ArrayList[T], ArrayList[U])"),
      (ln, "[T extends {Aa}](ArrayList[T], Aa)", "[T extends {Bb}](List[T], Bb)",
           "[T extends {Bb}](ArrayList[T], Bb)"),
      
      // Between two type variables
      (ln, "[T]T", "[T]T", "[T]T"),
      (ln, "[T extends {Aa}]T", "[T extends {Bb}]T",
           "[T extends {Bb}]T")
      
      /* DOESN'T WORK YET
      (ln, "[T, U extends {List[T]}]U",
          "[T extends {List[U]}, U extends {Zz}]T",
          "[T extends {Zz}, U extends {List[T]}]U"),
      (ln, "[T extends {Bb}, U extends {List[T]}]U",
          "[T extends {ArrayList[U]}, U extends {Aa}]T",
          "[T extends {Zz}, U extends {List[T]}]U"),
       
      (ln, "[T extends {Zz}]T", "[T extends {String}]T", Some("BOTTOM")),
      (ln, "[T extends {Zz}]List[T]", "[T extends {String}]List[T]",
          Some("BOTTOM")),
      (ln, "[T extends {Zz}]List[T]", "[T extends {String}]ArrayList[T]",
          Some("BOTTOM"))
      */
    )
    
    // Test each one.
    for ((msg, sT1, sT2, sMeet) <- testCases) {
      val reducedMeet = tsa.meetED(typeSchema(sT1), typeSchema(sT2))
      val expectedMeet = typeSchema(sMeet)
      assertTrue("%s (reduced meet is not equal to expected)".format(msg),
                 tsa.equivalentED(reducedMeet, expectedMeet))
    }
  }
}
