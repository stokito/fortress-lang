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

public class GeneratedExpr extends LetExpr {
  private final Expr _expr;
  private final List<Generator> _gens;

  /**
   * Constructs a GeneratedExpr.
   * @throws java.lang.IllegalArgumentException  If any parameter to the constructor is null.
   */
  public GeneratedExpr(Span in_span, List<Expr> in_body, Expr in_expr, List<Generator> in_gens) {
    super(in_span, in_body);

    if (in_expr == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'expr' to the GeneratedExpr constructor was null");
    }
    _expr = in_expr;

    if (in_gens == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'gens' to the GeneratedExpr constructor was null");
    }
    _gens = in_gens;
  }

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forGeneratedExpr(this);
    }

    GeneratedExpr(Span span) {
        super(span);
        _expr = null;
        _gens = null;
    }

  final public Expr getExpr() { return _expr; }
  final public List<Generator> getGens() { return _gens; }

  public <RetType> RetType visit(NodeVisitor<RetType> visitor) { return visitor.forGeneratedExpr(this); }
  public void visit(NodeVisitor_void visitor) { visitor.forGeneratedExpr(this); }

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
    writer.print("GeneratedExpr:");
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

    Expr temp_expr = getExpr();
    writer.startLine();
    writer.print("expr = ");
    temp_expr.outputHelp(writer, lossless);

    List<Generator> temp_gens = getGens();
    writer.startLine();
    writer.print("gens = ");
    writer.print("{");
    writer.indent();
    boolean isempty_temp_gens = true;
    for (Generator elt_temp_gens : temp_gens) {
      isempty_temp_gens = false;
      writer.startLine("* ");
      if (elt_temp_gens == null) {
        writer.print("null");
      } else {
        elt_temp_gens.outputHelp(writer, lossless);
      }
    }
    writer.unindent();
    if (isempty_temp_gens) writer.print(" }");
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
      GeneratedExpr casted = (GeneratedExpr) obj;
      List<Expr> temp_body = getBody();
      List<Expr> casted_body = casted.getBody();
      if (!(temp_body == casted_body || temp_body.equals(casted_body))) return false;
      Expr temp_expr = getExpr();
      Expr casted_expr = casted.getExpr();
      if (!(temp_expr == casted_expr || temp_expr.equals(casted_expr))) return false;
      List<Generator> temp_gens = getGens();
      List<Generator> casted_gens = casted.getGens();
      if (!(temp_gens == casted_gens || temp_gens.equals(casted_gens))) return false;
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
    Expr temp_expr = getExpr();
    code ^= temp_expr.hashCode();
    List<Generator> temp_gens = getGens();
    code ^= temp_gens.hashCode();
    return code;
  }
}
