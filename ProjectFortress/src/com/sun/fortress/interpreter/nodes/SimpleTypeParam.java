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

public class SimpleTypeParam extends StaticParam {
  private final Id _id;
  private final Option<List<TypeRef>> _extendsClause;
  private final boolean _absorbs;

  /**
   * Constructs a SimpleTypeParam.
   * @throw java.lang.IllegalArgumentException if any parameter to the constructor is null.
   */
  public SimpleTypeParam(Span in_span, Id in_id, Option<List<TypeRef>> in_extendsClause, boolean in_absorbs) {
    super(in_span);

    if (in_id == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'id' to the SimpleTypeParam constructor was null. This class may not have null field values.");
    }
    _id = in_id;

    if (in_extendsClause == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'extendsClause' to the SimpleTypeParam constructor was null. This class may not have null field values.");
    }
    _extendsClause = in_extendsClause;
    _absorbs = in_absorbs;
  }

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forSimpleTypeParam(this);
    }

    SimpleTypeParam(Span span) {
        super(span);
        _id = null;
        _extendsClause = null;
        _absorbs = false;
    }

  final public Id getId() { return _id; }
  final public Option<List<TypeRef>> getExtendsClause() { return _extendsClause; }
  final public boolean isAbsorbs() { return _absorbs; }

  public <RetType> RetType visit(NodeVisitor<RetType> visitor) { return visitor.forSimpleTypeParam(this); }
  public void visit(NodeVisitor_void visitor) { visitor.forSimpleTypeParam(this); }

  /**
   * Implementation of toString that uses
   * {@see #output} to generated nicely tabbed tree.
   */
    /*
  public java.lang.String toString() {
    java.io.StringWriter w = new java.io.StringWriter();
    output(w);
    return w.toString();
  }
    */
    @Override
    public String toString() {
        return _id.getName();
    }

  /**
   * Prints this object out as a nicely tabbed tree.
   */
  public void output(java.io.Writer writer) {
    outputHelp(new TabPrintWriter(writer, 2));
  }

  public void outputHelp(TabPrintWriter writer) {
    writer.print("SimpleTypeParam" + ":");
    writer.indent();

    writer.startLine("");
    writer.print("span = ");
    Span temp_span = getSpan();
    if (temp_span == null) {
      writer.print("null");
    } else {
      writer.print(temp_span);
    }

    writer.startLine("");
    writer.print("id = ");
    Id temp_id = getId();
    if (temp_id == null) {
      writer.print("null");
    } else {
      temp_id.outputHelp(writer);
    }

    writer.startLine("");
    writer.print("extendsClause = ");
    Option<List<TypeRef>> temp_extendsClause = getExtendsClause();
    if (temp_extendsClause == null) {
      writer.print("null");
    } else {
      writer.print(temp_extendsClause);
    }

    writer.startLine("");
    writer.print("absorbs = ");
    boolean temp_absorbs = isAbsorbs();
    writer.print(temp_absorbs);
    writer.unindent();
  }

  /**
   * Implementation of equals that is based on the values
   * of the fields of the object. Thus, two objects
   * created with identical parameters will be equal.
   */
  public boolean equals(java.lang.Object obj) {
    if (obj == null) return false;
    if ((obj.getClass() != this.getClass()) || (obj.hashCode() != this.hashCode())) {
      return false;
    } else {
      SimpleTypeParam casted = (SimpleTypeParam) obj;
      if (! (getId().equals(casted.getId()))) return false;
      if (! (getExtendsClause().equals(casted.getExtendsClause()))) return false;
      if (! (isAbsorbs() == casted.isAbsorbs())) return false;
      return true;
    }
  }

  /**
   * Implementation of hashCode that is consistent with
   * equals. The value of the hashCode is formed by
   * XORing the hashcode of the class object with
   * the hashcodes of all the fields of the object.
   */
  protected int generateHashCode() {
    int code = getClass().hashCode();
    code ^= 0;
    code ^= getId().hashCode();
    code ^= getExtendsClause().hashCode();
    code ^= (isAbsorbs() ? 1231 : 1237);
    return code;
  }
}
