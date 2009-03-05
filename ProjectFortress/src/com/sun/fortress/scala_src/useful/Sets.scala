package com.sun.fortress.scala_src.useful

import scala.collection.Set
import _root_.java.util.{HashSet => JHashSet}
import _root_.java.util.{Set => JavaSet}
import scala.collection.jcl.Conversions

object Sets {
  def toJavaSet[T](sset: Set[T]): JavaSet[T] = {
    val temp = new JHashSet[T]()
    for(e <- sset){
      temp.add(e)
    }
    temp
  }
  
  def toSet[T](jset: JavaSet[T]): Set[T] = Set() ++ Conversions.convertSet(jset)
  
}
