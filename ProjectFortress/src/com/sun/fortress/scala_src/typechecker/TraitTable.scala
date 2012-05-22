/*******************************************************************************
    Copyright 2009,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.scala_src.typechecker

import _root_.java.util.Map
//import _root_.java.util._
import edu.rice.cs.plt.tuple.{Option => JOption}

import com.sun.fortress.nodes._
import com.sun.fortress.nodes_util.NodeFactory
import com.sun.fortress.compiler.GlobalEnvironment
import com.sun.fortress.compiler.index.ApiIndex
import com.sun.fortress.compiler.index.CompilationUnitIndex
import com.sun.fortress.compiler.index.ComponentIndex
import com.sun.fortress.compiler.index.TypeConsIndex
import com.sun.fortress.scala_src.useful.Iterators._

import scala.collection.mutable.HashSet
import scala.collection.mutable.Set

class TraitTable(current: CompilationUnitIndex, globalEnv: GlobalEnvironment) extends Iterable[TypeConsIndex] {
  def typeCons(name: Id): JOption[TypeConsIndex] = {
    val simpleName: Id = NodeFactory.makeId(NodeFactory.makeSpan(name), name.getText)
    val ast = current.ast

    if ( name.getApiName.isNone ||
         ( ast.isInstanceOf[Api] && ast.getName.equals(name.getApiName.unwrap) ) ||
         ( ast.isInstanceOf[Component] &&
           ast.asInstanceOf[Component].getExports.contains(name.getApiName.unwrap) ) ) {
      JOption.wrap(current.typeConses.get(simpleName))
    }
    else {
      val api = globalEnv.api(name.getApiName.unwrap)
      if (api == null) JOption.none[TypeConsIndex]
      else JOption.wrap(api.typeConses().get(simpleName))
    }
  }

  def compilationUnit(name: APIName): CompilationUnitIndex = {
    if (current.ast.getName.equals(name)) current
    else globalEnv.api(name)
  }

  override def iterator() = {
    var result: Set[TypeConsIndex] = new HashSet()
    for (t <- current.typeConses.values) {
      result += t
    }
    for (api <- globalEnv.apis.values) {
      for (t <- api.typeConses.values) {
        result += t
      }
    }
    result.iterator
  }
}
