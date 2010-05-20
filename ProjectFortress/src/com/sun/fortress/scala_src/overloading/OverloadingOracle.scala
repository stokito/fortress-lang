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


class OverloadingOracle(implicit analyzer: TypeAnalyzer) {
  val schemaAnalyzer = new TypeSchemaAnalyzer()
  
  //ToDo: handle where clauses
  def moreSpecific(s: ArrowType, t: ArrowType): Boolean = {
    val edom1 = insertStaticParams(s.getDomain, getStaticParams(s))
    val edom2 = insertStaticParams(t.getDomain, getStaticParams(t))
    schemaAnalyzer.lteqExistential(edom1, edom2)
  }
  
  //ToDo: handle where clauses
  def typeSafe(s: ArrowType, t: ArrowType) = (schemaAnalyzer.alphaRenameTypeSchema(s),schemaAnalyzer.alphaRenameTypeSchema(t)) match {
    case (SArrowType(STypeInfo(s1, p1, sp1, w1), d1, r1, e1, i1, m1),SArrowType(STypeInfo(s2, p2, sp2, w2), d2, r2, e2, i2, m2)) => {
      val meet = SArrowType(STypeInfo(s1, p1,sp1 ++ sp2, None), analyzer.meet(d1,d2), r2, analyzer.minimalEffect(e1,e2), i1 && i2, None)
      schemaAnalyzer.lteq(s, meet)
    }
  }
  
}