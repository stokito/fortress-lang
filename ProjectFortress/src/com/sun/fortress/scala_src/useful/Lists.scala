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
import _root_.java.util.{LinkedList => JavaLinkedList}
import _root_.java.util.{List => JavaList}
import _root_.junit.framework.TestCase

object Lists {
  def toJavaList[T](xs:ScalaList[T]):JavaLinkedList[T] = {
    xs match {
      case ScalaList() => new JavaLinkedList[T]
      case y :: ys => {
        var result = toJavaList(ys)
        result.addFirst(y)
        result
      }
    }
  }
  def fromJavaList[T](xs:JavaList[T]) = {
    var result = ScalaList[T]()
    val rightmost = xs.size - 1
    for (i <- 0 to rightmost) {
      result = xs.get(rightmost - i) :: result
    }
    result
  }
}

class ListsJUTest() extends TestCase {
  def testEmptyToJavaList() = {
    val xs = ScalaList[Int]()
    assert(Lists.toJavaList(xs).isEmpty,
           "Empty Scala lists are not mapped to empty Java lists")
  }
}
