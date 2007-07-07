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

public class PropertyDecl extends AbstractNode implements Decl, AbsDecl {
  private final Option<Id> _id;
  private final List<Param> _params;
  private final Expr _expr;

  /**
   * Constructs a PropertyDecl.
   * @throws java.lang.IllegalArgumentException  If any parameter to the constructor is null.
   */
  public PropertyDecl(Span in_span, Option<Id> in_id, List<Param> in_params, Expr in_expr) {
    super(in_span);

    if (in_id == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'id' to the PropertyDecl constructor was null");
    }
    _id = in_id;

    if (in_params == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'params' to the PropertyDecl constructor was null");
    }
    _params = in_params;

    if (in_expr == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'expr' to the PropertyDecl constructor was null");
    }
    _expr = in_expr;
  }

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forPropertyDecl(this);
    }

    PropertyDecl(Span span) {
        super(span);
        _id = null;
        _params = null;
        _expr = null;
    }

  final public Option<Id> getId() { return _id; }
  final public List<Param> getParams() { return _params; }
  final public Expr getExpr() { return _expr; }

  public <RetType> RetType visit(NodeVisitor<RetType> visitor) { return visitor.forPropertyDecl(this); }
  public void visit(NodeVisitor_void visitor) { visitor.forPropertyDecl(this); }

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
    writer.print("PropertyDecl:");
    writer.indent();

    Span temp_span = getSpan();
    writer.startLine();
    writer.print("span = ");
    if (lossless) {
      writer.printSerialized(temp_span);
      writer.print(" ");
      writer.printEscaped(temp_span);
    } else { writer.print(temp_span); }

    Option<Id> temp_id = getId();
    writer.startLine();
    writer.print("id = ");
    if (lossless) {
      writer.printSerialized(temp_id);
      writer.print(" ");
      writer.printEscaped(temp_id);
    } else { writer.print(temp_id); }

    List<Param> temp_params = getParams();
    writer.startLine();
    writer.print("params = ");
    writer.print("{");
    writer.indent();
    boolean isempty_temp_params = true;
    for (Param elt_temp_params : temp_params) {
      isempty_temp_params = false;
      writer.startLine("* ");
      if (elt_temp_params == null) {
        writer.print("null");
      } else {
        elt_temp_params.outputHelp(writer, lossless);
      }
    }
    writer.unindent();
    if (isempty_temp_params) writer.print(" }");
    else writer.startLine("}");

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
      PropertyDecl casted = (PropertyDecl) obj;
      Option<Id> temp_id = getId();
      Option<Id> casted_id = casted.getId();
      if (!(temp_id == casted_id || temp_id.equals(casted_id))) return false;
      List<Param> temp_params = getParams();
      List<Param> casted_params = casted.getParams();
      if (!(temp_params == casted_params || temp_params.equals(casted_params))) return false;
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
  protected int generateHashCode() {
    int code = getClass().hashCode();
    Option<Id> temp_id = getId();
    code ^= temp_id.hashCode();
    List<Param> temp_params = getParams();
    code ^= temp_params.hashCode();
    Expr temp_expr = getExpr();
    code ^= temp_expr.hashCode();
    return code;
  }
}
