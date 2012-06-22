/*******************************************************************************
    Copyright 2009, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.scala_src.linker

import com.sun.fortress.compiler.index.ApiIndex
import com.sun.fortress.compiler.GlobalEnvironment
import com.sun.fortress.compiler.index.ApiIndex
import com.sun.fortress.nodes._
import com.sun.fortress.repository.FortressRepository
import com.sun.fortress.scala_src.nodes._
import com.sun.fortress.scala_src.typechecker.IndexBuilder
import com.sun.fortress.scala_src.useful.Lists._
import _root_.java.util.{List => JList}
import _root_.java.util.Map
import _root_.java.util.ArrayList

class ApiLinker(env: Map[APIName, ApiIndex], globalEnv: GlobalEnvironment) {
  def link(api: Api): Api = {
    api match {
      case SApi(info, name, imports, decls, comprises) => {
        if (comprises.isEmpty) { api }
        else {
          val allDecls = new ArrayList[Decl]()
          for (cname <- comprises) {
            // Because api has already been checked by the CompoundApiChecker,
            // each name must be in env.
            env.get(cname).ast match {
              case SApi(_, _, _, cdecls, _) => {
                // The comprises clause should be empty because repositories
                // only store simple APIs
                allDecls.addAll(toJavaList(fixRefs(cdecls, name, comprises)))
              }
            }
          }
          // Keep comprises clause in linked API so we can distinguish it
          // as a compound API in later phases.
          new Api(info, name, toJavaList(imports), allDecls, toJavaList(comprises))
        }
      }
    }
  }

  def fixRefs(ds: List[Decl], compoundName: APIName, comprises: List[APIName]): List[Decl] = {
    new ReferenceFixer(compoundName, comprises)(ds).asInstanceOf[List[Decl]]
  }

  class ReferenceFixer(compoundName: APIName, comprises: List[APIName]) extends Walker {
    override def walk(node: Any): Any = node match {
      case old@SAPIName(getInfo, getIds, getText) if (comprises contains old) => compoundName
      case _ => super.walk(node)
    }
  }
}
