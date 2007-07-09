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

public class SubscriptExpr extends OpExpr implements LHS {
  private final Expr _obj;
  private final List<Expr> _subs;
  private final Option<Enclosing> _op;

  /**
   * Constructs a SubscriptExpr.
   * @throws java.lang.IllegalArgumentException  If any parameter to the constructor is null.
   */
  public SubscriptExpr(Span in_span, Expr in_obj, List<Expr> in_subs, Option<Enclosing> in_op) {
    super(in_span);

    if (in_obj == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'obj' to the SubscriptExpr constructor was null");
    }
    _obj = in_obj;

    if (in_subs == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'subs' to the SubscriptExpr constructor was null");
    }
    _subs = in_subs;

    if (in_op == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'op' to the SubscriptExpr constructor was null");
    }
    _op = in_op;
  }

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forSubscriptExpr(this);
    }

    SubscriptExpr(Span span) {
        super(span);
        _obj = null;
        _subs = null;
        _op = null;
    }

  final public Expr getObj() { return _obj; }
  final public List<Expr> getSubs() { return _subs; }
  final public Option<Enclosing> getOp() { return _op; }

  public <RetType> RetType visit(NodeVisitor<RetType> visitor) { return visitor.forSubscriptExpr(this); }
  public void visit(NodeVisitor_void visitor) { visitor.forSubscriptExpr(this); }

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
    writer.print("SubscriptExpr:");
    writer.indent();

    Span temp_span = getSpan();
    writer.startLine();
    writer.print("span = ");
    if (lossless) {
      writer.printSerialized(temp_span);
      writer.print(" ");
      writer.printEscaped(temp_span);
    } else { writer.print(temp_span); }

    Expr temp_obj = getObj();
    writer.startLine();
    writer.print("obj = ");
    temp_obj.outputHelp(writer, lossless);

    List<Expr> temp_subs = getSubs();
    writer.startLine();
    writer.print("subs = ");
    writer.print("{");
    writer.indent();
    boolean isempty_temp_subs = true;
    for (Expr elt_temp_subs : temp_subs) {
      isempty_temp_subs = false;
      writer.startLine("* ");
      if (elt_temp_subs == null) {
        writer.print("null");
      } else {
        elt_temp_subs.outputHelp(writer, lossless);
      }
    }
    writer.unindent();
    if (isempty_temp_subs) writer.print(" }");
    else writer.startLine("}");

    Option<Enclosing> temp_op = getOp();
    writer.startLine();
    writer.print("op = ");
    if (lossless) {
      writer.printSerialized(temp_op);
      writer.print(" ");
      writer.printEscaped(temp_op);
    } else { writer.print(temp_op); }
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
      SubscriptExpr casted = (SubscriptExpr) obj;
      Expr temp_obj = getObj();
      Expr casted_obj = casted.getObj();
      if (!(temp_obj == casted_obj || temp_obj.equals(casted_obj))) return false;
      List<Expr> temp_subs = getSubs();
      List<Expr> casted_subs = casted.getSubs();
      if (!(temp_subs == casted_subs || temp_subs.equals(casted_subs))) return false;
      Option<Enclosing> temp_op = getOp();
      Option<Enclosing> casted_op = casted.getOp();
      if (!(temp_op == casted_op || temp_op.equals(casted_op))) return false;
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
    Expr temp_obj = getObj();
    code ^= temp_obj.hashCode();
    List<Expr> temp_subs = getSubs();
    code ^= temp_subs.hashCode();
    Option<Enclosing> temp_op = getOp();
    code ^= temp_op.hashCode();
    return code;
  }
}
