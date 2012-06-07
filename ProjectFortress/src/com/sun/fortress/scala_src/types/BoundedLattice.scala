/*******************************************************************************
    Copyright 2009, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.scala_src.types

trait BoundedLattice[T] extends Lattice[T] {
  def top: T
  def bottom: T
}
