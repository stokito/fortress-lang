/*******************************************************************************
    Copyright 2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.scala_src.useful

import _root_.junit.framework._
import _root_.junit.framework.Assert._
import com.sun.fortress.compiler.Types._
import com.sun.fortress.nodes._
import com.sun.fortress.nodes_util.{ExprFactory => EF}
import com.sun.fortress.nodes_util.{NodeFactory => NF}
import com.sun.fortress.nodes_util.{NodeUtil => NU}
import com.sun.fortress.scala_src.useful.Lists._
import com.sun.fortress.scala_src.useful.STypesUtil._

// import scala.collection.mutable.HashMap

class STypesUtilJUTest extends TestCase {
  
  def testLiftTypeSubstitution = {
    val iv1 = NF.make_InferenceVarType(NF.typeSpan)
    val iv2 = NF.make_InferenceVarType(NF.typeSpan)
    val tvT = NF.makeVarType(NF.typeSpan, "T")
    val tvU = NF.makeVarType(NF.typeSpan, "U")
    
    // Make a 3-element tuple type.
    def makeTriple(e1: Type, e2: Type, e3: Type) =
      NF.makeTupleType(toJavaList(List(e1, e2, e3)))
    
    // t1 =           ($iv1, BOTTOM, U)
    // t2 = ($iv2, T, ($iv1, BOTTOM, U))
    val t1 = makeTriple(iv1, BOTTOM, tvU)
    val t2 = makeTriple(iv2, tvT, t1)
    
    {
      val map = Map[_InferenceVarType, Type](iv1 -> ANY, iv2 -> OBJECT)
      val subst: Type => Type = liftSubstitution(map)
      assertEquals(ANY, subst(iv1))
      assertEquals(OBJECT, subst(iv2))
      assertEquals(makeTriple(OBJECT, tvT, makeTriple(ANY, BOTTOM, tvU)),
                   subst(t2))
    }
    
    {
      val map = Map[VarType, Type](tvT -> ANY, tvU -> OBJECT)
      val subst: Type => Type = liftSubstitution(map)
      assertEquals(ANY, subst(tvT))
      assertEquals(OBJECT, subst(tvU))
      assertEquals(makeTriple(iv2, ANY, makeTriple(iv1, BOTTOM, OBJECT)),
                   subst(t2))
    }
  }
}
