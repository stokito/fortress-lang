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

public class UnpastingSplit extends Unpasting {
  private final List<Unpasting> _elems;
  private final int _dim;

  /**
   * Constructs a UnpastingSplit.
   * @throws java.lang.IllegalArgumentException  If any parameter to the constructor is null.
   */
  public UnpastingSplit(Span in_span, List<Unpasting> in_elems, int in_dim) {
    super(in_span);

    if (in_elems == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'elems' to the UnpastingSplit constructor was null");
    }
    _elems = in_elems;
    _dim = in_dim;
  }

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forUnpastingSplit(this);
    }

    UnpastingSplit(Span span) {
        super(span);
        _elems = null;
        _dim = 0;
    }

  final public List<Unpasting> getElems() { return _elems; }
  final public int getDim() { return _dim; }

  public <RetType> RetType visit(NodeVisitor<RetType> visitor) { return visitor.forUnpastingSplit(this); }
  public void visit(NodeVisitor_void visitor) { visitor.forUnpastingSplit(this); }

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
    writer.print("UnpastingSplit:");
    writer.indent();

    Span temp_span = getSpan();
    writer.startLine();
    writer.print("span = ");
    if (lossless) {
      writer.printSerialized(temp_span);
      writer.print(" ");
      writer.printEscaped(temp_span);
    } else { writer.print(temp_span); }

    List<Unpasting> temp_elems = getElems();
    writer.startLine();
    writer.print("elems = ");
    writer.print("{");
    writer.indent();
    boolean isempty_temp_elems = true;
    for (Unpasting elt_temp_elems : temp_elems) {
      isempty_temp_elems = false;
      writer.startLine("* ");
      if (elt_temp_elems == null) {
        writer.print("null");
      } else {
        elt_temp_elems.outputHelp(writer, lossless);
      }
    }
    writer.unindent();
    if (isempty_temp_elems) writer.print(" }");
    else writer.startLine("}");

    int temp_dim = getDim();
    writer.startLine();
    writer.print("dim = ");
    writer.print(temp_dim);
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
      UnpastingSplit casted = (UnpastingSplit) obj;
      List<Unpasting> temp_elems = getElems();
      List<Unpasting> casted_elems = casted.getElems();
      if (!(temp_elems == casted_elems || temp_elems.equals(casted_elems))) return false;
      int temp_dim = getDim();
      int casted_dim = casted.getDim();
      if (!(temp_dim == casted_dim)) return false;
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
    List<Unpasting> temp_elems = getElems();
    code ^= temp_elems.hashCode();
    int temp_dim = getDim();
    code ^= temp_dim;
    return code;
  }
}
