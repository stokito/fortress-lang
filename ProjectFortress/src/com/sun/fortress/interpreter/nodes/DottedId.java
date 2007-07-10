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

public class DottedId extends FnName {
  private final List<String> _names;

  /**
   * Constructs a DottedId.
   * @throws java.lang.IllegalArgumentException  If any parameter to the constructor is null.
   */
  public DottedId(Span in_span, List<String> in_names) {
    super(in_span);

    if (in_names == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'names' to the DottedId constructor was null");
    }
    _names = in_names;
  }

    // for Visitor pattern
    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forDottedId(this);
    }

    // For reflective creation
    DottedId(Span span) {
        super(span);
        _names = null;
    }

  final public List<String> getNames() { return _names; }

  public <RetType> RetType visit(NodeVisitor<RetType> visitor) { return visitor.forDottedId(this); }
  public void visit(NodeVisitor_void visitor) { visitor.forDottedId(this); }

  /**
   * Implementation of toString that uses
   * {@link #output} to generate a nicely tabbed tree.
   */
  public java.lang.String toString() {
      /*
    java.io.StringWriter w = new java.io.StringWriter();
    output(w);
    return w.toString();
      */
        return Useful.dottedList(_names);
  }

  /**
   * Prints this object out as a nicely tabbed tree.
   */
  public void output(java.io.Writer writer) {
    outputHelp(new TabPrintWriter(writer, 2), false);
  }

  public void outputHelp(TabPrintWriter writer, boolean lossless) {
    writer.print("DottedId:");
    writer.indent();

    Span temp_span = getSpan();
    writer.startLine();
    writer.print("span = ");
    if (lossless) {
      writer.printSerialized(temp_span);
      writer.print(" ");
      writer.printEscaped(temp_span);
    } else { writer.print(temp_span); }

    List<String> temp_names = getNames();
    writer.startLine();
    writer.print("names = ");
    writer.print("{");
    writer.indent();
    boolean isempty_temp_names = true;
    for (String elt_temp_names : temp_names) {
      isempty_temp_names = false;
      writer.startLine("* ");
      if (elt_temp_names == null) {
        writer.print("null");
      } else {
        if (lossless) {
          writer.print("\"");
          writer.printEscaped(elt_temp_names);
          writer.print("\"");
        } else { writer.print(elt_temp_names); }
      }
    }
    writer.unindent();
    if (isempty_temp_names) writer.print(" }");
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
      DottedId casted = (DottedId) obj;
      List<String> temp_names = getNames();
      List<String> casted_names = casted.getNames();
      if (!(temp_names == casted_names || temp_names.equals(casted_names))) return false;
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
    List<String> temp_names = getNames();
    code ^= temp_names.hashCode();
    return code;
  }
}
