/*******************************************************************************
    Copyright 2009 Sun Microsystems, Inc.,
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

import com.sun.fortress.compiler.index._
import com.sun.fortress.compiler.Types.ANY
import com.sun.fortress.compiler.Types.BOTTOM
import com.sun.fortress.compiler.Types.OBJECT
import com.sun.fortress.exceptions.InterpreterBug.bug
import com.sun.fortress.nodes._
import com.sun.fortress.nodes_util.NodeFactory.typeSpan
import com.sun.fortress.scala_src.nodes._
import com.sun.fortress.scala_src.types.TypeAnalyzerUtil._
import com.sun.fortress.scala_src.useful.Lists._
import com.sun.fortress.scala_src.useful.Maps._
import com.sun.fortress.scala_src.useful.Options._
import com.sun.fortress.scala_src.useful.Sets._
import com.sun.fortress.scala_src.useful.STypesUtil._
import com.sun.fortress.useful.NI

class CoveringAnalyzer(ta: TypeAnalyzer) {

  def minimalCover(x: Type): Type = ta.normalize(x) match {
    case SIntersectionType(_, e)
      if e.forall(_.isInstanceOf[TraitType]) =>
        ta.meet(e.map(x => makeUnionType(comprisesLeaves(x.asInstanceOf[TraitType]))))
    case SIntersectionType(_, e) => ta.meet(e.map(minimalCover))
    case SUnionType(_, e) => ta.join(e.map(minimalCover))
    case SArrowType(i, d, r, e, io, m) =>
      SArrowType(i, d, minimalCover(r), e, io, m)
    //ToDo: Handle keywords
    case STupleType(i, e, mv, _) => STupleType(i, e.map(minimalCover), mv.map(minimalCover), Nil)
    case _ => x
  }
  
  private def comprisesLeaves(x: TraitType): Set[TraitType] = ta.comprisesClause(x) match {
    case ts if ts.isEmpty => Set(x)
    case ts => ts.flatMap(comprisesLeaves)
  }
  
  private def mergeArrows(x: ArrowType, y: ArrowType) = {
    x
  }
  
  
  
}
