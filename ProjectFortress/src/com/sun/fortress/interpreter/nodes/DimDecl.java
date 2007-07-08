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

public class DimDecl extends AbstractNode implements Decl {
  private final Id _id;
  private final Option<TypeRef> _derived;
  private final Option<TypeRef> _default;

  /**
   * Constructs a DimDecl.
   * @throws java.lang.IllegalArgumentException  If any parameter to the constructor is null.
   */
  public DimDecl(Span in_span, Id in_id, Option<TypeRef> in_derived, Option<TypeRef> in_default) {
    super(in_span);

    if (in_id == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'id' to the DimDecl constructor was null");
    }
    _id = in_id;

    if (in_derived == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'derived' to the DimDecl constructor was null");
    }
    _derived = in_derived;

    if (in_default == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'default' to the DimDecl constructor was null");
    }
    _default = in_default;
  }

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forDimDecl(this);
    }

    DimDecl(Span span) {
        super(span);
        _id = null;
        _derived = null;
        _default = null;
    }

  final public Id getId() { return _id; }
  final public Option<TypeRef> getDerived() { return _derived; }
  final public Option<TypeRef> getDefault() { return _default; }

  public <RetType> RetType visit(NodeVisitor<RetType> visitor) { return visitor.forDimDecl(this); }
  public void visit(NodeVisitor_void visitor) { visitor.forDimDecl(this); }

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
    writer.print("DimDecl:");
    writer.indent();

    Span temp_span = getSpan();
    writer.startLine();
    writer.print("span = ");
    if (lossless) {
      writer.printSerialized(temp_span);
      writer.print(" ");
      writer.printEscaped(temp_span);
    } else { writer.print(temp_span); }

    Id temp_id = getId();
    writer.startLine();
    writer.print("id = ");
    temp_id.outputHelp(writer, lossless);

    Option<TypeRef> temp_derived = getDerived();
    writer.startLine();
    writer.print("derived = ");
    if (lossless) {
      writer.printSerialized(temp_derived);
      writer.print(" ");
      writer.printEscaped(temp_derived);
    } else { writer.print(temp_derived); }

    Option<TypeRef> temp_default = getDefault();
    writer.startLine();
    writer.print("default = ");
    if (lossless) {
      writer.printSerialized(temp_default);
      writer.print(" ");
      writer.printEscaped(temp_default);
    } else { writer.print(temp_default); }
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
      DimDecl casted = (DimDecl) obj;
      Id temp_id = getId();
      Id casted_id = casted.getId();
      if (!(temp_id == casted_id || temp_id.equals(casted_id))) return false;
      Option<TypeRef> temp_derived = getDerived();
      Option<TypeRef> casted_derived = casted.getDerived();
      if (!(temp_derived == casted_derived || temp_derived.equals(casted_derived))) return false;
      Option<TypeRef> temp_default = getDefault();
      Option<TypeRef> casted_default = casted.getDefault();
      if (!(temp_default == casted_default || temp_default.equals(casted_default))) return false;
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
    Id temp_id = getId();
    code ^= temp_id.hashCode();
    Option<TypeRef> temp_derived = getDerived();
    code ^= temp_derived.hashCode();
    Option<TypeRef> temp_default = getDefault();
    code ^= temp_default.hashCode();
    return code;
  }
}
