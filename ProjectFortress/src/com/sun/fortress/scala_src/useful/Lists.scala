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
import _root_.java.util.Arrays
import _root_.java.util.{LinkedList => JLinkedList}
import _root_.java.util.{List => JList}
import _root_.junit.framework.TestCase
import scala.collection.jcl.Conversions

object Lists {
  def toJavaList[T](xs:List[T]):JLinkedList[T] = {
    xs match {
      case List() => new JLinkedList[T]
      case y :: ys => {
        val result = toJavaList(ys)
        result.addFirst(y)
        result
      }
    }
  }

  def toList[T](xs:JList[T]) = List()++Conversions.convertList(xs)
 
  def map[S,T](list: JList[S], fun: S=>T ): JList[T] = toJavaList(toList(list).map(fun))
  
}

class JavaList[T] {
  def apply(xs:T*) = {
    val result = new JLinkedList[T]
    for (x <- xs.elements) {
      result.addLast(x)
    }
    result
  }
  def unapplySeq(xs:JList[T]) = Some(Lists.toList(xs))
}

class ListsJUTest() extends TestCase {
  def testEmptyToJavaList() = {
    val xs = List[Int]()
    assert(Lists.toJavaList(xs).isEmpty,
           "Empty Scala lists are not mapped to empty Java lists")
  }
}
