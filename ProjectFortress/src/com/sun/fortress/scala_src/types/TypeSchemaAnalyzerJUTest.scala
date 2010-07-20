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
    val tsa = typeSchemaAnalyzer("""{
      trait Aa,
      trait Eq[T],
      trait List[T],
      trait Array[T],
      trait ArrayList[T] extends {List[T], Array[T]},
      trait Zz extends {Eq[Zz]}}""")
    
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
      val tsa = typeSchemaAnalyzer("""{
      trait Aa,
      trait Eq[T],
      trait List[T],
      trait Array[T],
      trait ArrayList[T] extends {List[T], Array[T]},
      trait Zz extends {Eq[Zz]}}""")
      
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
  
}
