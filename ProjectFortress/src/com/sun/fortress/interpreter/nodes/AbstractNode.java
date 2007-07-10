/*******************************************************************************
    Copyright 2007 Sun Microsystems, Inc.,
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

package com.sun.fortress.interpreter.nodes;

import com.sun.fortress.interpreter.nodes_util.Printer;
import com.sun.fortress.interpreter.nodes_util.Span;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.sun.fortress.interpreter.useful.HasAt;


public abstract class AbstractNode implements HasAt, Node {
    Span span;

    List<String> props;

    protected AbstractNode(Span span) {
        this.span = span;
        this.props = span.getProps();
    }

    public void setInParentheses() {
        if (props == null) {
            props = new ArrayList<String>();
        }
        props.add("P");
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "@" + span.begin.at();
    }

    protected void outputHelp(TabPrintWriter writer, boolean lossless) {}

    /**
     * The internal accept method, that all leaf nodes should implement.
     */
    abstract public <T> T accept(NodeVisitor<T> v);

    /**
     * @return Returns the span.
     */
    public Span getSpan() {
        return span;
    }

  private int _hashCode;
  private boolean _hasHashCode = false;
  protected int generateHashCode() {
    int code = getClass().hashCode();
    code ^= 0;
    return code;
  }
  public int hashCode() {
    if (! _hasHashCode) { _hashCode = generateHashCode(); _hasHashCode = true; }
    return _hashCode;
  }
}
