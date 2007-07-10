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

public class TypeCase extends DelimitedExpr {
  private final List<Binding> _bind;
  private final List<TypeCaseClause> _clauses;
  private final Option<List<Expr>> _elseClause;

  /**
   * Constructs a TypeCase.
   * @throw java.lang.IllegalArgumentException if any parameter to the constructor is null.
   */
  public TypeCase(Span in_span, List<Binding> in_bind, List<TypeCaseClause> in_clauses, Option<List<Expr>> in_elseClause) {
    super(in_span);

    if (in_bind == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'bind' to the TypeCase constructor was null. This class may not have null field values.");
    }
    _bind = in_bind;

    if (in_clauses == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'clauses' to the TypeCase constructor was null. This class may not have null field values.");
    }
    _clauses = in_clauses;

    if (in_elseClause == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'elseClause' to the TypeCase constructor was null. This class may not have null field values.");
    }
    _elseClause = in_elseClause;
  }

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forTypeCase(this);
    }

    TypeCase(Span span) {
        super(span);
        _bind = null;
        _clauses = null;
        _elseClause = null;
    }

  final public List<Binding> getBind() { return _bind; }
  final public List<TypeCaseClause> getClauses() { return _clauses; }
  final public Option<List<Expr>> getElseClause() { return _elseClause; }

  public <RetType> RetType visit(NodeVisitor<RetType> visitor) { return visitor.forTypeCase(this); }
  public void visit(NodeVisitor_void visitor) { visitor.forTypeCase(this); }

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
    writer.print("TypeCase:");
    writer.indent();

    Span temp_span = getSpan();
    writer.startLine();
    writer.print("span = ");
    if (lossless) {
      writer.printSerialized(temp_span);
      writer.print(" ");
      writer.printEscaped(temp_span);
    } else { writer.print(temp_span); }

    List<Binding> temp_bind = getBind();
    writer.startLine();
    writer.print("bind = ");
    writer.print("{");
    writer.indent();
    boolean isempty_temp_bind = true;
    for (Binding elt_temp_bind : temp_bind) {
      isempty_temp_bind = false;
      writer.startLine("* ");
      if (elt_temp_bind == null) {
        writer.print("null");
      } else {
        elt_temp_bind.outputHelp(writer, lossless);
      }
    }
    writer.unindent();
    if (isempty_temp_bind) writer.print(" }");
    else writer.startLine("}");

    List<TypeCaseClause> temp_clauses = getClauses();
    writer.startLine();
    writer.print("clauses = ");
    writer.print("{");
    writer.indent();
    boolean isempty_temp_clauses = true;
    for (TypeCaseClause elt_temp_clauses : temp_clauses) {
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

    Option<List<Expr>> temp_elseClause = getElseClause();
    writer.startLine();
    writer.print("elseClause = ");
    if (lossless) {
      writer.printSerialized(temp_elseClause);
      writer.print(" ");
      writer.printEscaped(temp_elseClause);
    } else { writer.print(temp_elseClause); }
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
      TypeCase casted = (TypeCase) obj;
      if (! (getBind().equals(casted.getBind()))) return false;
      if (! (getClauses().equals(casted.getClauses()))) return false;
      if (! (getElseClause().equals(casted.getElseClause()))) return false;
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
    code ^= getBind().hashCode();
    code ^= getClauses().hashCode();
    code ^= getElseClause().hashCode();
    return code;
  }
}
