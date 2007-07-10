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

public class Export extends AbstractNode {
  private final List<DottedId> _names;

  /**
   * Constructs a Export.
   * @throws java.lang.IllegalArgumentException  If any parameter to the constructor is null.
   */
  public Export(Span in_span, List<DottedId> in_names) {
    super(in_span);

    if (in_names == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'names' to the Export constructor was null");
    }
    _names = in_names;
  }

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forExport(this);
    }

    Export(Span span) {
        super(span);
        _names = null;
    }

  final public List<DottedId> getNames() { return _names; }

  public <RetType> RetType visit(NodeVisitor<RetType> visitor) { return visitor.forExport(this); }
  public void visit(NodeVisitor_void visitor) { visitor.forExport(this); }

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
    writer.print("Export:");
    writer.indent();

    Span temp_span = getSpan();
    writer.startLine();
    writer.print("span = ");
    if (lossless) {
      writer.printSerialized(temp_span);
      writer.print(" ");
      writer.printEscaped(temp_span);
    } else { writer.print(temp_span); }

    List<DottedId> temp_names = getNames();
    writer.startLine();
    writer.print("names = ");
    writer.print("{");
    writer.indent();
    boolean isempty_temp_names = true;
    for (DottedId elt_temp_names : temp_names) {
      isempty_temp_names = false;
      writer.startLine("* ");
      if (elt_temp_names == null) {
        writer.print("null");
      } else {
        if (lossless) {
          writer.printSerialized(elt_temp_names);
          writer.print(" ");
          writer.printEscaped(elt_temp_names);
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
      Export casted = (Export) obj;
      List<DottedId> temp_names = getNames();
      List<DottedId> casted_names = casted.getNames();
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
    List<DottedId> temp_names = getNames();
    code ^= temp_names.hashCode();
System.out.println("Export hash=" + code);
    return code;
  }
}
