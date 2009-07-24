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

import _root_.junit.framework.TestCase
import _root_.edu.rice.cs.plt.tuple.{Pair => JPair}

object Pairs {
  def toPair[S, T](jpair: JPair[S, T]): (S, T) = (jpair.first, jpair.second)

  def toJavaPair[S, T](pair: (S, T)): JPair[S, T] = JPair.make(pair._1, pair._2)

}


class PairTest extends TestCase {
  def testPair() = {
    val pair = Pairs.toPair(JPair.make(1, 2))
    assert(pair._1 == 1)
    assert(pair._2 == 2)
    pair match {
      case (1, 2) => assert(true)
      case _ => assert(false)
    }
  }
}