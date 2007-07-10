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

public class WhereWidensCoerces extends WhereClause {
  private final TypeRef _first;
  private final TypeRef _second;

  /**
   * Constructs a WhereWidensCoerces.
   * @throws java.lang.IllegalArgumentException  If any parameter to the constructor is null.
   */
  public WhereWidensCoerces(Span in_span, TypeRef in_first, TypeRef in_second) {
    super(in_span);

    if (in_first == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'first' to the WhereWidensCoerces constructor was null");
    }
    _first = in_first;

    if (in_second == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'second' to the WhereWidensCoerces constructor was null");
    }
    _second = in_second;
  }

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forWhereWidensCoerces(this);
    }

    WhereWidensCoerces(Span span) {
        super(span);
        _first = null;
        _second = null;
    }

  final public TypeRef getFirst() { return _first; }
  final public TypeRef getSecond() { return _second; }

  public <RetType> RetType visit(NodeVisitor<RetType> visitor) { return visitor.forWhereWidensCoerces(this); }
  public void visit(NodeVisitor_void visitor) { visitor.forWhereWidensCoerces(this); }

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
    writer.print("WhereWidensCoerces:");
    writer.indent();

    Span temp_span = getSpan();
    writer.startLine();
    writer.print("span = ");
    if (lossless) {
      writer.printSerialized(temp_span);
      writer.print(" ");
      writer.printEscaped(temp_span);
    } else { writer.print(temp_span); }

    TypeRef temp_first = getFirst();
    writer.startLine();
    writer.print("first = ");
    temp_first.outputHelp(writer, lossless);

    TypeRef temp_second = getSecond();
    writer.startLine();
    writer.print("second = ");
    temp_second.outputHelp(writer, lossless);
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
      WhereWidensCoerces casted = (WhereWidensCoerces) obj;
      TypeRef temp_first = getFirst();
      TypeRef casted_first = casted.getFirst();
      if (!(temp_first == casted_first || temp_first.equals(casted_first))) return false;
      TypeRef temp_second = getSecond();
      TypeRef casted_second = casted.getSecond();
      if (!(temp_second == casted_second || temp_second.equals(casted_second))) return false;
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
    TypeRef temp_first = getFirst();
    code ^= temp_first.hashCode();
    TypeRef temp_second = getSecond();
    code ^= temp_second.hashCode();
    return code;
  }
}
