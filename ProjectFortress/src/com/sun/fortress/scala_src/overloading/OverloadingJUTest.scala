/*******************************************************************************
    Copyright 2009,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.scala_src.overloading

import junit.framework._
import com.sun.fortress.compiler.Types.ANY
import com.sun.fortress.compiler.Types.BOTTOM
import com.sun.fortress.compiler.Types.OBJECT
import com.sun.fortress.nodes._
import com.sun.fortress.scala_src.nodes._
import com.sun.fortress.scala_src.typechecker.ExclusionOracle
import com.sun.fortress.scala_src.types.TypeAnalyzer
import com.sun.fortress.scala_src.types.TypeSchemaAnalyzer
import com.sun.fortress.scala_src.useful.ErrorLog
import com.sun.fortress.scala_src.useful.TypeParser
import com.sun.fortress.scala_src.useful.STypesUtil

import Assert._


class OverloadingJUTest extends TestCase {
  
  def typeAnalyzer(str:String) = TypeParser.parse(TypeParser.typeAnalyzer, str).get
  def overloadingSet(str: String) = TypeParser.parse(TypeParser.overloadingSet, str).get
  def arrowSchema(str: String) = TypeParser.parse(TypeParser.arrowTypeSchema, str).get
  def overloadingOracle(str: String) = new OverloadingOracle()(typeAnalyzer(str))
  
  def testNonGeneric(){
    val oa = overloadingOracle("""{
      trait Aa,
      trait Bb extends {Aa},
      trait Eq[T],
      trait List[T],
      trait Array[T],
      trait ArrayList[T] extends {List[T], Array[T]},
      trait Zz extends {Eq[Zz]}}""")
    
    {
      val a1 = arrowSchema("Bb -> Bb")
      val a2 = arrowSchema("Aa -> Bb")
      //assertTrue(oa.moreSpecific(a1, a2))
      //assertTrue(oa.typeSafe(a1, a2))
    }
    
  }
}
