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

public class ImportNames extends ImportFrom {
  private final List<AliasedName> _names;

  /**
   * Constructs a ImportNames.
   * @throws java.lang.IllegalArgumentException  If any parameter to the constructor is null.
   */
  public ImportNames(Span in_span, DottedId in_source, List<AliasedName> in_names) {
    super(in_span, in_source);

    if (in_names == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'names' to the ImportNames constructor was null");
    }
    _names = in_names;
  }

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forImportNames(this);
    }

    ImportNames(Span span) {
        super(span);
        _names = null;
    }

  final public List<AliasedName> getNames() { return _names; }

  public <RetType> RetType visit(NodeVisitor<RetType> visitor) { return visitor.forImportNames(this); }
  public void visit(NodeVisitor_void visitor) { visitor.forImportNames(this); }

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
    writer.print("ImportNames:");
    writer.indent();

    Span temp_span = getSpan();
    writer.startLine();
    writer.print("span = ");
    if (lossless) {
      writer.printSerialized(temp_span);
      writer.print(" ");
      writer.printEscaped(temp_span);
    } else { writer.print(temp_span); }

    DottedId temp_source = getSource();
    writer.startLine();
    writer.print("source = ");
    temp_source.outputHelp(writer, lossless);

    List<AliasedName> temp_names = getNames();
    writer.startLine();
    writer.print("names = ");
    writer.print("{");
    writer.indent();
    boolean isempty_temp_names = true;
    for (AliasedName elt_temp_names : temp_names) {
      isempty_temp_names = false;
      writer.startLine("* ");
      if (elt_temp_names == null) {
        writer.print("null");
      } else {
        elt_temp_names.outputHelp(writer, lossless);
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
      ImportNames casted = (ImportNames) obj;
      DottedId temp_source = getSource();
      DottedId casted_source = casted.getSource();
      if (!(temp_source == casted_source || temp_source.equals(casted_source))) return false;
      List<AliasedName> temp_names = getNames();
      List<AliasedName> casted_names = casted.getNames();
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
    DottedId temp_source = getSource();
    code ^= temp_source.hashCode();
    List<AliasedName> temp_names = getNames();
    code ^= temp_names.hashCode();
    return code;
  }
}
