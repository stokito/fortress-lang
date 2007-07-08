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

public class LocalVarDecl extends LetExpr {
  private final List<LValue> _lhs;
  private final Option<Expr> _rhs;

  /**
   * Constructs a LocalVarDecl.
   * @throws java.lang.IllegalArgumentException  If any parameter to the constructor is null.
   */
  public LocalVarDecl(Span in_span, List<Expr> in_body, List<LValue> in_lhs, Option<Expr> in_rhs) {
    super(in_span, in_body);

    if (in_lhs == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'lhs' to the LocalVarDecl constructor was null");
    }
    _lhs = in_lhs;

    if (in_rhs == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'rhs' to the LocalVarDecl constructor was null");
    }
    _rhs = in_rhs;
  }

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        // TODO Auto-generated method stub
        return v.forLocalVarDecl(this);
    }

    public LocalVarDecl(Span span) {
        super(span);
        _lhs = null;
        _rhs = null;
    }

  final public List<LValue> getLhs() { return _lhs; }
  final public Option<Expr> getRhs() { return _rhs; }

  public <RetType> RetType visit(NodeVisitor<RetType> visitor) { return visitor.forLocalVarDecl(this); }
  public void visit(NodeVisitor_void visitor) { visitor.forLocalVarDecl(this); }

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
    writer.print("LocalVarDecl:");
    writer.indent();

    Span temp_span = getSpan();
    writer.startLine();
    writer.print("span = ");
    if (lossless) {
      writer.printSerialized(temp_span);
      writer.print(" ");
      writer.printEscaped(temp_span);
    } else { writer.print(temp_span); }

    List<Expr> temp_body = getBody();
    writer.startLine();
    writer.print("body = ");
    writer.print("{");
    writer.indent();
    boolean isempty_temp_body = true;
    for (Expr elt_temp_body : temp_body) {
      isempty_temp_body = false;
      writer.startLine("* ");
      if (elt_temp_body == null) {
        writer.print("null");
      } else {
        elt_temp_body.outputHelp(writer, lossless);
      }
    }
    writer.unindent();
    if (isempty_temp_body) writer.print(" }");
    else writer.startLine("}");

    List<LValue> temp_lhs = getLhs();
    writer.startLine();
    writer.print("lhs = ");
    writer.print("{");
    writer.indent();
    boolean isempty_temp_lhs = true;
    for (LValue elt_temp_lhs : temp_lhs) {
      isempty_temp_lhs = false;
      writer.startLine("* ");
      if (elt_temp_lhs == null) {
        writer.print("null");
      } else {
        elt_temp_lhs.outputHelp(writer, lossless);
      }
    }
    writer.unindent();
    if (isempty_temp_lhs) writer.print(" }");
    else writer.startLine("}");

    Option<Expr> temp_rhs = getRhs();
    writer.startLine();
    writer.print("rhs = ");
    if (lossless) {
      writer.printSerialized(temp_rhs);
      writer.print(" ");
      writer.printEscaped(temp_rhs);
    } else { writer.print(temp_rhs); }
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
      LocalVarDecl casted = (LocalVarDecl) obj;
      List<Expr> temp_body = getBody();
      List<Expr> casted_body = casted.getBody();
      if (!(temp_body == casted_body || temp_body.equals(casted_body))) return false;
      List<LValue> temp_lhs = getLhs();
      List<LValue> casted_lhs = casted.getLhs();
      if (!(temp_lhs == casted_lhs || temp_lhs.equals(casted_lhs))) return false;
      Option<Expr> temp_rhs = getRhs();
      Option<Expr> casted_rhs = casted.getRhs();
      if (!(temp_rhs == casted_rhs || temp_rhs.equals(casted_rhs))) return false;
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
    List<Expr> temp_body = getBody();
    code ^= temp_body.hashCode();
    List<LValue> temp_lhs = getLhs();
    code ^= temp_lhs.hashCode();
    Option<Expr> temp_rhs = getRhs();
    code ^= temp_rhs.hashCode();
    return code;
  }
}
