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
import _root_.scala.{List => ScalaList}
import _root_.java.util.{LinkedList => JLinkedList}
import _root_.java.util.{List => JList}
import _root_.junit.framework.TestCase

object Lists {
  def toJavaList[T](xs:ScalaList[T]):JLinkedList[T] = {
    xs match {
      case ScalaList() => new JLinkedList[T]
      case y :: ys => {
        val result = toJavaList(ys)
        result.addFirst(y)
        result
      }
    }
  }
  def toScalaList[T](xs:JList[T]) = {
    var result = ScalaList[T]()
    val rightmost = xs.size - 1
    for (i <- 0 to rightmost) {
      result = xs.get(rightmost - i) :: result
    }
    result
  }
  
  def map[S,T](list: JList[S], fun: S=>T ): JList[T] = toJavaList(toScalaList(list).map(fun))
  
}

object JavaList {
  def apply[T](xs:T*) = {
    val result = new JLinkedList[T]
    for (x <- xs.elements) {
      result.addLast(x)
    }
    result
  }
  def unapplySeq[T](xs:JList[T]) = Some(Lists.toScalaList(xs))
}

class ListsJUTest() extends TestCase {
  def testEmptyToJavaList() = {
    val xs = ScalaList[Int]()
    assert(Lists.toJavaList(xs).isEmpty,
           "Empty Scala lists are not mapped to empty Java lists")
  }
}
