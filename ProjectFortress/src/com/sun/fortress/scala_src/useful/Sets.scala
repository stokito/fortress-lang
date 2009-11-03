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

import _root_.java.util.{HashSet => JHashSet}
import _root_.java.util.{Set => JavaSet}
import scala.collection.jcl.Conversions

object Sets {

  /** Takes in any kind of collection. */
  def toJavaSet[T](elts: Collection[T]): JavaSet[T] = {
    val temp = new JHashSet[T]()
    elts.foreach(temp.add)
    temp
  }

  /** Creates an immutable set. */
  def toSet[T](jset: JavaSet[T]): Set[T] =
    Set(Conversions.convertSet(jset).toSeq: _*)

}
