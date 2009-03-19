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
    case o:JOption[_] => {
      if(o.isSome)
        Some(o.unwrap)
      else
        None
    }

    case l:JList[_] => List()++Conversions.convertList(l)

    case m:JMap[_,_] => Map.empty++Conversions.convertMap(m)

    case s:JSet[_] => Set.empty++Conversions.convertSet(s)
    case _ => typ
  }

  def javaify(typ: Any): Any = typ match {
    case Some(t) => JOption.some(t)
    case None => JOption.none

    case l:List[Object] => {
      val accum = new JLinkedList[Object]()
      for (e <- l) {
        accum.addLast(e)
      }
      accum
    }

    case m:Map[Object,Object] => {
      val accum = new JHashMap[Object,Object]()
      val keyset = m.keys
      for (k <- keyset) {
          accum.put(k,m.apply(k))
      }
      accum
    }

    case s:Set[Object] => {
      val accum = new JHashSet[Object]()
      for (e <- s) {
        accum.add(e)
      }
      accum
    }

    case _ => typ
  }
}
