package com.sun.fortress.scala_src.useful
import _root_.java.util.{HashMap => JavaHashMap}
import _root_.java.util.{Map => JavaMap}
import scala.collection.jcl.Conversions
import scala.collection.jcl.MapWrapper

object Maps {
  def toMap[S,T](jmap: JavaMap[S,T]):Map[S,T] = Map.empty ++ Conversions.convertMap(jmap)
  def toJavaMap[S,T](smap: Map[S,T]): JavaMap[S,T] = {
    var jmap = new JavaHashMap[S,T]()
    for(key <- smap.keys){
      val value = smap.get(key).get
      jmap.put(key,value)
    }
    jmap
  }
}
