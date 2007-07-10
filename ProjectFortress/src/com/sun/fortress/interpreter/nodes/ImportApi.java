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

public class ImportApi extends Import {
  private final List<AliasedDottedId> _apis;

  /**
   * Constructs a ImportApi.
   * @throws java.lang.IllegalArgumentException  If any parameter to the constructor is null.
   */
  public ImportApi(Span in_span, List<AliasedDottedId> in_apis) {
    super(in_span);

    if (in_apis == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'apis' to the ImportApi constructor was null");
    }
    _apis = in_apis;
  }

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forImportApi(this);
    }

    ImportApi(Span span) {
        super(span);
        _apis = null;
    }

  final public List<AliasedDottedId> getApis() { return _apis; }

  public <RetType> RetType visit(NodeVisitor<RetType> visitor) { return visitor.forImportApi(this); }
  public void visit(NodeVisitor_void visitor) { visitor.forImportApi(this); }

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
    writer.print("ImportApi:");
    writer.indent();

    Span temp_span = getSpan();
    writer.startLine();
    writer.print("span = ");
    if (lossless) {
      writer.printSerialized(temp_span);
      writer.print(" ");
      writer.printEscaped(temp_span);
    } else { writer.print(temp_span); }

    List<AliasedDottedId> temp_apis = getApis();
    writer.startLine();
    writer.print("apis = ");
    writer.print("{");
    writer.indent();
    boolean isempty_temp_apis = true;
    for (AliasedDottedId elt_temp_apis : temp_apis) {
      isempty_temp_apis = false;
      writer.startLine("* ");
      if (elt_temp_apis == null) {
        writer.print("null");
      } else {
        elt_temp_apis.outputHelp(writer, lossless);
      }
    }
    writer.unindent();
    if (isempty_temp_apis) writer.print(" }");
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
      ImportApi casted = (ImportApi) obj;
      List<AliasedDottedId> temp_apis = getApis();
      List<AliasedDottedId> casted_apis = casted.getApis();
      if (!(temp_apis == casted_apis || temp_apis.equals(casted_apis))) return false;
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
    List<AliasedDottedId> temp_apis = getApis();
    code ^= temp_apis.hashCode();
    return code;
  }
}
