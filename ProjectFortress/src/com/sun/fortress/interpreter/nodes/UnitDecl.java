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

public class UnitDecl extends AbstractNode implements Decl {
  private final List<Id> _names;
  private final Option<TypeRef> _type;
  private final Option<Expr> _def;
  private final boolean _si;

  /**
   * Constructs a UnitDecl.
   * @throws java.lang.IllegalArgumentException  If any parameter to the constructor is null.
   */
  public UnitDecl(Span in_span, List<Id> in_names, Option<TypeRef> in_type, Option<Expr> in_def, boolean in_si) {
    super(in_span);

    if (in_names == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'names' to the UnitDecl constructor was null");
    }
    _names = in_names;

    if (in_type == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'type' to the UnitDecl constructor was null");
    }
    _type = in_type;

    if (in_def == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'def' to the UnitDecl constructor was null");
    }
    _def = in_def;
    _si = in_si;
  }

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forUnitDecl(this);
    }

    UnitDecl(Span span) {
        super(span);
        _names = null;
        _type = null;
        _def = null;
        _si = false;
    }

  final public List<Id> getNames() { return _names; }
  final public Option<TypeRef> getType() { return _type; }
  final public Option<Expr> getDef() { return _def; }
  final public boolean isSi() { return _si; }

  public <RetType> RetType visit(NodeVisitor<RetType> visitor) { return visitor.forUnitDecl(this); }
  public void visit(NodeVisitor_void visitor) { visitor.forUnitDecl(this); }

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
    writer.print("UnitDecl:");
    writer.indent();

    Span temp_span = getSpan();
    writer.startLine();
    writer.print("span = ");
    if (lossless) {
      writer.printSerialized(temp_span);
      writer.print(" ");
      writer.printEscaped(temp_span);
    } else { writer.print(temp_span); }

    List<Id> temp_names = getNames();
    writer.startLine();
    writer.print("names = ");
    writer.print("{");
    writer.indent();
    boolean isempty_temp_names = true;
    for (Id elt_temp_names : temp_names) {
      isempty_temp_names = false;
      writer.startLine("* ");
      if (elt_temp_names == null) {
        writer.print("null");
      } else {
        elt_temp_names.outputHelp(writer, lossless);
      }
    }
    writer.unindent();
    if (isempty_temp_names) writer.print(" }");
    else writer.startLine("}");

    Option<TypeRef> temp_type = getType();
    writer.startLine();
    writer.print("type = ");
    if (lossless) {
      writer.printSerialized(temp_type);
      writer.print(" ");
      writer.printEscaped(temp_type);
    } else { writer.print(temp_type); }

    Option<Expr> temp_def = getDef();
    writer.startLine();
    writer.print("def = ");
    if (lossless) {
      writer.printSerialized(temp_def);
      writer.print(" ");
      writer.printEscaped(temp_def);
    } else { writer.print(temp_def); }

    boolean temp_si = isSi();
    writer.startLine();
    writer.print("si = ");
    writer.print(temp_si);
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
      UnitDecl casted = (UnitDecl) obj;
      List<Id> temp_names = getNames();
      List<Id> casted_names = casted.getNames();
      if (!(temp_names == casted_names || temp_names.equals(casted_names))) return false;
      Option<TypeRef> temp_type = getType();
      Option<TypeRef> casted_type = casted.getType();
      if (!(temp_type == casted_type || temp_type.equals(casted_type))) return false;
      Option<Expr> temp_def = getDef();
      Option<Expr> casted_def = casted.getDef();
      if (!(temp_def == casted_def || temp_def.equals(casted_def))) return false;
      boolean temp_si = isSi();
      boolean casted_si = casted.isSi();
      if (!(temp_si == casted_si)) return false;
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
    List<Id> temp_names = getNames();
    code ^= temp_names.hashCode();
    Option<TypeRef> temp_type = getType();
    code ^= temp_type.hashCode();
    Option<Expr> temp_def = getDef();
    code ^= temp_def.hashCode();
    boolean temp_si = isSi();
    code ^= temp_si ? 1231 : 1237;
    return code;
  }
}
