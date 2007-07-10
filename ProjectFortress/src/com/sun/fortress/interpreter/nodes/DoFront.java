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

public class DoFront extends AbstractNode {
  private final Option<Expr> _loc;
  private final boolean _atomic;
  private final Expr _expr;

  /**
   * Constructs a DoFront.
   * @throws java.lang.IllegalArgumentException  If any parameter to the constructor is null.
   */
  public DoFront(Span in_span, Option<Expr> in_loc, boolean in_atomic, Expr in_expr) {
    super(in_span);

    if (in_loc == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'loc' to the DoFront constructor was null");
    }
    _loc = in_loc;
    _atomic = in_atomic;

    if (in_expr == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'expr' to the DoFront constructor was null");
    }
    _expr = in_expr;
  }

    public DoFront(Span span) {
        super(span);
        _loc = null;
        _atomic = false;
        _expr = null;
    }

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forDoFront(this);
    }

  final public Option<Expr> getLoc() { return _loc; }
  final public boolean isAtomic() { return _atomic; }
  final public Expr getExpr() { return _expr; }

  public <RetType> RetType visit(NodeVisitor<RetType> visitor) { return visitor.forDoFront(this); }
  public void visit(NodeVisitor_void visitor) { visitor.forDoFront(this); }

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
    writer.print("DoFront:");
    writer.indent();

    Span temp_span = getSpan();
    writer.startLine();
    writer.print("span = ");
    if (lossless) {
      writer.printSerialized(temp_span);
      writer.print(" ");
      writer.printEscaped(temp_span);
    } else { writer.print(temp_span); }

    Option<Expr> temp_loc = getLoc();
    writer.startLine();
    writer.print("loc = ");
    if (lossless) {
      writer.printSerialized(temp_loc);
      writer.print(" ");
      writer.printEscaped(temp_loc);
    } else { writer.print(temp_loc); }

    boolean temp_atomic = isAtomic();
    writer.startLine();
    writer.print("atomic = ");
    writer.print(temp_atomic);

    Expr temp_expr = getExpr();
    writer.startLine();
    writer.print("expr = ");
    temp_expr.outputHelp(writer, lossless);
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
      DoFront casted = (DoFront) obj;
      Option<Expr> temp_loc = getLoc();
      Option<Expr> casted_loc = casted.getLoc();
      if (!(temp_loc == casted_loc || temp_loc.equals(casted_loc))) return false;
      boolean temp_atomic = isAtomic();
      boolean casted_atomic = casted.isAtomic();
      if (!(temp_atomic == casted_atomic)) return false;
      Expr temp_expr = getExpr();
      Expr casted_expr = casted.getExpr();
      if (!(temp_expr == casted_expr || temp_expr.equals(casted_expr))) return false;
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
    Option<Expr> temp_loc = getLoc();
    code ^= temp_loc.hashCode();
    boolean temp_atomic = isAtomic();
    code ^= temp_atomic ? 1231 : 1237;
    Expr temp_expr = getExpr();
    code ^= temp_expr.hashCode();
    return code;
  }
}
