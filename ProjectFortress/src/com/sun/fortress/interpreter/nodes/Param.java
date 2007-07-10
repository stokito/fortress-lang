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

public class Param extends AbstractNode {
  private final List<Modifier> _mods;
  private final Id _name;
  private final Option<TypeRef> _type;
  private final Option<Expr> _defaultExpr;

  /**
   * Constructs a Param.
   * @throws java.lang.IllegalArgumentException  If any parameter to the constructor is null.
   */
  public Param(Span in_span, List<Modifier> in_mods, Id in_name, Option<TypeRef> in_type, Option<Expr> in_defaultExpr) {
    super(in_span);

    if (in_mods == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'mods' to the Param constructor was null");
    }
    _mods = in_mods;

    if (in_name == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'name' to the Param constructor was null");
    }
    _name = in_name;

    if (in_type == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'type' to the Param constructor was null");
    }
    _type = in_type;

    if (in_defaultExpr == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'defaultExpr' to the Param constructor was null");
    }
    _defaultExpr = in_defaultExpr;
  }

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forParam(this);
    }

    Param(Span span) {
        super(span);
        _mods = null;
        _name = null;
        _type = new None<TypeRef>();
        _defaultExpr = new None<Expr>();
    }

  final public List<Modifier> getMods() { return _mods; }
  final public Id getName() { return _name; }
  final public Option<TypeRef> getType() { return _type; }
  final public Option<Expr> getDefaultExpr() { return _defaultExpr; }

  public <RetType> RetType visit(NodeVisitor<RetType> visitor) { return visitor.forParam(this); }
  public void visit(NodeVisitor_void visitor) { visitor.forParam(this); }

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
    writer.print("Param:");
    writer.indent();

    Span temp_span = getSpan();
    writer.startLine();
    writer.print("span = ");
    if (lossless) {
      writer.printSerialized(temp_span);
      writer.print(" ");
      writer.printEscaped(temp_span);
    } else { writer.print(temp_span); }

    List<Modifier> temp_mods = getMods();
    writer.startLine();
    writer.print("mods = ");
    writer.print("{");
    writer.indent();
    boolean isempty_temp_mods = true;
    for (Modifier elt_temp_mods : temp_mods) {
      isempty_temp_mods = false;
      writer.startLine("* ");
      if (elt_temp_mods == null) {
        writer.print("null");
      } else {
        if (lossless) {
          writer.printSerialized(elt_temp_mods);
          writer.print(" ");
          writer.printEscaped(elt_temp_mods);
        } else { writer.print(elt_temp_mods); }
      }
    }
    writer.unindent();
    if (isempty_temp_mods) writer.print(" }");
    else writer.startLine("}");

    Id temp_name = getName();
    writer.startLine();
    writer.print("name = ");
    temp_name.outputHelp(writer, lossless);

    Option<TypeRef> temp_type = getType();
    writer.startLine();
    writer.print("type = ");
    if (lossless) {
      writer.printSerialized(temp_type);
      writer.print(" ");
      writer.printEscaped(temp_type);
    } else { writer.print(temp_type); }

    Option<Expr> temp_defaultExpr = getDefaultExpr();
    writer.startLine();
    writer.print("defaultExpr = ");
    if (lossless) {
      writer.printSerialized(temp_defaultExpr);
      writer.print(" ");
      writer.printEscaped(temp_defaultExpr);
    } else { writer.print(temp_defaultExpr); }
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
      Param casted = (Param) obj;
      List<Modifier> temp_mods = getMods();
      List<Modifier> casted_mods = casted.getMods();
      if (!(temp_mods == casted_mods || temp_mods.equals(casted_mods))) return false;
      Id temp_name = getName();
      Id casted_name = casted.getName();
      if (!(temp_name == casted_name || temp_name.equals(casted_name))) return false;
      Option<TypeRef> temp_type = getType();
      Option<TypeRef> casted_type = casted.getType();
      if (!(temp_type == casted_type || temp_type.equals(casted_type))) return false;
      Option<Expr> temp_defaultExpr = getDefaultExpr();
      Option<Expr> casted_defaultExpr = casted.getDefaultExpr();
      if (!(temp_defaultExpr == casted_defaultExpr || temp_defaultExpr.equals(casted_defaultExpr))) return false;
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
    List<Modifier> temp_mods = getMods();
    code ^= temp_mods.hashCode();
    Id temp_name = getName();
    code ^= temp_name.hashCode();
    Option<TypeRef> temp_type = getType();
    code ^= temp_type.hashCode();
    Option<Expr> temp_defaultExpr = getDefaultExpr();
    code ^= temp_defaultExpr.hashCode();
    return code;
  }
}
