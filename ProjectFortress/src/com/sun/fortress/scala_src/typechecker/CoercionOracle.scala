package com.sun.fortress.scala_src.typechecker

import com.sun.fortress.nodes._
import com.sun.fortress.scala_src.nodes._
import com.sun.fortress.compiler.index.CompilationUnitIndex
import com.sun.fortress.compiler.typechecker.TypeAnalyzer
import com.sun.fortress.compiler.typechecker.TraitTable
import com.sun.fortress.compiler.typechecker.TypeEnv

class CoercionOracle(env: TypeEnv, traits: TraitTable, current: CompilationUnitIndex){
  def mostSpecific(cs: Set[Type]): Option[Type] = None
  def coercionsTo(t: Type): Set[Type] = Set()
  def coercesTo(t: Type, b: Type): Boolean = coercionsTo(t).contains(b)
}
