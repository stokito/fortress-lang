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

package com.sun.fortress.scala_src.useful

import _root_.junit.framework._
import com.sun.fortress.compiler.Types._
import com.sun.fortress.nodes._
import com.sun.fortress.nodes_util.{ExprFactory => EF}
import com.sun.fortress.nodes_util.{NodeFactory => NF}
import com.sun.fortress.nodes_util.{NodeUtil => NU}
import com.sun.fortress.scala_src.useful.Lists._
import com.sun.fortress.scala_src.useful.STypesUtil._

// import scala.collection.mutable.HashMap

import Assert._

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
      val subst = liftTypeSubstitution(map)
      assertEquals(ANY, subst(iv1))
      assertEquals(OBJECT, subst(iv2))
      assertEquals(makeTriple(OBJECT, tvT, makeTriple(ANY, BOTTOM, tvU)),
                   subst(t2))
    }
    
    {
      val map = Map[VarType, Type](tvT -> ANY, tvU -> OBJECT)
      val subst = liftTypeSubstitution(map)
      assertEquals(ANY, subst(tvT))
      assertEquals(OBJECT, subst(tvU))
      assertEquals(makeTriple(iv2, ANY, makeTriple(iv1, BOTTOM, OBJECT)),
                   subst(t2))
    }
    
    {
      val map = Map[TypeVariable, Type](tvT -> ANY, tvU -> makeTriple(tvU, iv1, iv2))
      val subst = liftTypeSubstitution(map)
      assertEquals(ANY, subst(tvT))
      assertEquals(makeTriple(tvU, iv1, iv2), subst(tvU))
      assertEquals(makeTriple(iv2, ANY, makeTriple(iv1, BOTTOM, makeTriple(tvU, iv1, iv2))),
                   subst(t2))
    }
  }
}