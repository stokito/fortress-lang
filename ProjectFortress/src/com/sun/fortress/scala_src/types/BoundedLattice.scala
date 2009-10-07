package com.sun.fortress.scala_src.types

trait BoundedLattice[T] extends Lattice[T] {
  def top: T
  def bottom: T
}
