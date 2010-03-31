/*******************************************************************************
    Copyright 2010 Sun Microsystems, Inc.,
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
