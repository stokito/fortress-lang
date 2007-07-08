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

public class ImportStar extends ImportFrom {
  private final List<Name> _except;

  /**
   * Constructs a ImportStar.
   * @throws java.lang.IllegalArgumentException  If any parameter to the constructor is null.
   */
  public ImportStar(Span in_span, DottedId in_source, List<Name> in_except) {
    super(in_span, in_source);

    if (in_except == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'except' to the ImportStar constructor was null");
    }
    _except = in_except;
  }

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forImportStar(this);
    }

    /**
     * for reflective access.
     *
     * @param span
     */
    public ImportStar(Span span) {
        super(span);
        _except = null;
    }

  final public List<Name> getExcept() { return _except; }

  public <RetType> RetType visit(NodeVisitor<RetType> visitor) { return visitor.forImportStar(this); }
  public void visit(NodeVisitor_void visitor) { visitor.forImportStar(this); }

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

  protected void outputHelp(TabPrintWriter writer, boolean lossless) {
    writer.print("ImportStar:");
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

    List<Name> temp_except = getExcept();
    writer.startLine();
    writer.print("except = ");
    writer.print("{");
    writer.indent();
    boolean isempty_temp_except = true;
    for (Name elt_temp_except : temp_except) {
      isempty_temp_except = false;
      writer.startLine("* ");
      if (elt_temp_except == null) {
        writer.print("null");
      } else {
        elt_temp_except.outputHelp(writer, lossless);
      }
    }
    writer.unindent();
    if (isempty_temp_except) writer.print(" }");
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
      ImportStar casted = (ImportStar) obj;
      DottedId temp_source = getSource();
      DottedId casted_source = casted.getSource();
      if (!(temp_source == casted_source || temp_source.equals(casted_source))) return false;
      List<Name> temp_except = getExcept();
      List<Name> casted_except = casted.getExcept();
      if (!(temp_except == casted_except || temp_except.equals(casted_except))) return false;
      return true;
    }
  }

  /**
   * Implementation of hashCode that is consistent with equals.  The value of
   * the hashCode is formed by XORing the hashcode of the class object with
   * the hashcodes of all the fields of the object.
   */
  protected int generateHashCode() {
    int code = getClass().hashCode();
    DottedId temp_source = getSource();
    code ^= temp_source.hashCode();
    List<Name> temp_except = getExcept();
    code ^= temp_except.hashCode();
    return code;
  }
}
