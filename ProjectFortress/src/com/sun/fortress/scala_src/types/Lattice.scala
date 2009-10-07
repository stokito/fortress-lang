package com.sun.fortress.scala_src.types

import scala.PartialOrdering

trait Lattice[T] extends PartialOrdering[T] {
  def meet(x: T, y: T): T
  def join(x: T, y:T): T
}
