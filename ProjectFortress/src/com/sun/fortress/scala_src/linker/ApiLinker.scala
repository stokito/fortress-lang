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

package com.sun.fortress.scala_src.linker

import com.sun.fortress.compiler.index.ApiIndex
import com.sun.fortress.compiler.GlobalEnvironment
import com.sun.fortress.compiler.index.ApiIndex
import com.sun.fortress.compiler.IndexBuilder
import com.sun.fortress.nodes._
import com.sun.fortress.repository.FortressRepository
import com.sun.fortress.scala_src.nodes._
import com.sun.fortress.scala_src.useful.Lists._
import _root_.java.util.{List => JList}
import _root_.java.util.Map
import _root_.java.util.ArrayList

class ApiLinker(env: GlobalEnvironment, repository: FortressRepository) {
  def link(api: ApiIndex): ApiIndex = {
    api.ast match {
      case SApi(info, name, imports, decls, comprises) => {
        if (comprises.isEmpty) { 
          api 
        } 
        else {
          val allDecls = new ArrayList[Decl]()
          for (constituent <- comprises) {
            constituent match {
              case SApi(cinfo, cname, cimports, cdecls, _) => {
                // The comprises clause should be empty because repositories
                // only store simple APIs
                  allDecls.addAll(toJavaList(cdecls))
                }
              }
            }
            IndexBuilder.builder.buildApiIndex(new Api(info, name, toJavaList(imports), 
                                                       allDecls, new ArrayList[APIName]()), 
                                               api.modifiedDate)
        }
      }
    }
  }
}

