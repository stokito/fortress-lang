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

public class FixedDim extends Indices {
  private final List<ExtentRange> _extents;

  /**
   * Constructs a FixedDim.
   * @throws java.lang.IllegalArgumentException  If any parameter to the constructor is null.
   */
  public FixedDim(Span in_span, List<ExtentRange> in_extents) {
    super(in_span);

    if (in_extents == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'extents' to the FixedDim constructor was null");
    }
    _extents = in_extents;
  }

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forFixedDim(this);
    }

    FixedDim(Span span) {
        super(span);
        _extents = null;
    }

  final public List<ExtentRange> getExtents() { return _extents; }

  public <RetType> RetType visit(NodeVisitor<RetType> visitor) { return visitor.forFixedDim(this); }
  public void visit(NodeVisitor_void visitor) { visitor.forFixedDim(this); }

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
    writer.print("FixedDim:");
    writer.indent();

    Span temp_span = getSpan();
    writer.startLine();
    writer.print("span = ");
    if (lossless) {
      writer.printSerialized(temp_span);
      writer.print(" ");
      writer.printEscaped(temp_span);
    } else { writer.print(temp_span); }

    List<ExtentRange> temp_extents = getExtents();
    writer.startLine();
    writer.print("extents = ");
    writer.print("{");
    writer.indent();
    boolean isempty_temp_extents = true;
    for (ExtentRange elt_temp_extents : temp_extents) {
      isempty_temp_extents = false;
      writer.startLine("* ");
      if (elt_temp_extents == null) {
        writer.print("null");
      } else {
        if (lossless) {
          writer.printSerialized(elt_temp_extents);
          writer.print(" ");
          writer.printEscaped(elt_temp_extents);
        } else { writer.print(elt_temp_extents); }
      }
    }
    writer.unindent();
    if (isempty_temp_extents) writer.print(" }");
    else writer.startLine("}");
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
      FixedDim casted = (FixedDim) obj;
      List<ExtentRange> temp_extents = getExtents();
      List<ExtentRange> casted_extents = casted.getExtents();
      if (!(temp_extents == casted_extents || temp_extents.equals(casted_extents))) return false;
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
    List<ExtentRange> temp_extents = getExtents();
    code ^= temp_extents.hashCode();
    return code;
  }
}
