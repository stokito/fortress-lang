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

import junit.framework.TestCase
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
  
  def toString(t: Type) = {
    if(t.getInfo.getStaticParams.isEmpty)
      t.toString
    val sparams = getStaticParams(t)
    "[" + sparams.mkString(", ") + "]" + t.toString
    
  }
  
  def testAlphaRename(){
    val tsa = typeSchemaAnalyzer("{}")
    val t = typeSchema("[T extends {Eq[T]}]T")
    
    object staticReplacer extends Walker {
      override def walk(a: Any): Any = a match{
        case a: VarType => NF.makeVarType(a.getInfo.getSpan, "S")
        case _ => super.walk(a)
      }
    }
    
    val tt = tsa.alphaRename(t)
    
    println(toString(tt))
    
  }
  
  
}
