package com.sun.fortress.scalasrc.useful
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
      
