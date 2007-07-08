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

public class TypeAlias extends WhereClause implements Decl {
  private final Id _name;
  private final List<StaticParam> _staticParams;
  private final TypeRef _type;

  /**
   * Constructs a TypeAlias.
   * @throws java.lang.IllegalArgumentException  If any parameter to the constructor is null.
   */
  public TypeAlias(Span in_span, Id in_name, List<StaticParam> in_staticParams, TypeRef in_type) {
    super(in_span);

    if (in_name == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'name' to the TypeAlias constructor was null");
    }
    _name = in_name;

    if (in_staticParams == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'staticParams' to the TypeAlias constructor was null");
    }
    _staticParams = in_staticParams;

    if (in_type == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'type' to the TypeAlias constructor was null");
    }
    _type = in_type;
  }

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forTypeAlias(this);
    }

    TypeAlias(Span span) {
        super(span);
        _name = null;
        _staticParams = null;
        _type = null;
    }

  final public Id getName() { return _name; }
  final public List<StaticParam> getStaticParams() { return _staticParams; }
  final public TypeRef getType() { return _type; }

  public <RetType> RetType visit(NodeVisitor<RetType> visitor) { return visitor.forTypeAlias(this); }
  public void visit(NodeVisitor_void visitor) { visitor.forTypeAlias(this); }

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
    writer.print("TypeAlias:");
    writer.indent();

    Span temp_span = getSpan();
    writer.startLine();
    writer.print("span = ");
    if (lossless) {
      writer.printSerialized(temp_span);
      writer.print(" ");
      writer.printEscaped(temp_span);
    } else { writer.print(temp_span); }

    Id temp_name = getName();
    writer.startLine();
    writer.print("name = ");
    temp_name.outputHelp(writer, lossless);

    List<StaticParam> temp_staticParams = getStaticParams();
    writer.startLine();
    writer.print("staticParams = ");
    writer.print("{");
    writer.indent();
    boolean isempty_temp_staticParams = true;
    for (StaticParam elt_temp_staticParams : temp_staticParams) {
      isempty_temp_staticParams = false;
      writer.startLine("* ");
      if (elt_temp_staticParams == null) {
        writer.print("null");
      } else {
        elt_temp_staticParams.outputHelp(writer, lossless);
      }
    }
    writer.unindent();
    if (isempty_temp_staticParams) writer.print(" }");
    else writer.startLine("}");

    TypeRef temp_type = getType();
    writer.startLine();
    writer.print("type = ");
    temp_type.outputHelp(writer, lossless);
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
      TypeAlias casted = (TypeAlias) obj;
      Id temp_name = getName();
      Id casted_name = casted.getName();
      if (!(temp_name == casted_name || temp_name.equals(casted_name))) return false;
      List<StaticParam> temp_staticParams = getStaticParams();
      List<StaticParam> casted_staticParams = casted.getStaticParams();
      if (!(temp_staticParams == casted_staticParams || temp_staticParams.equals(casted_staticParams))) return false;
      TypeRef temp_type = getType();
      TypeRef casted_type = casted.getType();
      if (!(temp_type == casted_type || temp_type.equals(casted_type))) return false;
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
    Id temp_name = getName();
    code ^= temp_name.hashCode();
    List<StaticParam> temp_staticParams = getStaticParams();
    code ^= temp_staticParams.hashCode();
    TypeRef temp_type = getType();
    code ^= temp_type.hashCode();
    return code;
  }
}
