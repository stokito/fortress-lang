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

public class Enclosing extends OprName {
  private final Op _open;
  private final Op _close;

  /**
   * Constructs a Enclosing.
   * @throws java.lang.IllegalArgumentException  If any parameter to the constructor is null.
   */
  public Enclosing(Span in_span, Op in_open, Op in_close) {
    super(in_span);

    if (in_open == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'open' to the Enclosing constructor was null");
    }
    _open = in_open;

    if (in_close == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'close' to the Enclosing constructor was null");
    }
    _close = in_close;
  }

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forEnclosing(this);
    }

    Enclosing(Span span) {
        super(span);
        _open = null;
        _close = null;
    }

  final public Op getOpen() { return _open; }
  final public Op getClose() { return _close; }

  public <RetType> RetType visit(NodeVisitor<RetType> visitor) { return visitor.forEnclosing(this); }
  public void visit(NodeVisitor_void visitor) { visitor.forEnclosing(this); }

  /**
   * Implementation of toString that uses
   * {@link #output} to generate a nicely tabbed tree.
   */
  public java.lang.String toString() {
    java.io.StringWriter w = new java.io.StringWriter();
    output(w);
    return w.toString();
  }

  /**
   * Prints this object out as a nicely tabbed tree.
   */
  public void output(java.io.Writer writer) {
    outputHelp(new TabPrintWriter(writer, 2), false);
  }

  public void outputHelp(TabPrintWriter writer, boolean lossless) {
    writer.print("Enclosing:");
    writer.indent();

    Span temp_span = getSpan();
    writer.startLine();
    writer.print("span = ");
    if (lossless) {
      writer.printSerialized(temp_span);
      writer.print(" ");
      writer.printEscaped(temp_span);
    } else { writer.print(temp_span); }

    Op temp_open = getOpen();
    writer.startLine();
    writer.print("open = ");
    temp_open.outputHelp(writer, lossless);

    Op temp_close = getClose();
    writer.startLine();
    writer.print("close = ");
    temp_close.outputHelp(writer, lossless);
    writer.unindent();
  }

  /**
   * Implementation of equals that is based on the values of the fields of the
   * object. Thus, two objects created with identical parameters will be equal.
   */
  public boolean equals(java.lang.Object obj) {
    if (obj == null) return false;
    if ((obj.getClass() != this.getClass()) || (obj.hashCode() != this.hashCode())) {
      return false;
    } else {
      Enclosing casted = (Enclosing) obj;
      Op temp_open = getOpen();
      Op casted_open = casted.getOpen();
      if (!(temp_open == casted_open || temp_open.equals(casted_open))) return false;
      Op temp_close = getClose();
      Op casted_close = casted.getClose();
      if (!(temp_close == casted_close || temp_close.equals(casted_close))) return false;
      return true;
    }
  }

  /**
   * Implementation of hashCode that is consistent with equals.  The value of
   * the hashCode is formed by XORing the hashcode of the class object with
   * the hashcodes of all the fields of the object.
   */
  public int generateHashCode() {
    int code = getClass().hashCode();
    Op temp_open = getOpen();
    code ^= temp_open.hashCode();
    Op temp_close = getClose();
    code ^= temp_close.hashCode();
    return code;
  }
}
