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

package com.sun.fortress.scala_src.useful

import _root_.java.util.{HashMap => JavaHashMap}
import _root_.java.util.{Map => JavaMap}
import scala.collection.{Map => MMap}
import scala.collection.JavaConversions

object Maps {
  def toMap[S, T](jmap: JavaMap[S, T]): Map[S, T] = Map.empty ++ JavaConversions.asMap(jmap)

  def toJavaMap[S, T](smap: MMap[S, T]): JavaMap[S, T] = {
    var jmap = new JavaHashMap[S, T]()
    for (key <- smap.keysIterator) {
      val value = smap.get(key).get
      jmap.put(key, value)
    }
    jmap
  }
}
