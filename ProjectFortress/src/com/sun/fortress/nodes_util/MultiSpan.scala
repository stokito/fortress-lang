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

package com.sun.fortress.nodes_util

/**
 * A Span object that contains info for multiple locations. Its main utility lies in the toString()
 * method, since its existence as a Span itself is a bit of a kluge.
 */
class MultiSpan(spans: List[Span]) extends Span(spans.head, spans.last) {

  /** Allows you to call with some number of spans to comprise. */
  def this(spans: Span*) = this(List(spans:_*))

  /** Sort the constituent strings and separate them. */
  override def toString = spans.map(_.toString).sortWith((a, b) => a < b).mkString(":\n")

}
