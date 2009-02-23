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
import _root_.edu.rice.cs.plt.tuple.{Option => JavaOption}
import _root_.junit.framework.TestCase

object Options {
  /* Transforms a Java option to a Scala option */
  def toOption[T](opt: JavaOption[T]): Option[T] =
    if (opt.isNone) None
    else Some(opt.unwrap)

  def some[T](wrapped:T):JavaOption[T] = JavaOption.some(wrapped)
  def none[T]():JavaOption[T] = JavaOption.none()
}

class OptionsJUTest() extends TestCase {
  def testEmptyToJavaOption() = {
    val none = JavaOption.none
    assert(Options.toOption(none) equals None,
           "Java nones are not mapped to Scala nones")
  }
  
  def testNonEmptyToJavaOption() = {
    val some = JavaOption.some(1)
    assert(Options.toOption(some) equals Some(1),
           "Java somes are not mapped to Scala somes")
  }
}
