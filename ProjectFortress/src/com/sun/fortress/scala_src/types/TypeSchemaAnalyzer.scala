package com.sun.fortress.scala_src.types

import com.sun.fortress.compiler.index._
import com.sun.fortress.compiler.Types.ANY
import com.sun.fortress.compiler.Types.BOTTOM
import com.sun.fortress.compiler.Types.OBJECT
import com.sun.fortress.exceptions.InterpreterBug.bug
import com.sun.fortress.nodes._
import com.sun.fortress.nodes_util.NodeFactory.typeSpan
import com.sun.fortress.nodes_util.{NodeFactory => NF}
import com.sun.fortress.scala_src.nodes._
import com.sun.fortress.scala_src.typechecker.ConstraintFormula
import com.sun.fortress.scala_src.typechecker.CnFalse
import com.sun.fortress.scala_src.typechecker.CnTrue
import com.sun.fortress.scala_src.typechecker.CnAnd
import com.sun.fortress.scala_src.typechecker.CnOr
import com.sun.fortress.scala_src.typechecker.staticenv.KindEnv
import com.sun.fortress.scala_src.typechecker.TraitTable
import com.sun.fortress.scala_src.types.TypeAnalyzerUtil._
import com.sun.fortress.scala_src.useful.ErrorLog
import com.sun.fortress.scala_src.useful.Lists._
import com.sun.fortress.scala_src.useful.Maps._
import com.sun.fortress.scala_src.useful.Options._
import com.sun.fortress.scala_src.useful.Sets._
import com.sun.fortress.scala_src.useful.STypesUtil._
import com.sun.fortress.useful.NI

class TypeSchemaAnalyzer(val ta: TypeAnalyzer) extends BoundedLattice[Type] {
  def top = ANY
  def bottom = BOTTOM
  
  def meet(s: Type, t: Type): Type = s
  def join(s: Type, t: Type): Type = s
  def lteq(s: Type, t:Type): Boolean = false
  
  //Are two type schema the same up to alpha equivalence
  def syntacticEq(s: Type, t: Type): Boolean = false
  //Remove Duplicates
  def removeDuplicates(ts: List[Type]): List[Type] = ts 
  def duplicateFreeIntersection(ts: List[Type]): Type = top  
  
}