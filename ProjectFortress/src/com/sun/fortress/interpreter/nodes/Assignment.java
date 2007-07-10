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

public class Assignment extends Expr {
  private final List<? extends LHS> _lhs;
  private final Option<Op> _op;
  private final Expr _rhs;

  /**
   * Constructs a Assignment.
   * @throws java.lang.IllegalArgumentException  If any parameter to the constructor is null.
   */
  public Assignment(Span in_span, List<? extends LHS> in_lhs, Option<Op> in_op, Expr in_rhs) {
    super(in_span);

    if (in_lhs == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'lhs' to the Assignment constructor was null");
    }
    _lhs = in_lhs;

    if (in_op == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'op' to the Assignment constructor was null");
    }
    _op = in_op;

    if (in_rhs == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'rhs' to the Assignment constructor was null");
    }
    _rhs = in_rhs;
  }

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forAssignment(this);
    }

    Assignment(Span span) {
        super(span);
        _lhs = null;
        _op = null;
        _rhs = null;
    }

  final public List<? extends LHS> getLhs() { return _lhs; }
  final public Option<Op> getOp() { return _op; }
  final public Expr getRhs() { return _rhs; }

  public <RetType> RetType visit(NodeVisitor<RetType> visitor) { return visitor.forAssignment(this); }
  public void visit(NodeVisitor_void visitor) { visitor.forAssignment(this); }

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
    writer.print("Assignment:");
    writer.indent();

    Span temp_span = getSpan();
    writer.startLine();
    writer.print("span = ");
    if (lossless) {
      writer.printSerialized(temp_span);
      writer.print(" ");
      writer.printEscaped(temp_span);
    } else { writer.print(temp_span); }

    List<? extends LHS> temp_lhs = getLhs();
    writer.startLine();
    writer.print("lhs = ");
    writer.print("{");
    writer.indent();
    boolean isempty_temp_lhs = true;
    for (LHS elt_temp_lhs : temp_lhs) {
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

    Option<Op> temp_op = getOp();
    writer.startLine();
    writer.print("op = ");
    if (lossless) {
      writer.printSerialized(temp_op);
      writer.print(" ");
      writer.printEscaped(temp_op);
    } else { writer.print(temp_op); }

    Expr temp_rhs = getRhs();
    writer.startLine();
    writer.print("rhs = ");
    temp_rhs.outputHelp(writer, lossless);
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
      Assignment casted = (Assignment) obj;
      List<? extends LHS> temp_lhs = getLhs();
      List<? extends LHS> casted_lhs = casted.getLhs();
      if (!(temp_lhs == casted_lhs || temp_lhs.equals(casted_lhs))) return false;
      Option<Op> temp_op = getOp();
      Option<Op> casted_op = casted.getOp();
      if (!(temp_op == casted_op || temp_op.equals(casted_op))) return false;
      Expr temp_rhs = getRhs();
      Expr casted_rhs = casted.getRhs();
      if (!(temp_rhs == casted_rhs || temp_rhs.equals(casted_rhs))) return false;
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
    List<? extends LHS> temp_lhs = getLhs();
    code ^= temp_lhs.hashCode();
    Option<Op> temp_op = getOp();
    code ^= temp_op.hashCode();
    Expr temp_rhs = getRhs();
    code ^= temp_rhs.hashCode();
    return code;
  }
}
