/*******************************************************************************
    Copyright 2009,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

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
