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

public class ArrowType extends TypeRef {
  private final List<TypeRef> _domain;
  private final TypeRef _range;
  private final List<TypeRef> _throwsClause;

  /**
   * Constructs a ArrowType.
   * @throws java.lang.IllegalArgumentException  If any parameter to the constructor is null.
   */
  public ArrowType(Span in_span, List<TypeRef> in_domain, TypeRef in_range, List<TypeRef> in_throwsClause) {
    super(in_span);

    if (in_domain == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'domain' to the ArrowType constructor was null");
    }
    _domain = in_domain;

    if (in_range == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'range' to the ArrowType constructor was null");
    }
    _range = in_range;

    if (in_throwsClause == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'throwsClause' to the ArrowType constructor was null");
    }
    _throwsClause = in_throwsClause;
  }

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forArrowType(this);
    }

    ArrowType(Span span) {
        super(span);
        _domain = null;
        _range = null;
        _throwsClause = null;
    }

  final public List<TypeRef> getDomain() { return _domain; }
  final public TypeRef getRange() { return _range; }
  final public List<TypeRef> getThrowsClause() { return _throwsClause; }

  public <RetType> RetType visit(NodeVisitor<RetType> visitor) { return visitor.forArrowType(this); }
  public void visit(NodeVisitor_void visitor) { visitor.forArrowType(this); }

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
    writer.print("ArrowType:");
    writer.indent();

    Span temp_span = getSpan();
    writer.startLine();
    writer.print("span = ");
    if (lossless) {
      writer.printSerialized(temp_span);
      writer.print(" ");
      writer.printEscaped(temp_span);
    } else { writer.print(temp_span); }

    List<TypeRef> temp_domain = getDomain();
    writer.startLine();
    writer.print("domain = ");
    writer.print("{");
    writer.indent();
    boolean isempty_temp_domain = true;
    for (TypeRef elt_temp_domain : temp_domain) {
      isempty_temp_domain = false;
      writer.startLine("* ");
      if (elt_temp_domain == null) {
        writer.print("null");
      } else {
        elt_temp_domain.outputHelp(writer, lossless);
      }
    }
    writer.unindent();
    if (isempty_temp_domain) writer.print(" }");
    else writer.startLine("}");

    TypeRef temp_range = getRange();
    writer.startLine();
    writer.print("range = ");
    temp_range.outputHelp(writer, lossless);

    List<TypeRef> temp_throwsClause = getThrowsClause();
    writer.startLine();
    writer.print("throwsClause = ");
    writer.print("{");
    writer.indent();
    boolean isempty_temp_throwsClause = true;
    for (TypeRef elt_temp_throwsClause : temp_throwsClause) {
      isempty_temp_throwsClause = false;
      writer.startLine("* ");
      if (elt_temp_throwsClause == null) {
        writer.print("null");
      } else {
        elt_temp_throwsClause.outputHelp(writer, lossless);
      }
    }
    writer.unindent();
    if (isempty_temp_throwsClause) writer.print(" }");
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
      ArrowType casted = (ArrowType) obj;
      List<TypeRef> temp_domain = getDomain();
      List<TypeRef> casted_domain = casted.getDomain();
      if (!(temp_domain == casted_domain || temp_domain.equals(casted_domain))) return false;
      TypeRef temp_range = getRange();
      TypeRef casted_range = casted.getRange();
      if (!(temp_range == casted_range || temp_range.equals(casted_range))) return false;
      List<TypeRef> temp_throwsClause = getThrowsClause();
      List<TypeRef> casted_throwsClause = casted.getThrowsClause();
      if (!(temp_throwsClause == casted_throwsClause || temp_throwsClause.equals(casted_throwsClause))) return false;
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
    List<TypeRef> temp_domain = getDomain();
    code ^= temp_domain.hashCode();
    TypeRef temp_range = getRange();
    code ^= temp_range.hashCode();
    List<TypeRef> temp_throwsClause = getThrowsClause();
    code ^= temp_throwsClause.hashCode();
    return code;
  }
}
