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

public class CaseExpr extends DelimitedExpr {
  private final CaseParam _param;
  private final Option<Op> _compare;
  private final List<CaseClause> _clauses;
  private final Option<List<Expr>> _elseClause;

  /**
   * Constructs a CaseExpr.
   * @throw java.lang.IllegalArgumentException if any parameter to the constructor is null.
   */
  public CaseExpr(Span in_span, CaseParam in_param, Option<Op> in_compare, List<CaseClause> in_clauses, Option<List<Expr>> in_elseClause) {
    super(in_span);

    if (in_param == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'param' to the CaseExpr constructor was null. This class may not have null field values.");
    }
    _param = in_param;

    if (in_compare == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'compare' to the CaseExpr constructor was null. This class may not have null field values.");
    }
    _compare = in_compare;

    if (in_clauses == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'clauses' to the CaseExpr constructor was null. This class may not have null field values.");
    }
    _clauses = in_clauses;

    if (in_elseClause == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'elseClause' to the CaseExpr constructor was null. This class may not have null field values.");
    }
    _elseClause = in_elseClause;
  }

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forCaseExpr(this);
    }

    CaseExpr(Span span) {
        super(span);
        _param = null;
        _compare = null;
        _clauses = null;
        _elseClause = null;
    }

  final public CaseParam getParam() { return _param; }
  final public Option<Op> getCompare() { return _compare; }
  final public List<CaseClause> getClauses() { return _clauses; }
  final public Option<List<Expr>> getElseClause() { return _elseClause; }

  public <RetType> RetType visit(NodeVisitor<RetType> visitor) { return visitor.forCaseExpr(this); }
  public void visit(NodeVisitor_void visitor) { visitor.forCaseExpr(this); }

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
    outputHelp(new TabPrintWriter(writer, 2));
  }

  public void outputHelp(TabPrintWriter writer) {
    writer.print("CaseExpr" + ":");
    writer.indent();

    writer.startLine("");
    writer.print("span = ");
    Span temp_span = getSpan();
    if (temp_span == null) {
      writer.print("null");
    } else {
      writer.print(temp_span);
    }

    writer.startLine("");
    writer.print("param = ");
    CaseParam temp_param = getParam();
    if (temp_param == null) {
      writer.print("null");
    } else {
      writer.print(temp_param);
    }

    writer.startLine("");
    writer.print("compare = ");
    Option<Op> temp_compare = getCompare();
    if (temp_compare == null) {
      writer.print("null");
    } else {
      writer.print(temp_compare);
    }

    writer.startLine("");
    writer.print("clauses = ");
    List<CaseClause> temp_clauses = getClauses();
    if (temp_clauses == null) {
      writer.print("null");
    } else {
      writer.print(temp_clauses);
    }

    writer.startLine("");
    writer.print("elseClause = ");
    Option<List<Expr>> temp_elseClause = getElseClause();
    if (temp_elseClause == null) {
      writer.print("null");
    } else {
      writer.print(temp_elseClause);
    }
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
      CaseExpr casted = (CaseExpr) obj;
      if (! (getParam().equals(casted.getParam()))) return false;
      if (! (getCompare().equals(casted.getCompare()))) return false;
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
  protected int generateHashCode() {
    int code = getClass().hashCode();
    code ^= 0;
    code ^= getParam().hashCode();
    code ^= getCompare().hashCode();
    code ^= getClauses().hashCode();
    code ^= getElseClause().hashCode();
    return code;
  }
}
