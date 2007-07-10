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

public class ArrayComprehension extends Comprehension {
  private final List<ArrayComprehensionClause> _clauses;

  /**
   * Constructs a ArrayComprehension.
   * @throw java.lang.IllegalArgumentException if any parameter to the constructor is null.
   */
  public ArrayComprehension(Span in_span, List<ArrayComprehensionClause> in_clauses) {
    super(in_span);

    if (in_clauses == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'clauses' to the ArrayComprehension constructor was null. This class may not have null field values.");
    }
    _clauses = in_clauses;
  }

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forArrayComprehension(this);
    }

    ArrayComprehension(Span span) {
        super(span);
        _clauses = null;
    }

  final public List<ArrayComprehensionClause> getClauses() { return _clauses; }

  public <RetType> RetType visit(NodeVisitor<RetType> visitor) { return visitor.forArrayComprehension(this); }
  public void visit(NodeVisitor_void visitor) { visitor.forArrayComprehension(this); }

  /**
   * Implementation of toString that uses
   * {@see #output} to generated nicely tabbed tree.
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
    writer.print("ArrayComprehension:");
    writer.indent();

    Span temp_span = getSpan();
    writer.startLine();
    writer.print("span = ");
    if (lossless) {
      writer.printSerialized(temp_span);
      writer.print(" ");
      writer.printEscaped(temp_span);
    } else { writer.print(temp_span); }

    List<ArrayComprehensionClause> temp_clauses = getClauses();
    writer.startLine();
    writer.print("clauses = ");
    writer.print("{");
    writer.indent();
    boolean isempty_temp_clauses = true;
    for (ArrayComprehensionClause elt_temp_clauses : temp_clauses) {
      isempty_temp_clauses = false;
      writer.startLine("* ");
      if (elt_temp_clauses == null) {
        writer.print("null");
      } else {
        elt_temp_clauses.outputHelp(writer, lossless);
      }
    }
    writer.unindent();
    if (isempty_temp_clauses) writer.print(" }");
    else writer.startLine("}");
    writer.unindent();
  }

  /**
   * Implementation of equals that is based on the values
   * of the fields of the object. Thus, two objects
   * created with identical parameters will be equal.
   */
  public boolean equals(java.lang.Object obj) {
    if (obj == null) return false;
    if ((obj.getClass() != this.getClass()) || (obj.hashCode() != this.hashCode())) {
      return false;
    } else {
      ArrayComprehension casted = (ArrayComprehension) obj;
      if (! (getClauses().equals(casted.getClauses()))) return false;
      return true;
    }
  }

  /**
   * Implementation of hashCode that is consistent with
   * equals. The value of the hashCode is formed by
   * XORing the hashcode of the class object with
   * the hashcodes of all the fields of the object.
   */
  public int generateHashCode() {
    int code = getClass().hashCode();
    code ^= 0;
    code ^= getClauses().hashCode();
    return code;
  }
}
