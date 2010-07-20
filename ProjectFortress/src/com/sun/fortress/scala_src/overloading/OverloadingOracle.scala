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

package com.sun.fortress.scala_src.overloading


import com.sun.fortress.compiler.GlobalEnvironment
import com.sun.fortress.compiler.index._
import com.sun.fortress.compiler.Types.ANY
import com.sun.fortress.compiler.Types.BOTTOM
import com.sun.fortress.compiler.Types.OBJECT
import com.sun.fortress.exceptions.InterpreterBug.bug
import com.sun.fortress.nodes._
import com.sun.fortress.nodes_util.NodeFactory._
import com.sun.fortress.scala_src.nodes._
import com.sun.fortress.scala_src.typechecker.ConstraintFormula
import com.sun.fortress.scala_src.typechecker.CnFalse
import com.sun.fortress.scala_src.typechecker.CnTrue
import com.sun.fortress.scala_src.typechecker.CnAnd
import com.sun.fortress.scala_src.typechecker.CnOr
import com.sun.fortress.scala_src.typechecker.staticenv.KindEnv
import com.sun.fortress.scala_src.typechecker.TraitTable
import com.sun.fortress.scala_src.types.TypeAnalyzer
import com.sun.fortress.scala_src.types.TypeSchemaAnalyzer
import com.sun.fortress.scala_src.useful.ErrorLog
import com.sun.fortress.scala_src.useful.Lists._
import com.sun.fortress.scala_src.useful.Maps._
import com.sun.fortress.scala_src.useful.Options._
import com.sun.fortress.scala_src.useful.Sets._
import com.sun.fortress.scala_src.useful.STypesUtil._
import com.sun.fortress.useful.NI


class OverloadingOracle(implicit analyzer: TypeAnalyzer) extends PartialOrdering[Functional] {
  
  val schemaAnalyzer = new TypeSchemaAnalyzer()
  
  override def tryCompare(x: Functional, y: Functional): Option[Int] = {
    val xLEy = lteq(x,y)
    val yLEx = lteq(y,x)
    if (xLEy) Some(if (yLEx) 0 else -1)
    else if (yLEx) Some(1) else None
  }
  
  // Checks when f is more specific than g
  def lteq(f: Functional, g: Functional): Boolean = {
    val fa = makeArrowFromFunctional(f).get
    val ga = makeArrowFromFunctional(g).get
    val fd = insertStaticParams(fa.getDomain, getStaticParams(fa))
    val gd = insertStaticParams(ga.getDomain, getStaticParams(ga))
    schemaAnalyzer.lteqExistential(fd, gd)
  }
  // Checks the return type rule
  def typeSafe(f: Functional, g: Functional): Boolean = {
    if(!lteq(f, g))
      true
    else {
      val fa = schemaAnalyzer.alphaRenameTypeSchema(makeArrowFromFunctional(f).get)
      val ga = schemaAnalyzer.alphaRenameTypeSchema(makeArrowFromFunctional(g).get)
      val SArrowType(STypeInfo(s1, p1, sp1, w1), d1, r1, e1, i1, m1) = fa
      val SArrowType(STypeInfo(s2, p2, sp2, w2), d2, r2, e2, i2, m2) = ga
      val meet = SArrowType(STypeInfo(s1, p1,sp1 ++ sp2, None), analyzer.meet(d1,d2), r2, analyzer.minimalEffect(e1,e2), i1 && i2, None)
      schemaAnalyzer.lteq(fa, meet)
    }
  }
}