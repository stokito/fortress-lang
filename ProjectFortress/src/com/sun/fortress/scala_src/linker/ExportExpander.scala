/*******************************************************************************
    Copyright 2009,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.scala_src.linker

import com.sun.fortress.compiler.index.ApiIndex
import com.sun.fortress.compiler.GlobalEnvironment
import com.sun.fortress.compiler.index.ComponentIndex
import com.sun.fortress.nodes._
import com.sun.fortress.repository.FortressRepository
import com.sun.fortress.useful.HasAt
import com.sun.fortress.scala_src.nodes._
import com.sun.fortress.scala_src.typechecker.IndexBuilder
import com.sun.fortress.scala_src.useful.ErrorLog
import com.sun.fortress.scala_src.useful.ASTGenHelper._
import com.sun.fortress.scala_src.useful.Lists._
import com.sun.fortress.scala_src.useful.Maps._
import com.sun.fortress.scala_src.useful.Sets._
import _root_.java.util.{List => JList}
import _root_.java.util.Map
import _root_.java.util.ArrayList

class ExportExpander(env: GlobalEnvironment) {
  val errors = new ErrorLog()

  def signal(msg: String, hasAt: HasAt) = {
    errors.signal(msg, hasAt)
  }

  def expand(comp: Component) = {
    var newExports: List[APIName] = List()

    // Add constituents of exported compound APIs
    for (export <- toListFromImmutable(comp.getExports)) {
      if (env.definesApi(export)) {
        for (constituent <- toSet(env.lookup(export).comprises)) {
          newExports = constituent :: newExports
        }
      }
      else {
        signal("Undefined API in export clause: " + export, export)
      }
    }
    // Add compound APIs whose constituents are all exported.
    // Implicit additions might enable yet more implicit additions.
    // Continue until a fixed point is reached.
    var moreAdded = true
    while (moreAdded) {
      moreAdded = false
      for (api <- toMap(env.apis).valuesIterator) {
        val name = api.ast.getName
        // Simple APIs have empty comprises clauses and are never added in this loop
        var canAdd = !(api.comprises.isEmpty) && !(newExports contains name)
        for (constituent <- toSet(api.comprises)) {
          // A compound API is added iff all of its constituents are exported
          canAdd = canAdd && (comp.getExports contains constituent)
        }
        if (canAdd) {
          moreAdded = true
          newExports = name :: newExports
        }
      }
    }

    // Build new list of exports. Note that ComponentIndices always hold Components.
    val allExports = toListFromImmutable(comp.getExports) ::: newExports
    // Remove duplicates
    allExports.distinct
    // Construct a new AST with allExports as its extends clause
    replaceExports(comp, allExports)
  }

  def replaceExports(ast: Component, allExports: List[APIName]) = new ExportReplacer(allExports)(ast)

  class ExportReplacer(allExports: List[APIName]) extends Walker {
    override def walk(node: Any): Any = node match {
      case SComponent(getInfo, getName, getImports, getDecls, getComprises, is_native, getExports) =>
        SComponent(getInfo, getName, getImports, getDecls, getComprises, is_native, allExports)
      case _ => super.walk(node)
    }
  }
}
