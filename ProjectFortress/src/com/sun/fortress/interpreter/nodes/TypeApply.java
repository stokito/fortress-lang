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

public class TypeApply extends Primary {
  private final Expr _expr;
  private final List<StaticArg> _args;

  /**
   * Constructs a TypeApply.
   * @throws java.lang.IllegalArgumentException  If any parameter to the constructor is null.
   */
  public TypeApply(Span in_span, Expr in_expr, List<StaticArg> in_args) {
    super(in_span);

    if (in_expr == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'expr' to the TypeApply constructor was null");
    }
    _expr = in_expr;

    if (in_args == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'args' to the TypeApply constructor was null");
    }
    _args = in_args;
  }

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forTypeApply(this);
    }

    TypeApply(Span span) {
        super(span);
        _expr = null;
        _args = null;
    }

  final public Expr getExpr() { return _expr; }
  final public List<StaticArg> getArgs() { return _args; }

  public <RetType> RetType visit(NodeVisitor<RetType> visitor) { return visitor.forTypeApply(this); }
  public void visit(NodeVisitor_void visitor) { visitor.forTypeApply(this); }

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
    writer.print("TypeApply:");
    writer.indent();

    Span temp_span = getSpan();
    writer.startLine();
    writer.print("span = ");
    if (lossless) {
      writer.printSerialized(temp_span);
      writer.print(" ");
      writer.printEscaped(temp_span);
    } else { writer.print(temp_span); }

    Expr temp_expr = getExpr();
    writer.startLine();
    writer.print("expr = ");
    temp_expr.outputHelp(writer, lossless);

    List<StaticArg> temp_args = getArgs();
    writer.startLine();
    writer.print("args = ");
    writer.print("{");
    writer.indent();
    boolean isempty_temp_args = true;
    for (StaticArg elt_temp_args : temp_args) {
      isempty_temp_args = false;
      writer.startLine("* ");
      if (elt_temp_args == null) {
        writer.print("null");
      } else {
        elt_temp_args.outputHelp(writer, lossless);
      }
    }
    writer.unindent();
    if (isempty_temp_args) writer.print(" }");
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
      TypeApply casted = (TypeApply) obj;
      Expr temp_expr = getExpr();
      Expr casted_expr = casted.getExpr();
      if (!(temp_expr == casted_expr || temp_expr.equals(casted_expr))) return false;
      List<StaticArg> temp_args = getArgs();
      List<StaticArg> casted_args = casted.getArgs();
      if (!(temp_args == casted_args || temp_args.equals(casted_args))) return false;
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
    Expr temp_expr = getExpr();
    code ^= temp_expr.hashCode();
    List<StaticArg> temp_args = getArgs();
    code ^= temp_args.hashCode();
    return code;
  }
}
