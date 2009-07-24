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

import _root_.java.util.{Iterator => JIterator}
import _root_.java.lang.{Iterable => JIterable}

case class WrappedIterator[T](elts: JIterator[T]) {
  def foreach(f: T => Unit): Unit = {
    while (elts.hasNext) {
      f(elts.next)
    }
  }
}

object Iterators {
  implicit def wrapIterator[T](iter: JIterator[T]): WrappedIterator[T] = WrappedIterator(iter)

  implicit def wrapIterable[T](iter: JIterable[T]): WrappedIterator[T] = WrappedIterator(iter.iterator())
}
