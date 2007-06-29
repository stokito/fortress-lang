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

public class TypeCaseClause extends AbstractNode {
  private final List<TypeRef> _match;
  private final List<Expr> _body;

  /**
   * Constructs a TypeCaseClause.
   * @throw java.lang.IllegalArgumentException if any parameter to the constructor is null.
   */
  public TypeCaseClause(Span in_span, List<TypeRef> in_match, List<Expr> in_body) {
    super(in_span);

    if (in_match == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'match' to the TypeCaseClause constructor was null. This class may not have null field values.");
    }
    _match = in_match;

    if (in_body == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'body' to the TypeCaseClause constructor was null. This class may not have null field values.");
    }
    _body = in_body;
  }

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forTypeCaseClause(this);
    }

    TypeCaseClause(Span span) {
        super(span);
        _match = null;
        _body = null;
    }

  final public List<TypeRef> getMatch() { return _match; }
  final public List<Expr> getBody() { return _body; }

  public <RetType> RetType visit(NodeVisitor<RetType> visitor) { return visitor.forTypeCaseClause(this); }
  public void visit(NodeVisitor_void visitor) { visitor.forTypeCaseClause(this); }

  /**
   * Implementation of toString that uses
   * {@see #output} to generated nicely tabbed tree.
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
    outputHelp(new TabPrintWriter(writer, 2));
  }

  public void outputHelp(TabPrintWriter writer) {
    writer.print("TypeCaseClause" + ":");
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
    writer.print("match = ");
    List<TypeRef> temp_match = getMatch();
    if (temp_match == null) {
      writer.print("null");
    } else {
      writer.print(temp_match);
    }

    writer.startLine("");
    writer.print("body = ");
    List<Expr> temp_body = getBody();
    if (temp_body == null) {
      writer.print("null");
    } else {
      writer.print(temp_body);
    }
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
      TypeCaseClause casted = (TypeCaseClause) obj;
      if (! (getMatch().equals(casted.getMatch()))) return false;
      if (! (getBody().equals(casted.getBody()))) return false;
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
    code ^= getMatch().hashCode();
    code ^= getBody().hashCode();
    return code;
  }
}
