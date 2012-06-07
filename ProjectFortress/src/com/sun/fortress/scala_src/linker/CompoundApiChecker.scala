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
import com.sun.fortress.exceptions.StaticError
import com.sun.fortress.nodes._
import com.sun.fortress.repository.FortressRepository
import com.sun.fortress.useful.HasAt
import com.sun.fortress.scala_src.nodes._
import com.sun.fortress.scala_src.useful.ErrorLog
import com.sun.fortress.scala_src.useful.Lists._
import _root_.java.util.{List => JList}
import _root_.java.util.Map
import _root_.java.util.ArrayList

class CompoundApiChecker(env: Map[APIName, ApiIndex], globalEnv: GlobalEnvironment) {
  val errors = new ErrorLog()
  
  def signal(msg: String, hasAt: HasAt) = {
    errors.signal(msg, hasAt)
  }

  def check(api: Api): JList[StaticError] = {
    api match {
      case SApi(info, name, imports, decls, comprises) => {
        for (cname <- comprises) {
          if (!(env containsKey cname)) {
            signal("Undefined API in comprises clause: " + cname.toString, cname)
          }
        }
      }
    }
    toJavaList(errors.asList)
  }
}


