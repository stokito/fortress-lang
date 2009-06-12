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

package com.sun.fortress.scala_src.useful

import _root_.java.util.{HashMap=>JHashMap}
import _root_.java.util.{HashSet=>JHashSet}
import _root_.java.util.{List=>JList}
import _root_.java.util.{LinkedList=>JLinkedList}
import edu.rice.cs.plt.tuple.{Option=>JOption}
import _root_.java.util.{Map => JMap}
import _root_.java.util.{Set => JSet}
import scala.collection.jcl.MapWrapper
import scala.collection.jcl.BufferWrapper
import scala.collection.jcl.Conversions

object ASTGenHelper {
  def scalaify(typ: Any): Any = typ match {
    case o:JOption[Object] => {
      if(o.isSome)
        Some(scalaify(o.unwrap))
      else
        None
    }

    case l:JList[Object] => {
      var accum = List[Any]()
      for (e <- (List()++Conversions.convertList(l))) {
        accum = accum:::List(scalaify(e))
      }
      accum
    }

    case m:JMap[Object,Object] => {
      var accum = Map[Any,Any]()
      for (k <- (Map.empty++Conversions.convertMap(m)).keySet) {
          accum += ((scalaify(k), scalaify(m.get(k))))
      }
      accum
    }

    case s:JSet[Object] => {
      var accum = Set[Any]()
      for (e <- Set.empty++Conversions.convertSet(s)) {
        accum = accum + scalaify(e)
      }
      accum
    }

    case _ => typ
  }

  def javaify(typ: Any): Object = typ match {
    case Some(t) => JOption.some(javaify(t))
    case None => JOption.none

    case l:List[Object] => {
      val accum = new JLinkedList[Object]()
      for (e <- l) {
        accum.addLast(javaify(e))
      }
      accum
    }

    case m:Map[Object,Object] => {
      val accum = new JHashMap[Object,Object]()
      val keyset = m.keys
      for (k <- keyset) {
        accum.put(javaify(k),javaify(m.apply(k)))
      }
      accum
    }

    case s:Set[Object] => {
      val accum = new JHashSet[Object]()
      for (e <- s) {
        accum.add(javaify(e))
      }
      accum
    }

    case _ => typ.asInstanceOf[Object]
  }
}
