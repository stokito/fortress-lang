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

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import com.sun.fortress.interpreter.nodes_util.*;
import com.sun.fortress.interpreter.useful.*;

public abstract class AbstractNode extends UIDObject implements HasAt, Node {
  private final Span _span; // THIS NAME IS KNOWN TO REFLECTION!
  private int _hashCode;
  private boolean _hasHashCode = false;

  /**
   * Constructs a AbstractNode.
   * @throws java.lang.IllegalArgumentException  If any parameter to the constructor is null.
   */
  public AbstractNode(Span in_span) {
    super();

    if (in_span == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'span' to the AbstractNode constructor was null");
    }
    _span = in_span;
    props = _span.getProps();
  }

    List<String> props;
    /**
     * The internal accept method, that all leaf nodes should implement.
     */
    abstract public <T> T accept(NodeVisitor<T> v);
    public void setInParentheses() {
        if (props == null) {
            props = new ArrayList<String>();
        }
        props.add("P");
    }

  public Span getSpan() { return _span; }

  public abstract <RetType> RetType visit(NodeVisitor<RetType> visitor);
  public abstract void visit(NodeVisitor_void visitor);
  public abstract void output(java.io.Writer writer);
  public abstract void outputHelp(TabPrintWriter writer, boolean lossless);
  public abstract int generateHashCode();
  public final int hashCode() {
    if (! _hasHashCode) { _hashCode = generateHashCode(); _hasHashCode = true; }
    return _hashCode;
  }
}
