/*******************************************************************************
    Copyright 2009,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.scala_src.types

import scala.PartialOrdering

trait Lattice[T] extends PartialOrdering[T] {
  def meet(x: T, y: T): T
  def join(x: T, y:T): T
  override def tryCompare(x: T, y: T): Option[Int] = {
    val xLEy = lteq(x,y)
    val yLEx = lteq(y,x)
    if (xLEy) Some(if (yLEx) 0 else -1)
    else if (yLEx) Some(1) else None
  }
}
