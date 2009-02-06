package com.sun.fortress.scala_src.typechecker

import com.sun.fortress.nodes._
import com.sun.fortress.scala_src.nodes._
import com.sun.fortress.exceptions.StaticError
import com.sun.fortress.compiler.index.CompilationUnitIndex
import com.sun.fortress.compiler.typechecker.TraitTable
import com.sun.fortress.compiler.typechecker.TypeEnv
import com.sun.fortress.scala_src.useful.Lists._


class TypeChecker(current: CompilationUnitIndex, traits: TraitTable ) {
  
  val errors: List[StaticError] = List()
  
  def check(node: Node, env: TypeEnv):Node = node match {
    case e:Expr => checkExpr(e,env)
    case Component(info,name,imports,decls,is_native,exports)  => Component(info,name,imports,map(decls,(n:Decl)=>check(n,env).asInstanceOf),is_native,exports)
    case _ => node
  }
  
  def checkExpr(expr: Expr, env: TypeEnv):Expr = expr match {
    case _ => expr
  }
  
  
  
}
